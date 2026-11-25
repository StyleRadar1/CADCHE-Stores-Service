package edu.dosw.styleradar.tiendas.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Excepciones de registro duplicado: campo del error que llega al ProblemDetail (409)")
class RegistroDuplicadoExceptionTest {

    @Test
    void fundacion_infiereElCampoDesdeElMensaje() {
        assertEquals(Map.of("nit", "Ya existe una fundación con el NIT 900"),
                new FundacionYaRegistradaException("Ya existe una fundación con el NIT 900").getValidationErrors());
        assertEquals(Map.of("name", "El nombre ya está registrado"),
                new FundacionYaRegistradaException("El nombre ya está registrado").getValidationErrors());
        assertEquals(Map.of("representativeId", "El representante ya tiene fundación"),
                new FundacionYaRegistradaException("El representante ya tiene fundación").getValidationErrors());
        assertTrue(new FundacionYaRegistradaException("Duplicada").getValidationErrors().isEmpty());
        assertTrue(new FundacionYaRegistradaException((String) null).getValidationErrors().isEmpty());
    }

    @Test
    void fundacion_campoExplicitoSoloSiCampoYMensajeExisten() {
        assertEquals(Map.of("nit", "dup"), new FundacionYaRegistradaException("nit", "dup").getValidationErrors());
        assertTrue(new FundacionYaRegistradaException(null, "dup").getValidationErrors().isEmpty());
        assertTrue(new FundacionYaRegistradaException(" ", "dup").getValidationErrors().isEmpty());
        assertTrue(new FundacionYaRegistradaException("nit", (String) null).getValidationErrors().isEmpty());
    }

    @Test
    void fundacion_mapaExplicitoSeCopia() {
        assertEquals(Map.of("a", "b"), new FundacionYaRegistradaException("m", Map.of("a", "b")).getValidationErrors());
        assertTrue(new FundacionYaRegistradaException("m", (Map<String, String>) null).getValidationErrors().isEmpty());
    }

    @Test
    void almacen_infiereNitYAceptaCampoOMapaExplicito() {
        assertEquals(Map.of("nit", "NIT duplicado"), new AlmacenYaRegistradoException("NIT duplicado").getValidationErrors());
        assertTrue(new AlmacenYaRegistradoException("Duplicado").getValidationErrors().isEmpty());
        assertTrue(new AlmacenYaRegistradoException((String) null).getValidationErrors().isEmpty());

        assertEquals(Map.of("nit", "dup"), new AlmacenYaRegistradoException("nit", "dup").getValidationErrors());
        assertTrue(new AlmacenYaRegistradoException(null, "dup").getValidationErrors().isEmpty());
        assertTrue(new AlmacenYaRegistradoException("", "dup").getValidationErrors().isEmpty());
        assertTrue(new AlmacenYaRegistradoException("nit", (String) null).getValidationErrors().isEmpty());

        assertEquals(Map.of("a", "b"), new AlmacenYaRegistradoException("m", Map.of("a", "b")).getValidationErrors());
        assertTrue(new AlmacenYaRegistradoException("m", (Map<String, String>) null).getValidationErrors().isEmpty());
    }
}
