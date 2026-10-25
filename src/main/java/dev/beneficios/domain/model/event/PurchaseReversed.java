package dev.beneficios.domain.model.event;

import dev.beneficios.domain.model.shared.DomainEvent;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.WalletId;

public record PurchaseReversed(WalletId walletId, BenefitCategory category, Money amount) implements DomainEvent {
}
