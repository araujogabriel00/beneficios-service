package dev.beneficios.domain.model.authorization;

import dev.beneficios.domain.exception.InvalidStateException;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.BenefitCategory;
import dev.beneficios.domain.model.wallet.WalletId;
import java.util.Optional;

public class Authorization {

    public enum Status {
        APPROVED,
        DENIED,
        REVERSED
    }

    private final AuthorizationId id;
    private final IdempotencyKey key;
    private final WalletId walletId;
    private final MerchantType merchantType;
    private final Money amount;
    private final BenefitCategory category;
    private final DenialReason denialReason;
    private Status status;

    private Authorization(IdempotencyKey key, WalletId walletId, MerchantType merchantType, Money amount,
                          BenefitCategory category, DenialReason denialReason, Status status) {
        this.id = AuthorizationId.generate();
        this.key = key;
        this.walletId = walletId;
        this.merchantType = merchantType;
        this.amount = amount;
        this.category = category;
        this.denialReason = denialReason;
        this.status = status;
    }

    public static Authorization approved(IdempotencyKey key, WalletId walletId, MerchantType merchantType,
                                         Money amount, BenefitCategory category) {
        return new Authorization(key, walletId, merchantType, amount, category, null, Status.APPROVED);
    }

    public static Authorization denied(IdempotencyKey key, WalletId walletId, MerchantType merchantType,
                                       Money amount, DenialReason reason) {
        return new Authorization(key, walletId, merchantType, amount, null, reason, Status.DENIED);
    }

    /** RN-07: only an approved authorization can be reversed, once, for its full amount. */
    public void reverse() {
        if (status != Status.APPROVED) {
            throw new InvalidStateException("Only an approved authorization can be reversed, but it is " + status);
        }
        status = Status.REVERSED;
    }

    /** RN-04: whether a repeated request with the same key carries the same content. */
    public boolean sameRequest(WalletId walletId, MerchantType merchantType, Money amount) {
        return this.walletId.equals(walletId) && this.merchantType == merchantType && this.amount.equals(amount);
    }

    public AuthorizationId id() {
        return id;
    }

    public IdempotencyKey key() {
        return key;
    }

    public WalletId walletId() {
        return walletId;
    }

    public MerchantType merchantType() {
        return merchantType;
    }

    public Money amount() {
        return amount;
    }

    public Status status() {
        return status;
    }

    /** The category debited; empty for denied authorizations. */
    public Optional<BenefitCategory> category() {
        return Optional.ofNullable(category);
    }

    public Optional<DenialReason> denialReason() {
        return Optional.ofNullable(denialReason);
    }
}
