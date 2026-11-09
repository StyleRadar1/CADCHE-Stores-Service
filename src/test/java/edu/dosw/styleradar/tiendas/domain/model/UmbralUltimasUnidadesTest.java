package edu.dosw.styleradar.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;

@DisplayName("Pruebas de Dominio - Value Object UmbralUltimasUnidades (SR-RF-59)")
class UmbralUltimasUnidadesTest {

    @Test
    @DisplayName("Dado un valor nulo, cuando se instancia, entonces aplica el valor por defecto de 3")
    void debeAplicarValorPorDefectoCuandoEsNulo() {
        UmbralUltimasUnidades umbral = UmbralUltimasUnidades.de(null);

        assertThat(umbral).isNotNull();
        assertThat(umbral.getValor()).isEqualTo(3);
        assertThat(umbral.getValor()).isEqualTo(UmbralUltimasUnidades.VALOR_POR_DEFECTO);
    }

    @Test
    @DisplayName("Dado el método fábrica porDefecto, retorna valor 3")
    void debeRetornarInstanciaConValorPorDefecto() {
        UmbralUltimasUnidades umbral = UmbralUltimasUnidades.porDefecto();

        assertThat(umbral.getValor()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 5, 10, 25, 50})
    @DisplayName("Dado un umbral válido entre 0 y 50, se instancia correctamente")
    void debeInstanciarConValorValido(int valorValido) {
        UmbralUltimasUnidades umbral = UmbralUltimasUnidades.de(valorValido);

        assertThat(umbral.getValor()).isEqualTo(valorValido);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -3, -10, -99})
    @DisplayName("Dado un umbral negativo, cuando se instancia, entonces lanza UmbralInvalidoException")
    void debeLanzarExcepcionConUmbralNegativo(int valorNegativo) {
        assertThatThrownBy(() -> UmbralUltimasUnidades.de(valorNegativo))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @ParameterizedTest
    @ValueSource(ints = {51, 100, 500})
    @DisplayName("Dado un umbral que supera el máximo permitido (50), lanza UmbralInvalidoException")
    void debeLanzarExcepcionConUmbralExcedido(int valorExcedido) {
        assertThatThrownBy(() -> UmbralUltimasUnidades.de(valorExcedido))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede superar el límite");
    }
}

