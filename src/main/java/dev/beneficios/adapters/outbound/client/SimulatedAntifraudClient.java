package dev.beneficios.adapters.outbound.client;

import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;
import java.util.concurrent.atomic.AtomicBoolean;

/** Stands in for the external service: denies purchases above a limit and can be switched off. */
public class SimulatedAntifraudClient implements AntifraudClient {

    private final Money denyAbove;
    private final AtomicBoolean available = new AtomicBoolean(true);

    public SimulatedAntifraudClient(Money denyAbove) {
        this.denyAbove = denyAbove;
    }

    public void setAvailable(boolean value) {
        available.set(value);
    }

    @Override
    public boolean approves(WalletId walletId, Money amount) {
        if (!available.get()) {
            throw new IllegalStateException("Antifraud service unavailable");
        }
        return !amount.isGreaterThan(denyAbove);
    }
}
