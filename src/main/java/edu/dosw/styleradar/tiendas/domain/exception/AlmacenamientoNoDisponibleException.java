package edu.dosw.styleradar.tiendas.domain.exception;

/**
 * El repositorio de archivos (Azure Blob Storage) no está configurado o no respondió. Se traduce a {@code 503}
 * para que el cliente reintente; nunca se devuelve una URL de un archivo que no quedó guardado (CADCHE-117).
 */
public class AlmacenamientoNoDisponibleException extends RuntimeException {

    public AlmacenamientoNoDisponibleException(String message) {
        super(message);
    }

    public AlmacenamientoNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
