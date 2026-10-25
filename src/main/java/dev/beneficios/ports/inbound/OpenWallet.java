package dev.beneficios.ports.inbound;

import dev.beneficios.domain.model.wallet.Wallet;

public interface OpenWallet {

    Wallet open(OpenWalletCommand command);

    record OpenWalletCommand(String beneficiaryName) {
    }
}
