package dev.beneficios.domain.model.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.beneficios.domain.exception.InvalidStateException;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.WalletId;
import org.junit.jupiter.api.Test;

class AuthorizationTest {

    private final WalletId walletId = WalletId.generate();
    private final IdempotencyKey key = new IdempotencyKey("k");

    private Authorization approved() {
        return Authorization.approved(key, walletId, MerchantType.RESTAURANTE, Money.ofCents(3_000),
                BenefitCategory.REFEICAO);
    }

    @Test
    void ca_04_1_approvedCanBeReversed() {
        Authorization authorization = approved();

        authorization.reverse();

        assertThat(authorization.status()).isEqualTo(Authorization.Status.REVERSED);
    }

    @Test
    void ca_04_2_reversingTwiceFails() {
        Authorization authorization = approved();
        authorization.reverse();

        assertThatThrownBy(authorization::reverse).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void ca_04_3_deniedCannotBeReversed() {
        Authorization authorization = Authorization.denied(key, walletId, MerchantType.OUTROS,
                Money.ofCents(3_000), DenialReason.MERCHANT_NOT_ACCEPTED);

        assertThatThrownBy(authorization::reverse).isInstanceOf(InvalidStateException.class);
        assertThat(authorization.status()).isEqualTo(Authorization.Status.DENIED);
    }

    @Test
    void ca_03_6_sameRequestComparesWalletMerchantAndAmount() {
        Authorization authorization = approved();

        assertThat(authorization.sameRequest(walletId, MerchantType.RESTAURANTE, Money.ofCents(3_000))).isTrue();
        assertThat(authorization.sameRequest(walletId, MerchantType.RESTAURANTE, Money.ofCents(3_001))).isFalse();
        assertThat(authorization.sameRequest(WalletId.generate(), MerchantType.RESTAURANTE, Money.ofCents(3_000)))
                .isFalse();
    }
}
