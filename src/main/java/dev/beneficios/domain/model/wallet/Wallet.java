package dev.beneficios.domain.model.wallet;

import dev.beneficios.domain.exception.InsufficientBalanceException;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.AggregateRoot;
import dev.beneficios.domain.exception.IdempotencyConflictException;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.event.BenefitCredited;
import dev.beneficios.domain.model.event.PurchaseDebited;
import dev.beneficios.domain.model.event.PurchaseReversed;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class Wallet extends AggregateRoot {

    private record CreditRecord(BenefitCategory category, Money amount) {
    }

    private final WalletId id;
    private final String beneficiaryName;
    private final Map<BenefitCategory, Money> balances = new EnumMap<>(BenefitCategory.class);
    private final Map<IdempotencyKey, CreditRecord> credits = new HashMap<>();

    private Wallet(WalletId id, String beneficiaryName) {
        this.id = id;
        this.beneficiaryName = beneficiaryName;
        for (BenefitCategory category : BenefitCategory.values()) {
            balances.put(category, Money.ZERO);
        }
    }

    public static Wallet open(String beneficiaryName) {
        if (beneficiaryName == null || beneficiaryName.isBlank()) {
            throw new IllegalArgumentException("Beneficiary name must not be blank");
        }
        return new Wallet(WalletId.generate(), beneficiaryName.trim());
    }

    public WalletId id() {
        return id;
    }

    public String beneficiaryName() {
        return beneficiaryName;
    }

    public Money balance(BenefitCategory category) {
        return balances.get(category);
    }

    public Map<BenefitCategory, Money> balances() {
        return Map.copyOf(balances);
    }

    /** RN-04: crediting twice with the same key and content has no further effect. */
    public void credit(IdempotencyKey key, BenefitCategory category, Money amount) {
        requirePositive(amount);
        CreditRecord record = new CreditRecord(category, amount);
        CreditRecord previous = credits.get(key);
        if (previous != null) {
            if (!previous.equals(record)) {
                throw new IdempotencyConflictException("Idempotency key reused with different content");
            }
            return;
        }
        credits.put(key, record);
        balances.merge(category, amount, Money::plus);
        record(new BenefitCredited(id, category, amount));
    }

    /** RN-01: never leaves a negative balance. */
    public void debit(BenefitCategory category, Money amount) {
        requirePositive(amount);
        if (amount.isGreaterThan(balances.get(category))) {
            throw new InsufficientBalanceException(category);
        }
        balances.merge(category, amount, Money::minus);
        record(new PurchaseDebited(id, category, amount));
    }

    public void refund(BenefitCategory category, Money amount) {
        requirePositive(amount);
        balances.merge(category, amount, Money::plus);
        record(new PurchaseReversed(id, category, amount));
    }

    public boolean canPay(BenefitCategory category, Money amount) {
        return !amount.isGreaterThan(balances.get(category));
    }

    private static void requirePositive(Money amount) {
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
