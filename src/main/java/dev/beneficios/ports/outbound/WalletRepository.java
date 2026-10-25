package dev.beneficios.ports.outbound;

import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import java.util.Optional;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(WalletId id);
}
