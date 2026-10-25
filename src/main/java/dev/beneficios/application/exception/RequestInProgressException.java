package dev.beneficios.application.exception;

/** A request with the same idempotency key is still being processed. */
public class RequestInProgressException extends RuntimeException {

    public RequestInProgressException(String message) {
        super(message);
    }
}
