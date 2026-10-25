package dev.beneficios.adapters.outbound.persistence;

import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import dev.beneficios.ports.outbound.WalletRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWalletRepository implements WalletRepository {

    private final ConcurrentMap<WalletId, Wallet> store = new ConcurrentHashMap<>();

    @Override
    public Wallet save(Wallet wallet) {
        store.put(wallet.id(), wallet);
        return wallet;
    }

    @Override
    public Optional<Wallet> findById(WalletId id) {
        return Optional.ofNullable(store.get(id));
    }
}
