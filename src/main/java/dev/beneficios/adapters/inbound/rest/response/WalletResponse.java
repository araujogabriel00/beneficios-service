package dev.beneficios.adapters.inbound.rest.response;

import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record WalletResponse(UUID id, String beneficiaryName, Map<BenefitCategory, Long> balancesCents) {

    public static WalletResponse from(Wallet wallet) {
        Map<BenefitCategory, Long> balances = new LinkedHashMap<>();
        for (BenefitCategory category : BenefitCategory.values()) {
            balances.put(category, wallet.balance(category).cents());
        }
        return new WalletResponse(wallet.id().value(), wallet.beneficiaryName(), balances);
    }
}
