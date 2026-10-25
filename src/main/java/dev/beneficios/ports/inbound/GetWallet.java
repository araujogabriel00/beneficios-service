package dev.beneficios.ports.inbound;

import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;

public interface GetWallet {

    Wallet get(WalletId walletId);
}
