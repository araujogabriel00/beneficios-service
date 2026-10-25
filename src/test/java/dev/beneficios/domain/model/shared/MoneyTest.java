package dev.beneficios.domain.model.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void rn01_rejectsNegativeValue() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rn01_subtractionBelowZeroFails() {
        assertThatThrownBy(() -> Money.ofCents(100).minus(Money.ofCents(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addsAndSubtractsCents() {
        assertThat(Money.ofCents(100).plus(Money.ofCents(50))).isEqualTo(Money.ofCents(150));
        assertThat(Money.ofCents(100).minus(Money.ofCents(30))).isEqualTo(Money.ofCents(70));
    }
}
