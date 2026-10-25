package dev.beneficios.domain.model.wallet;

import java.util.Objects;
import java.util.UUID;

public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "value");
    }

    public static WalletId generate() {
        return new WalletId(UUID.randomUUID());
    }

    public static WalletId of(String value) {
        return new WalletId(UUID.fromString(value));
    }
}
