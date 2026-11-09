package edu.dosw.styleradar.tiendas.domain.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;

@DisplayName("Pruebas Unitarias de UmbralInventarioValidator - SR-RF-59")
class UmbralInventarioValidatorTest {

    private UmbralInventarioValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UmbralInventarioValidator();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10, 25, 50})
    @DisplayName("Debe aceptar valores de umbral válidos entre 0 y 50")
    void debeAceptarUmbralesValidos(int umbralValido) {
        assertThatCode(() -> validator.validar(umbralValido))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar umbral nulo con UmbralInvalidoException")
    void debeRechazarUmbralNulo() {
        assertThatThrownBy(() -> validator.validar(null))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("obligatorio");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, -10, -100})
    @DisplayName("Debe rechazar umbrales negativos con UmbralInvalidoException")
    void debeRechazarUmbralesNegativos(int umbralNegativo) {
        assertThatThrownBy(() -> validator.validar(umbralNegativo))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @ParameterizedTest
    @ValueSource(ints = {51, 100, 999})
    @DisplayName("Debe rechazar umbrales mayores a 50 con UmbralInvalidoException")
    void debeRechazarUmbralesExcesivos(int umbralExcesivo) {
        assertThatThrownBy(() -> validator.validar(umbralExcesivo))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede superar el límite");
    }
}
