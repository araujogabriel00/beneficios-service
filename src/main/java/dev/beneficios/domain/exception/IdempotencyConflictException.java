package dev.beneficios.domain.exception;

/** An idempotency key was reused with different content. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
