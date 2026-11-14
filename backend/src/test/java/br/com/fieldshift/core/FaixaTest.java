package br.com.fieldshift.core;

import br.com.fieldshift.core.modelo.Faixa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FaixaTest {

    private final Faixa faixa = new Faixa(20, 30, 10, 40);

    @Test
    void dentroDoIdealValeUm() {
        assertThat(faixa.pontuar(20)).isEqualTo(1.0);
        assertThat(faixa.pontuar(25)).isEqualTo(1.0);
        assertThat(faixa.pontuar(30)).isEqualTo(1.0);
    }

    @Test
    void caiEmLinhaRetaAteOLimiteAbsoluto() {
        assertThat(faixa.pontuar(15)).isCloseTo(0.5, within(1e-9));
        assertThat(faixa.pontuar(35)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void foraDoLimiteAbsolutoValeZero() {
        assertThat(faixa.pontuar(10)).isZero();
        assertThat(faixa.pontuar(5)).isZero();
        assertThat(faixa.pontuar(45)).isZero();
    }

    @Test
    void recusaFaixaForaDeOrdem() {
        assertThatThrownBy(() -> new Faixa(30, 20, 10, 40)).isInstanceOf(IllegalArgumentException.class);
    }
}
