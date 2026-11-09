package edu.dosw.styleradar.tiendas.domain.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;

@DisplayName("Pruebas Unitarias de PeriodoNuevaLlegadaValidator - SR-RF-60")
class PeriodoNuevaLlegadaValidatorTest {

    private PeriodoNuevaLlegadaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PeriodoNuevaLlegadaValidator();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 7, 15, 30, 60})
    @DisplayName("Debe aceptar valores de periodo válidos entre 1 y 60 días")
    void debeAceptarPeriodosValidos(int diasValidos) {
        assertThatCode(() -> validator.validar(diasValidos))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar periodo nulo con PeriodoInvalidoException")
    void debeRechazarPeriodoNulo() {
        assertThatThrownBy(() -> validator.validar(null))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("obligatorio");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -15, -60})
    @DisplayName("Criterio: Dado un periodo de 0 días o menos, debe rechazar con PeriodoInvalidoException (400)")
    void debeRechazarPeriodoCeroONegativo(int diasInvalidos) {
        assertThatThrownBy(() -> validator.validar(diasInvalidos))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("debe ser de al menos 1 día");
    }

    @ParameterizedTest
    @ValueSource(ints = {61, 90, 180, 365})
    @DisplayName("Debe rechazar periodo mayor a 60 días con PeriodoInvalidoException")
    void debeRechazarPeriodoExcesivo(int diasExcesivos) {
        assertThatThrownBy(() -> validator.validar(diasExcesivos))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("no puede superar los 60 días");
    }
}
