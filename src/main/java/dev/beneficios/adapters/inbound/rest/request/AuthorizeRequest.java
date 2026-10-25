package dev.beneficios.adapters.inbound.rest.request;

import dev.beneficios.domain.model.authorization.MerchantType;

public record AuthorizeRequest(String walletId, MerchantType merchantType, long amountCents) {
}
