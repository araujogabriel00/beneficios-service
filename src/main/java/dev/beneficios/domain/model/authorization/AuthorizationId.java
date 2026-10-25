package dev.beneficios.domain.model.authorization;

import java.util.Objects;
import java.util.UUID;

public record AuthorizationId(UUID value) {

    public AuthorizationId {
        Objects.requireNonNull(value, "value");
    }

    public static AuthorizationId generate() {
        return new AuthorizationId(UUID.randomUUID());
    }

    public static AuthorizationId of(String value) {
        return new AuthorizationId(UUID.fromString(value));
    }
}
