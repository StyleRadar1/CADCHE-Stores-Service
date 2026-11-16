package edu.dosw.styleradar.tiendas.domain.model;

/**
 * Catálogo de códigos de error de ms-tiendas (CADCHE-121). El front decide el texto por {@code codigo}
 * y no por el mensaje; cada código lleva su estado HTTP.
 */
public enum CodigoError {

    DATOS_INVALIDOS(400),
    REGLA_DE_NEGOCIO(400),
    NO_AUTENTICADO(401),
    ACCESO_DENEGADO(403),
    RECURSO_NO_ENCONTRADO(404),
    METODO_NO_PERMITIDO(405),
    CONFLICTO(409),
    ARCHIVO_DEMASIADO_GRANDE(413),
    OPERACION_NO_PROCESABLE(422),
    ERROR_INTERNO(500),
    SERVICIO_NO_DISPONIBLE(503);

    private final int estadoHttp;

    CodigoError(int estadoHttp) {
        this.estadoHttp = estadoHttp;
    }

    public int estadoHttp() {
        return estadoHttp;
    }
}
