package edu.dosw.styleradar.tiendas.domain.exception;

import java.util.Map;

/**
 * Un dato de entrada no cumple su regla y la regla solo puede revisarse en el dominio (por ejemplo, que el nuevo orden
 * de fotos incluya todas las actuales). Se responde como INVALID_DATA, con el mensaje en {@code errors.<campo>}.
 */
public class DatoInvalidoException extends RuntimeException {

    private final transient Map<String, String> errores;

    public DatoInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.errores = Map.of(campo, mensaje);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
