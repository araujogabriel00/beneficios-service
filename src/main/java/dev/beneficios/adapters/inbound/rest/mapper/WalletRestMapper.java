package dev.beneficios.adapters.inbound.rest.mapper;

import dev.beneficios.adapters.inbound.rest.request.CreditRequest;
import dev.beneficios.adapters.inbound.rest.request.OpenWalletRequest;
import dev.beneficios.adapters.inbound.rest.response.WalletResponse;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.Wallet;
import dev.beneficios.ports.inbound.CreditBenefit.CreditBenefitCommand;
import dev.beneficios.ports.inbound.OpenWallet.OpenWalletCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ValueObjectConverters.class)
public interface WalletRestMapper {

    OpenWalletCommand toCommand(OpenWalletRequest request);

    @Mapping(target = "walletId", source = "walletId")
    @Mapping(target = "key", source = "idempotencyKey")
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "amount", source = "request.amountCents")
    CreditBenefitCommand toCommand(String walletId, String idempotencyKey, CreditRequest request);

    @Mapping(target = "id", expression = "java(wallet.id().value())")
    @Mapping(target = "beneficiaryName", expression = "java(wallet.beneficiaryName())")
    @Mapping(target = "balancesCents", expression = "java(balancesCents(wallet))")
    WalletResponse toResponse(Wallet wallet);

    default Map<BenefitCategory, Long> balancesCents(Wallet wallet) {
        Map<BenefitCategory, Long> balances = new LinkedHashMap<>();
        for (BenefitCategory category : BenefitCategory.values()) {
            balances.put(category, wallet.balance(category).cents());
        }
        return balances;
    }
}
