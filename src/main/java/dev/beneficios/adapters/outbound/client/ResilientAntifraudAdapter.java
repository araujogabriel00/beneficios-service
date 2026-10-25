package dev.beneficios.adapters.outbound.client;

import dev.beneficios.ports.outbound.AntifraudPort;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry (outermost) around CircuitBreaker around a call with timeout. The fallback of RN-06 sits outside
 * the Retry, so it applies to failures, timeouts and an open circuit alike.
 */
public class ResilientAntifraudAdapter implements AntifraudPort {

    /** RN-06: with the service down, purchases up to this value are approved and larger ones denied. */
    static final Money FALLBACK_LIMIT = Money.ofCents(10_000);

    private static final Logger log = LoggerFactory.getLogger(ResilientAntifraudAdapter.class);

    private final AntifraudClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "antifraud-call");
        thread.setDaemon(true);
        return thread;
    });

    public ResilientAntifraudAdapter(AntifraudClient client, CircuitBreaker circuitBreaker, Retry retry,
                                     Duration timeout) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.timeout = timeout;
    }

    @Override
    public boolean approves(WalletId walletId, Money amount) {
        Supplier<Boolean> call = () -> callWithTimeout(walletId, amount);
        Supplier<Boolean> guarded = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call));
        try {
            return guarded.get();
        } catch (RuntimeException failure) {
            log.warn("Antifraud unavailable ({}); applying fallback", failure.toString());
            return !amount.isGreaterThan(FALLBACK_LIMIT);
        }
    }

    private boolean callWithTimeout(WalletId walletId, Money amount) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> client.approves(walletId, amount), executor);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Antifraud call timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Antifraud call interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Antifraud call failed", e.getCause());
        }
    }
}
