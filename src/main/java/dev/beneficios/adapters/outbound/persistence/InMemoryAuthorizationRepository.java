package dev.beneficios.adapters.outbound.persistence;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.ports.outbound.AuthorizationRepository;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {

    private final ConcurrentMap<AuthorizationId, Authorization> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<IdempotencyKey, Authorization> byKey = new ConcurrentHashMap<>();

    @Override
    public Authorization save(Authorization authorization) {
        byId.put(authorization.id(), authorization);
        byKey.put(authorization.key(), authorization);
        return authorization;
    }

    @Override
    public Optional<Authorization> findById(AuthorizationId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Authorization> findByIdempotencyKey(IdempotencyKey key) {
        return Optional.ofNullable(byKey.get(key));
    }
}
