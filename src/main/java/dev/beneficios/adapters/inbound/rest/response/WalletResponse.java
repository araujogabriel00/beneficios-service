package dev.beneficios.adapters.inbound.rest.response;

import dev.beneficios.domain.model.wallet.BenefitCategory;
import java.util.Map;
import java.util.UUID;

public record WalletResponse(UUID id, String beneficiaryName, Map<BenefitCategory, Long> balancesCents) {
}
