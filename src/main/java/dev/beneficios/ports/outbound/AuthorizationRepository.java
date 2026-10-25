package dev.beneficios.ports.outbound;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import java.util.Optional;

public interface AuthorizationRepository {

    Authorization save(Authorization authorization);

    Optional<Authorization> findById(AuthorizationId id);

    Optional<Authorization> findByIdempotencyKey(IdempotencyKey key);
}
