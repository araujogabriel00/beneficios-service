package dev.beneficios.application.service;

import dev.beneficios.domain.model.wallet.WalletId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** One monitor per wallet, so balance changes on the same wallet are serialized (D-04, in-memory). */
public class WalletLocks {

    private final ConcurrentMap<WalletId, Object> locks = new ConcurrentHashMap<>();

    public Object of(WalletId walletId) {
        return locks.computeIfAbsent(walletId, id -> new Object());
    }
}
