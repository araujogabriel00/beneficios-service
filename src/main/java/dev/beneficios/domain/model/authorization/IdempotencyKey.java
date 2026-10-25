package dev.beneficios.domain.model.authorization;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }
    }
}
