package edu.dosw.styleradar.tiendas.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.DatoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.model.CodigoError;

@DisplayName("Pruebas Unitarias de GlobalExceptionHandler - mensajes claros de validación y HTTP")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Incluye los mensajes de validación en detail y message y sanitiza errores typeMismatch")
    void debeIncluirDetalleClaroEnValidacionesYSanitizarTypeMismatch() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/foundations");

        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "consulta");
        binding.addError(new FieldError("consulta", "latitude", "La latitud debe estar entre -90 y 90."));
        binding.addError(new FieldError("consulta", "type", "ZAPATO", true, new String[] {"typeMismatch"}, null,
                "Failed to convert value of type 'java.lang.String' to required type 'TipoPrenda'"));

        ResponseEntity<ErrorResponseDTO> res = handler.handleBindingValidation(new BindException(binding), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo(CodigoError.INVALID_DATA);
        assertThat(res.getBody().getDetail())
                .startsWith("La solicitud contiene datos inválidos: ")
                .contains("La latitud debe estar entre -90 y 90")
                .contains("El valor ingresado para 'type' no tiene un formato válido")
                .doesNotContain("java.lang.String");
        assertThat(res.getBody().getMessage()).isEqualTo(res.getBody().getDetail());
        assertThat(res.getBody().getErrors()).containsKeys("latitude", "type");

        ResponseEntity<ErrorResponseDTO> resDato = handler.handleDatoInvalido(
                new DatoInvalidoException("nit", "El NIT del almacén es obligatorio"), request);
        assertThat(resDato.getBody().getDetail())
                .isEqualTo("La solicitud contiene datos inválidos: El NIT del almacén es obligatorio");

        assertThat(GlobalExceptionHandler.resumirErroresValidacion(Map.of()))
                .isEqualTo("La solicitud contiene datos inválidos");
    }

    @Test
    @DisplayName("Devuelve mensajes específicos para JSON mal formado, parte multipart faltante, content-type y tipo inválido")
    void debeDescribirErroresHttpInvalidosConClaridad() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/stores");

        ResponseEntity<ErrorResponseDTO> rJson = handler.handleInvalidHttpRequest(
                new HttpMessageNotReadableException("bad json", (HttpInputMessage) null), request);
        assertThat(rJson.getBody().getDetail()).contains("JSON está mal formado");

        ResponseEntity<ErrorResponseDTO> rPart = handler.handleInvalidHttpRequest(
                new MissingServletRequestPartException("file"), request);
        assertThat(rPart.getBody().getDetail()).contains("multipart");

        ResponseEntity<ErrorResponseDTO> rMedia = handler.handleInvalidHttpRequest(
                new HttpMediaTypeNotSupportedException("text/plain"), request);
        assertThat(rMedia.getBody().getDetail()).contains("tipo de contenido");

        ResponseEntity<ErrorResponseDTO> rMismatch = handler.handleInvalidHttpRequest(
                new MethodArgumentTypeMismatchException("x", Long.class, "id", null, null), request);
        assertThat(rMismatch.getBody().getDetail()).contains("tipo o formato inválido");
    }
}
