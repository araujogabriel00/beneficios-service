package dev.beneficios.domain.model.wallet;

import dev.beneficios.domain.exception.InsufficientBalanceException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.exception.IdempotencyConflictException;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.event.BenefitCredited;
import dev.beneficios.domain.model.event.PurchaseDebited;
import dev.beneficios.domain.model.event.PurchaseReversed;
import org.junit.jupiter.api.Test;

class WalletTest {

    private static final IdempotencyKey KEY = new IdempotencyKey("k1");

    @Test
    void ca_01_1_newWalletHasZeroBalanceInEveryCategory() {
        Wallet wallet = Wallet.open("Maria");

        for (BenefitCategory category : BenefitCategory.values()) {
            assertThat(wallet.balance(category)).isEqualTo(Money.ZERO);
        }
    }

    @Test
    void ca_01_2_blankNameIsRejected() {
        assertThatThrownBy(() -> Wallet.open("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ca_02_1_creditRaisesOnlyTheChosenCategory() {
        Wallet wallet = Wallet.open("Maria");

        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(50_000));

        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(50_000));
        assertThat(wallet.balance(BenefitCategory.ALIMENTACAO)).isEqualTo(Money.ZERO);
    }

    @Test
    void ca_02_2_creditEmitsBenefitCredited() {
        Wallet wallet = Wallet.open("Maria");

        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(500));

        assertThat(wallet.pullEvents())
                .containsExactly(new BenefitCredited(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(500)));
    }

    @Test
    void ca_02_3_zeroCreditIsRejected() {
        Wallet wallet = Wallet.open("Maria");

        assertThatThrownBy(() -> wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ca_02_4_sameKeyCreditsOnce() {
        Wallet wallet = Wallet.open("Maria");

        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(500));
        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(500));

        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(500));
        assertThat(wallet.pullEvents()).hasSize(1);
    }

    @Test
    void rn04_sameKeyWithDifferentContentConflicts() {
        Wallet wallet = Wallet.open("Maria");
        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(500));

        assertThatThrownBy(() -> wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(600)))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void ca_03_1_debitLowersBalanceAndEmitsEvent() {
        Wallet wallet = Wallet.open("Maria");
        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(10_000));
        wallet.pullEvents();

        wallet.debit(BenefitCategory.REFEICAO, Money.ofCents(3_000));

        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(7_000));
        assertThat(wallet.pullEvents())
                .containsExactly(new PurchaseDebited(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(3_000)));
    }

    @Test
    void rn01_debitAboveBalanceFailsAndKeepsBalance() {
        Wallet wallet = Wallet.open("Maria");
        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(10_000));

        assertThatThrownBy(() -> wallet.debit(BenefitCategory.REFEICAO, Money.ofCents(10_001)))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
    }

    @Test
    void ca_04_1_refundRestoresBalanceAndEmitsEvent() {
        Wallet wallet = Wallet.open("Maria");
        wallet.credit(KEY, BenefitCategory.REFEICAO, Money.ofCents(10_000));
        wallet.debit(BenefitCategory.REFEICAO, Money.ofCents(3_000));
        wallet.pullEvents();

        wallet.refund(BenefitCategory.REFEICAO, Money.ofCents(3_000));

        assertThat(wallet.balance(BenefitCategory.REFEICAO)).isEqualTo(Money.ofCents(10_000));
        assertThat(wallet.pullEvents())
                .containsExactly(new PurchaseReversed(wallet.id(), BenefitCategory.REFEICAO, Money.ofCents(3_000)));
    }
}
