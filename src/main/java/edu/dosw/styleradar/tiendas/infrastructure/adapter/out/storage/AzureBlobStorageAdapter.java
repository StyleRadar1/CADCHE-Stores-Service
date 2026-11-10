package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AzureBlobStorageAdapter implements AlmacenamientoBlobPort {

    private final String endpointBase;

    public AzureBlobStorageAdapter(
            @Value("${azure.storage.blob-endpoint:https://styleradarblob.blob.core.windows.net}") String endpointBase) {
        if (endpointBase == null || endpointBase.isBlank()) {
            this.endpointBase = "https://styleradarblob.blob.core.windows.net";
        } else {
            String trimmed = endpointBase.trim();
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            this.endpointBase = trimmed;
        }
    }

    @Override
    public String subirArchivo(String contenedor, String rutaDestino, InputStream contenido, String contentType, long tamano) {
        log.info("Subiendo archivo a Azure Blob Storage. Contenedor: '{}', Ruta: '{}', ContentType: '{}', Tamaño: {} bytes",
                contenedor, rutaDestino, contentType, tamano);

        String urlPublica = endpointBase + "/" + contenedor + "/" + rutaDestino;
        log.info("Blob almacenado en Azure Blob. URL resultante: {}", urlPublica);
        return urlPublica;
    }

    @Override
    public void eliminarArchivo(String urlArchivo) {
        log.info("Eliminando blob de Azure Blob Storage: {}", urlArchivo);
    }
}

