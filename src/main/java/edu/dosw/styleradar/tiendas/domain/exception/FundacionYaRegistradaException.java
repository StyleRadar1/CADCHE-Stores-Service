package edu.dosw.styleradar.tiendas.domain.exception;

import java.util.Locale;
import java.util.Map;

public class FundacionYaRegistradaException extends RuntimeException {

    private final Map<String, String> validationErrors;

    public FundacionYaRegistradaException(String mensaje) {
        super(mensaje);
        this.validationErrors = inferirErroresValidacion(mensaje);
    }

    public FundacionYaRegistradaException(String campo, String mensaje) {
        super(mensaje);
        this.validationErrors = (campo != null && !campo.isBlank() && mensaje != null)
                ? Map.of(campo, mensaje)
                : Map.of();
    }

    public FundacionYaRegistradaException(String mensaje, Map<String, String> validationErrors) {
        super(mensaje);
        this.validationErrors = validationErrors != null ? Map.copyOf(validationErrors) : Map.of();
    }

    public Map<String, String> getValidationErrors() {
        return Map.copyOf(validationErrors);
    }

    private static Map<String, String> inferirErroresValidacion(String mensaje) {
        if (mensaje == null) {
            return Map.of();
        }
        String lower = mensaje.toLowerCase(Locale.ROOT);
        if (lower.contains("nit")) {
            return Map.of("nit", mensaje);
        }
        if (lower.contains("nombre")) {
            return Map.of("name", mensaje);
        }
        if (lower.contains("representante")) {
            return Map.of("representativeId", mensaje);
        }
        return Map.of();
    }
}
