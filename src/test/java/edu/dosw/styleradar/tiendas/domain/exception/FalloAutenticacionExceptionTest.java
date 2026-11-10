package edu.dosw.styleradar.tiendas.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pruebas Unitarias de FalloAutenticacionException")
class FalloAutenticacionExceptionTest {

    @Test
    @DisplayName("Debe instanciar con mensaje y causa")
    void debeInstanciarConMensajeYCausa() {
        Throwable causa = new IllegalArgumentException("Causa raíz");
        FalloAutenticacionException excepcion = new FalloAutenticacionException("Fallo de autenticación", causa);

        assertThat(excepcion.getMessage()).isEqualTo("Fallo de autenticación");
        assertThat(excepcion.getCause()).isEqualTo(causa);
    }

    @Test
    @DisplayName("Debe instanciar solo con mensaje")
    void debeInstanciarSoloConMensaje() {
        FalloAutenticacionException excepcion = new FalloAutenticacionException("Solo mensaje");

        assertThat(excepcion.getMessage()).isEqualTo("Solo mensaje");
        assertThat(excepcion.getCause()).isNull();
    }
}
