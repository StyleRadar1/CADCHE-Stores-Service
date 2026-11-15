package edu.dosw.styleradar.tiendas.domain.exception;

import java.util.Map;

public class AlmacenYaRegistradoException extends RuntimeException {

    private final Map<String, String> validationErrors;

    public AlmacenYaRegistradoException(String message) {
        super(message);
        if (message != null && message.toUpperCase().contains("NIT")) {
            this.validationErrors = Map.of("nit", message);
        } else {
            this.validationErrors = Map.of();
        }
    }

    public AlmacenYaRegistradoException(String campo, String message) {
        super(message);
        this.validationErrors = (campo != null && !campo.isBlank() && message != null)
                ? Map.of(campo, message)
                : Map.of();
    }

    public AlmacenYaRegistradoException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? Map.copyOf(validationErrors) : Map.of();
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
