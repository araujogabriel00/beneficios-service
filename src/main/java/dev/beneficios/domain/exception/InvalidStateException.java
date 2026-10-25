package dev.beneficios.domain.exception;

/** An operation is not allowed in the current state of the aggregate. */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
