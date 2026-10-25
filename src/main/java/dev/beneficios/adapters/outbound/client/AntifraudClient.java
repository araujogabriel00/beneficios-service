package dev.beneficios.adapters.outbound.client;

import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;

/** The raw call to the external antifraud service; throws when the service is unavailable. */
public interface AntifraudClient {

    boolean approves(WalletId walletId, Money amount);
}
