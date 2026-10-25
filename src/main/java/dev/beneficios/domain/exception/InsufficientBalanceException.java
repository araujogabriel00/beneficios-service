package dev.beneficios.domain.exception;

import dev.beneficios.domain.model.wallet.BenefitCategory;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(BenefitCategory category) {
        super("Insufficient balance in " + category);
    }
}
