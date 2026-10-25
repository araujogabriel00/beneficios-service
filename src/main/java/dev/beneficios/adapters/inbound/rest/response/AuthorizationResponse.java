package dev.beneficios.adapters.inbound.rest.response;

import dev.beneficios.domain.model.authorization.Authorization;
import dev.beneficios.domain.model.authorization.DenialReason;
import dev.beneficios.domain.model.authorization.MerchantType;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import java.util.UUID;

public record AuthorizationResponse(UUID id, UUID walletId, MerchantType merchantType, long amountCents,
                                    Authorization.Status status, BenefitCategory category,
                                    DenialReason denialReason) {
}
