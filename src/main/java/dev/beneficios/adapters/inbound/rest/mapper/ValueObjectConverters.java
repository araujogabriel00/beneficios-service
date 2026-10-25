package dev.beneficios.adapters.inbound.rest.mapper;

import dev.beneficios.domain.model.authorization.AuthorizationId;
import dev.beneficios.domain.model.authorization.IdempotencyKey;
import dev.beneficios.domain.model.shared.Money;
import dev.beneficios.domain.model.wallet.WalletId;
import org.springframework.stereotype.Component;

/** Conversions from the primitives of the API to the value objects of the domain, shared by the mappers. */
@Component
public class ValueObjectConverters {

    public WalletId toWalletId(String value) {
        return WalletId.of(value);
    }

    public AuthorizationId toAuthorizationId(String value) {
        return AuthorizationId.of(value);
    }

    public IdempotencyKey toIdempotencyKey(String value) {
        return new IdempotencyKey(value);
    }

    public Money toMoney(long cents) {
        return Money.ofCents(cents);
    }
}
