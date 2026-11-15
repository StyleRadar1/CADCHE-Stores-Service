package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AzureBlobStorageAdapter implements AlmacenamientoBlobPort {

    private final String endpointBase;
    private final Map<String, byte[]> blobsAlmacenados = new ConcurrentHashMap<>();

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

        byte[] payload = leerContenido(contenido);
        String urlPublica = endpointBase + "/" + contenedor + "/" + rutaDestino;
        blobsAlmacenados.put(urlPublica, payload);

        log.info("Blob almacenado en Azure Blob ({} bytes leídos). URL resultante: {}", payload.length, urlPublica);
        return urlPublica;
    }

    @Override
    public void eliminarArchivo(String urlArchivo) {
        log.info("Eliminando blob de Azure Blob Storage: {}", urlArchivo);
        if (urlArchivo != null && !urlArchivo.isBlank()) {
            blobsAlmacenados.remove(urlArchivo);
        }
    }

    public boolean existeBlob(String urlArchivo) {
        return urlArchivo != null && blobsAlmacenados.containsKey(urlArchivo);
    }

    public Optional<byte[]> obtenerBlob(String urlArchivo) {
        if (urlArchivo == null) {
            return Optional.empty();
        }
        byte[] bytes = blobsAlmacenados.get(urlArchivo);
        return bytes != null ? Optional.of(bytes.clone()) : Optional.empty();
    }

    private byte[] leerContenido(InputStream contenido) {
        if (contenido == null) {
            return new byte[0];
        }
        try {
            return contenido.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible almacenar el archivo en el repositorio de objetos", ex);
        }
    }
}
