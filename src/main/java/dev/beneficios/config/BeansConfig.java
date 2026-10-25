package dev.beneficios.config;

import dev.beneficios.adapters.outbound.client.AntifraudClient;
import dev.beneficios.adapters.outbound.client.ResilientAntifraudAdapter;
import dev.beneficios.adapters.outbound.client.SimulatedAntifraudClient;
import dev.beneficios.adapters.outbound.messaging.LoggingEventPublisher;
import dev.beneficios.adapters.outbound.persistence.InMemoryAuthorizationRepository;
import dev.beneficios.adapters.outbound.persistence.InMemoryWalletRepository;
import dev.beneficios.ports.outbound.AntifraudPort;
import dev.beneficios.ports.outbound.DomainEventPublisher;
import dev.beneficios.application.service.PurchaseService;
import dev.beneficios.application.service.WalletLocks;
import dev.beneficios.application.service.WalletService;
import dev.beneficios.ports.outbound.AuthorizationRepository;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.ports.outbound.WalletRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    WalletRepository walletRepository() {
        return new InMemoryWalletRepository();
    }

    @Bean
    AuthorizationRepository authorizationRepository() {
        return new InMemoryAuthorizationRepository();
    }

    @Bean
    DomainEventPublisher domainEventPublisher() {
        return new LoggingEventPublisher();
    }

    @Bean
    WalletLocks walletLocks() {
        return new WalletLocks();
    }

    @Bean
    SimulatedAntifraudClient antifraudClient() {
        // R$ 1.000,00: simulated rule of the external service.
        return new SimulatedAntifraudClient(Money.ofCents(100_000));
    }

    @Bean
    CircuitBreaker antifraudCircuitBreaker() {
        return CircuitBreaker.of("antifraud", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());
    }

    @Bean
    Retry antifraudRetry() {
        return Retry.of("antifraud", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(50))
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
    }

    @Bean
    AntifraudPort antifraudPort(AntifraudClient client, CircuitBreaker antifraudCircuitBreaker, Retry antifraudRetry) {
        return new ResilientAntifraudAdapter(client, antifraudCircuitBreaker, antifraudRetry,
                Duration.ofMillis(500));
    }

    @Bean
    WalletService walletService(WalletRepository wallets, DomainEventPublisher events, WalletLocks locks) {
        return new WalletService(wallets, events, locks);
    }

    @Bean
    PurchaseService purchaseService(WalletRepository wallets, AuthorizationRepository authorizations,
                                    AntifraudPort antifraud, DomainEventPublisher events, WalletLocks locks) {
        return new PurchaseService(wallets, authorizations, antifraud, events, locks);
    }
}
