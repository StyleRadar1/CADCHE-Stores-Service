package edu.dosw.styleradar.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;

@DisplayName("Pruebas Unitarias del Value Object PeriodoNuevaLlegada - SR-RF-60")
class PeriodoNuevaLlegadaTest {

    @Test
    @DisplayName("Criterio: Dado un almacén sin periodo propio, aplica el valor por defecto de 7 días")
    void debeRetornarValorPorDefectoDe7Dias() {
        PeriodoNuevaLlegada periodoPorDefecto = PeriodoNuevaLlegada.porDefecto();

        assertThat(periodoPorDefecto).isNotNull();
        assertThat(periodoPorDefecto.getDias()).isEqualTo(7);
        assertThat(PeriodoNuevaLlegada.VALOR_POR_DEFECTO).isEqualTo(7);
    }

    @Test
    @DisplayName("Cuando el valor recibido es nulo, debe aplicar el valor por defecto de 7 días")
    void debeAplicarValorPorDefectoCuandoEsNulo() {
        PeriodoNuevaLlegada periodo = PeriodoNuevaLlegada.de(null);

        assertThat(periodo).isNotNull();
        assertThat(periodo.getDias()).isEqualTo(7);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 7, 14, 30, 45, 60})
    @DisplayName("Criterio: Dado un periodo válido entre 1 y 60 días, se crea exitosamente")
    void debeCrearPeriodoValido(int diasValidos) {
        PeriodoNuevaLlegada periodo = PeriodoNuevaLlegada.de(diasValidos);

        assertThat(periodo).isNotNull();
        assertThat(periodo.getDias()).isEqualTo(diasValidos);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -7, -30, -100})
    @DisplayName("Criterio: Dado un periodo de 0 días o menos, debe lanzar PeriodoInvalidoException (400)")
    void debeRechazarPeriodoCeroONegativo(int diasInvalidos) {
        assertThatThrownBy(() -> PeriodoNuevaLlegada.de(diasInvalidos))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("debe ser de al menos 1 día");
    }

    @ParameterizedTest
    @ValueSource(ints = {61, 75, 100, 365})
    @DisplayName("Debe rechazar periodo superior al límite máximo de 60 días")
    void debeRechazarPeriodoSuperiorAlMaximo(int diasExcesivos) {
        assertThatThrownBy(() -> PeriodoNuevaLlegada.de(diasExcesivos))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("no puede superar los 60 días");
    }

    @Test
    @DisplayName("Debe cumplir con igualdad semántica de Value Object (equals y hashCode)")
    void debeCumplirIgualdadSemantica() {
        PeriodoNuevaLlegada p1 = PeriodoNuevaLlegada.de(14);
        PeriodoNuevaLlegada p2 = PeriodoNuevaLlegada.de(14);
        PeriodoNuevaLlegada p3 = PeriodoNuevaLlegada.de(21);

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(p3);
        assertThat(p1.toString()).isEqualTo("14");
    }
}
