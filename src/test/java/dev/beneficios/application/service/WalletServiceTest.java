package dev.beneficios.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.beneficios.application.exception.NotFoundException;
import dev.beneficios.ports.inbound.CreditBenefit.CreditBenefitCommand;
import dev.beneficios.ports.inbound.OpenWallet.OpenWalletCommand;
import dev.beneficios.application.service.Fakes.FakeWalletRepository;
import dev.beneficios.application.service.Fakes.RecordingPublisher;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import dev.beneficios.domain.model.event.BenefitCredited;
import org.junit.jupiter.api.Test;

class WalletServiceTest {

    private final RecordingPublisher publisher = new RecordingPublisher();
    private final WalletService service =
            new WalletService(new FakeWalletRepository(), publisher, new WalletLocks());

    @Test
    void ca_01_1_opensWalletWithZeroBalances() {
        Wallet wallet = service.open(new OpenWalletCommand("Maria"));

        assertThat(service.get(wallet.id()).balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ZERO);
        assertThat(service.get(wallet.id()).balance(BenefitCategory.ALIMENTACAO)).isEqualTo(Money.ZERO);
    }

    @Test
    void ca_01_2_blankNameCreatesNothing() {
        assertThatThrownBy(() -> service.open(new OpenWalletCommand("")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ca_02_1_and_02_2_creditRaisesBalanceAndPublishesEvent() {
        Wallet wallet = service.open(new OpenWalletCommand("Maria"));

        service.credit(new CreditBenefitCommand(wallet.id(), new IdempotencyKey("c1"),
                BenefitCategory.REFEICAO, Money.ofCents(50_000)));

        assertThat(service.get(wallet.id()).balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(50_000));
        assertThat(publisher.published)
                .containsExactly(new BenefitCredited(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(50_000)));
    }

    @Test
    void ca_02_4_sameKeyCreditsOnce() {
        Wallet wallet = service.open(new OpenWalletCommand("Maria"));
        CreditBenefitCommand command = new CreditBenefitCommand(wallet.id(), new IdempotencyKey("c1"),
                BenefitCategory.REFEICAO, Money.ofCents(500));

        service.credit(command);
        service.credit(command);

        assertThat(service.get(wallet.id()).balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(500));
    }

    @Test
    void ca_05_2_unknownWalletIsNotFound() {
        assertThatThrownBy(() -> service.get(WalletId.generate())).isInstanceOf(NotFoundException.class);
    }
}
