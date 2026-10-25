package dev.beneficios.ports.outbound;

import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;

public interface AntifraudPort {

    /** Whether the purchase passes the risk check. Implementations own their failure handling (RN-06). */
    boolean approves(WalletId walletId, Money amount);
}
