package dev.beneficios.domain.model.authorization;

import dev.beneficios.domain.model.wallet.BenefitCategory;
import java.util.Optional;

public enum MerchantType {
    RESTAURANTE(BenefitCategory.REFEICAO),
    SUPERMERCADO(BenefitCategory.ALIMENTACAO),
    OUTROS(null);

    private final BenefitCategory category;

    MerchantType(BenefitCategory category) {
        this.category = category;
    }

    /** RN-02: the category that pays purchases at this kind of merchant, if any. */
    public Optional<BenefitCategory> category() {
        return Optional.ofNullable(category);
    }
}
