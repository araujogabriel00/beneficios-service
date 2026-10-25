package dev.beneficios.ports.inbound;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.authorization.MerchantType;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;

public interface AuthorizePurchase {

    Authorization authorize(AuthorizePurchaseCommand command);

    record AuthorizePurchaseCommand(IdempotencyKey key, WalletId walletId, MerchantType merchantType, Money amount) {
    }
}
