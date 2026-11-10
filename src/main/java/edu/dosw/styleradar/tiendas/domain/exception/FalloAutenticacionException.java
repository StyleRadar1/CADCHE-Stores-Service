package edu.dosw.styleradar.tiendas.domain.exception;

/**
 * Excepción lanzada cuando la autenticación de una solicitud falla debido a
 * un token inválido, expirado, ausente o con firma adulterada (OWASP A07:2025).
 */
public class FalloAutenticacionException extends RuntimeException {

    public FalloAutenticacionException(String message) {
        super(message);
    }

    public FalloAutenticacionException(String message, Throwable cause) {
        super(message, cause);
    }
}

