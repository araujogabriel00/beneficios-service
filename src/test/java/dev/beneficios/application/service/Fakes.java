package dev.beneficios.application.service;

import dev.beneficios.ports.outbound.AntifraudPort;
import dev.beneficios.ports.outbound.DomainEventPublisher;
import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.ports.outbound.AuthorizationRepository;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.DomainEvent;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.domain.model.wallet.WalletId;
import dev.beneficios.ports.outbound.WalletRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class Fakes {

    private Fakes() {
    }

    static class FakeWalletRepository implements WalletRepository {
        private final Map<WalletId, Wallet> store = new HashMap<>();

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

    static class FakeAuthorizationRepository implements AuthorizationRepository {
        private final Map<AuthorizationId, Authorization> store = new HashMap<>();

        @Override
        public Authorization save(Authorization authorization) {
            store.put(authorization.id(), authorization);
            return authorization;
        }

        @Override
        public Optional<Authorization> findById(AuthorizationId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Authorization> findByIdempotencyKey(IdempotencyKey key) {
            return store.values().stream().filter(a -> a.key().equals(key)).findFirst();
        }
    }

    static class FakeAntifraud implements AntifraudPort {
        boolean approves = true;
        int calls;

        @Override
        public boolean approves(WalletId walletId, Money amount) {
            calls++;
            return approves;
        }
    }

    static class RecordingPublisher implements DomainEventPublisher {
        final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(List<DomainEvent> events) {
            published.addAll(events);
        }
    }
}
