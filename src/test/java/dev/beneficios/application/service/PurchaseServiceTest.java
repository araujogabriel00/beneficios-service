package dev.beneficios.application.service;

import dev.beneficios.domain.model.wallet.WalletId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.beneficios.application.exception.NotFoundException;
import dev.beneficios.ports.inbound.AuthorizePurchase.AuthorizePurchaseCommand;
import dev.beneficios.application.service.Fakes.FakeAntifraud;
import dev.beneficios.application.service.Fakes.FakeAuthorizationRepository;
import dev.beneficios.application.service.Fakes.FakeWalletRepository;
import dev.beneficios.application.service.Fakes.RecordingPublisher;
import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.Authorization.Status;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.domain.model.authorization.DenialReason;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.authorization.MerchantType;
import dev.beneficios.domain.exception.IdempotencyConflictException;
import dev.beneficios.domain.exception.InvalidStateException;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.event.PurchaseDebited;
import dev.beneficios.domain.model.event.PurchaseReversed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurchaseServiceTest {

    private final FakeWalletRepository wallets = new FakeWalletRepository();
    private final FakeAuthorizationRepository authorizations = new FakeAuthorizationRepository();
    private final FakeAntifraud antifraud = new FakeAntifraud();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final PurchaseService service =
            new PurchaseService(wallets, authorizations, antifraud, publisher, new WalletLocks());

    private Wallet wallet;

    @BeforeEach
    void walletWith100InMeal() {
        wallet = Wallet.open("Maria");
        wallet.credit(new IdempotencyKey("seed"), BenefitCategory.REFEICAO, Money.ofCents(10_000));
        wallet.pullEvents();
        wallets.save(wallet);
    }

    private AuthorizePurchaseCommand purchase(String key, MerchantType type, long cents) {
        return new AuthorizePurchaseCommand(new IdempotencyKey(key), wallet.id(), type, Money.ofCents(cents));
    }

    @Test
    void ca_03_1_approvesAndDebits() {
        Authorization result = service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));

        assertThat(result.status()).isEqualTo(Status.APPROVED);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(7_000));
        assertThat(publisher.published)
                .containsExactly(new PurchaseDebited(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(3_000)));
    }

    @Test
    void ca_03_2_deniesWithoutCallingAntifraudWhenBalanceIsShort() {
        Authorization result = service.authorize(purchase("k", MerchantType.RESTAURANTE, 10_001));

        assertThat(result.denialReason()).contains(DenialReason.INSUFFICIENT_BALANCE);
        assertThat(antifraud.calls).isZero();
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
    }

    @Test
    void ca_03_3_deniesUnacceptedMerchant() {
        Authorization result = service.authorize(purchase("k", MerchantType.OUTROS, 1_000));

        assertThat(result.denialReason()).contains(DenialReason.MERCHANT_NOT_ACCEPTED);
        assertThat(antifraud.calls).isZero();
    }

    @Test
    void ca_03_4_deniesWhenAntifraudRejects() {
        antifraud.approves = false;

        Authorization result = service.authorize(purchase("k", MerchantType.RESTAURANTE, 1_000));

        assertThat(result.denialReason()).contains(DenialReason.FRAUD_SUSPECTED);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
    }

    @Test
    void ca_03_5_repeatedKeyReturnsSameAuthorizationWithoutNewDebitOrAntifraudCall() {
        Authorization first = service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));
        Authorization second = service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(7_000));
        assertThat(antifraud.calls).isEqualTo(1);
    }

    @Test
    void ca_03_5_repeatedKeyReturnsTheSameDeniedAuthorization() {
        Authorization first = service.authorize(purchase("k", MerchantType.OUTROS, 3_000));
        Authorization second = service.authorize(purchase("k", MerchantType.OUTROS, 3_000));

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void ca_03_6_sameKeyWithOtherContentConflictsAndChangesNothing() {
        service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));

        assertThatThrownBy(() -> service.authorize(purchase("k", MerchantType.RESTAURANTE, 4_000)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(7_000));
    }

    @Test
    void unknownWalletIsNotFound() {
        AuthorizePurchaseCommand command = new AuthorizePurchaseCommand(new IdempotencyKey("k"),
                dev.beneficios.domain.model.wallet.WalletId.generate(), MerchantType.RESTAURANTE, Money.ofCents(100));

        assertThatThrownBy(() -> service.authorize(command)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void ca_04_1_reverseRestoresBalanceAndPublishesEvent() {
        Authorization approved = service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));
        publisher.published.clear();

        Authorization reversed = service.reverse(approved.id());

        assertThat(reversed.status()).isEqualTo(Status.REVERSED);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
        assertThat(publisher.published)
                .containsExactly(new PurchaseReversed(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(3_000)));
    }

    @Test
    void ca_04_2_reversingTwiceConflictsAndKeepsBalance() {
        Authorization approved = service.authorize(purchase("k", MerchantType.RESTAURANTE, 3_000));
        service.reverse(approved.id());

        assertThatThrownBy(() -> service.reverse(approved.id())).isInstanceOf(InvalidStateException.class);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
    }

    @Test
    void ca_04_3_deniedCannotBeReversed() {
        Authorization denied = service.authorize(purchase("k", MerchantType.OUTROS, 3_000));

        assertThatThrownBy(() -> service.reverse(denied.id())).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void ca_04_4_unknownAuthorizationIsNotFound() {
        assertThatThrownBy(() -> service.reverse(AuthorizationId.generate())).isInstanceOf(NotFoundException.class);
    }
}
