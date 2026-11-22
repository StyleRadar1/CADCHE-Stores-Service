package edu.dosw.styleradar.tiendas.domain.model;

/**
 * Catálogo de códigos de error de ms-tiendas (CADCHE-121). El front decide el texto por {@code codigo}
 * y no por el mensaje; cada código lleva su estado HTTP.
 */
public enum CodigoError {

    INVALID_DATA(400),
    BUSINESS_RULE_VIOLATION(400),
    UNAUTHENTICATED(401),
    ACCESS_DENIED(403),
    RESOURCE_NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    FILE_TOO_LARGE(413),
    UNPROCESSABLE_OPERATION(422),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int estadoHttp;

    CodigoError(int estadoHttp) {
        this.estadoHttp = estadoHttp;
    }

    public int estadoHttp() {
        return estadoHttp;
    }
}
