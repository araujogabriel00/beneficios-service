package dev.beneficios.adapters.inbound.rest.response;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.DenialReason;
import dev.beneficios.domain.model.authorization.MerchantType;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import java.util.UUID;

public record AuthorizationResponse(UUID id, UUID walletId, MerchantType merchantType, long amountCents,
                                    Authorization.Status status, BenefitCategory category,
                                    DenialReason denialReason) {

    public static AuthorizationResponse from(Authorization authorization) {
        return new AuthorizationResponse(
                authorization.id().value(),
                authorization.walletId().value(),
                authorization.merchantType(),
                authorization.amount().cents(),
                authorization.status(),
                authorization.category().orElse(null),
                authorization.denialReason().orElse(null));
    }
}
