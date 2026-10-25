package dev.beneficios.ports.inbound;

import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;

public interface CreditBenefit {

    Wallet credit(CreditBenefitCommand command);

    record CreditBenefitCommand(WalletId walletId, IdempotencyKey key, BenefitCategory category, Money amount) {
    }
}
