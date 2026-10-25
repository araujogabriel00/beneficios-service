package dev.beneficios.adapters.outbound.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ResilientAntifraudAdapterTest {

    private final WalletId wallet = WalletId.generate();
    private final AtomicInteger calls = new AtomicInteger();

    private ResilientAntifraudAdapter adapterOver(AntifraudClient client) {
        CircuitBreaker breaker = CircuitBreaker.of("antifraud", CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        Retry retry = Retry.of("antifraud", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
        return new ResilientAntifraudAdapter(client, breaker, retry, Duration.ofMillis(500));
    }

    private AntifraudClient down() {
        return (walletId, amount) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("down");
        };
    }

    @Test
    void usesTheClientDecisionWhenAvailable() {
        ResilientAntifraudAdapter adapter = adapterOver((w, amount) -> amount.cents() <= 50_000);

        assertThat(adapter.approves(wallet, Money.ofCents(50_000))).isTrue();
        assertThat(adapter.approves(wallet, Money.ofCents(50_001))).isFalse();
    }

    @Test
    void ca_03_7_fallbackApprovesUpTo100AndDeniesAbove() {
        ResilientAntifraudAdapter adapter = adapterOver(down());

        assertThat(adapter.approves(wallet, Money.ofCents(8_000))).isTrue();
        assertThat(adapter.approves(wallet, Money.ofCents(10_000))).isTrue();
        assertThat(adapter.approves(wallet, Money.ofCents(15_000))).isFalse();
    }

    @Test
    void ca_03_8_openCircuitSkipsTheClient() {
        ResilientAntifraudAdapter adapter = adapterOver(down());
        for (int i = 0; i < 4; i++) {
            adapter.approves(wallet, Money.ofCents(100));
        }
        int callsWhenOpened = calls.get();

        boolean approved = adapter.approves(wallet, Money.ofCents(100));

        assertThat(calls.get()).isEqualTo(callsWhenOpened);
        assertThat(approved).isTrue();
    }

    @Test
    void slowClientTimesOutIntoFallback() {
        ResilientAntifraudAdapter adapter = adapterOver((w, amount) -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        });

        assertThat(adapter.approves(wallet, Money.ofCents(100))).isTrue();
    }
}
