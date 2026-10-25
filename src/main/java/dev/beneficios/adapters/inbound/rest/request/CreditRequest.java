package dev.beneficios.adapters.inbound.rest.request;

import dev.beneficios.domain.model.wallet.BenefitCategory;

public record CreditRequest(BenefitCategory category, long amountCents) {
}
