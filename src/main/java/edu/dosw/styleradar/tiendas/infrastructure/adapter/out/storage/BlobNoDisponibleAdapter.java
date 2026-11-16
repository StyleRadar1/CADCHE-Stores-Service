package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import java.io.InputStream;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenamientoNoDisponibleException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;

/**
 * Se usa en QA/PROD cuando falta {@code AZURE_STORAGE_CONNECTION_STRING}: el servicio arranca, pero toda subida
 * responde {@code 503} en lugar de devolver una URL de un archivo que nunca se guardó.
 */
public class BlobNoDisponibleAdapter implements AlmacenamientoBlobPort {

    @Override
    public String subirArchivo(String contenedor, String rutaDestino, InputStream contenido, String contentType, long tamano) {
        throw new AlmacenamientoNoDisponibleException(
                "El almacenamiento de archivos no está configurado. Intente nuevamente más tarde.");
    }

    @Override
    public void eliminarArchivo(String urlArchivo) {
        // Sin almacenamiento configurado no hay nada que eliminar.
    }
}
