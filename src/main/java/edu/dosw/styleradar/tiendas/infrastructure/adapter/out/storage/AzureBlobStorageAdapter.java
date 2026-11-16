package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenamientoNoDisponibleException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptador real de Azure Blob Storage (CADCHE-117): sube fotos de la tienda, RUT del almacén y certificado de la
 * fundación y devuelve la URL del blob guardado. Los contenedores se crean en el primer uso; los listados en
 * {@code contenedoresPublicos} permiten lectura anónima por blob (no se pueden listar) para que la URL abra.
 */
@Slf4j
public class AzureBlobStorageAdapter implements AlmacenamientoBlobPort {

    private static final String MENSAJE_NO_DISPONIBLE =
            "El almacenamiento de archivos no está disponible. Intente nuevamente más tarde.";

    private final BlobServiceClient serviceClient;
    private final Set<String> contenedoresPublicos;
    private final Set<String> contenedoresListos = ConcurrentHashMap.newKeySet();

    public AzureBlobStorageAdapter(BlobServiceClient serviceClient, Set<String> contenedoresPublicos) {
        this.serviceClient = serviceClient;
        this.contenedoresPublicos = Set.copyOf(contenedoresPublicos);
    }

    @Override
    public String subirArchivo(String contenedor, String rutaDestino, InputStream contenido, String contentType, long tamano) {
        try {
            BlobContainerClient cliente = contenedor(contenedor);
            BlobClient blob = cliente.getBlobClient(rutaDestino);
            BlobParallelUploadOptions opciones = new BlobParallelUploadOptions(contenido)
                    .setHeaders(new BlobHttpHeaders().setContentType(contentType));
            blob.uploadWithResponse(opciones, null, Context.NONE);
            log.info("Archivo subido a Azure Blob. Contenedor: '{}', ruta: '{}', {} bytes", contenedor, rutaDestino, tamano);
            return cliente.getBlobContainerUrl() + "/" + codificarRuta(rutaDestino);
        } catch (RuntimeException ex) {
            log.error("No fue posible subir el archivo al contenedor '{}' de Azure Blob", contenedor, ex);
            throw new AlmacenamientoNoDisponibleException(MENSAJE_NO_DISPONIBLE, ex);
        }
    }

    @Override
    public void eliminarArchivo(String urlArchivo) {
        if (urlArchivo == null || urlArchivo.isBlank() || !urlArchivo.startsWith(serviceClient.getAccountUrl())) {
            return;
        }
        try {
            BlobUrlParts partes = BlobUrlParts.parse(urlArchivo);
            serviceClient.getBlobContainerClient(partes.getBlobContainerName())
                    .getBlobClient(partes.getBlobName())
                    .deleteIfExists();
            log.info("Blob eliminado de Azure Blob: {}/{}", partes.getBlobContainerName(), partes.getBlobName());
        } catch (RuntimeException ex) {
            // Limpieza de mejor esfuerzo: un blob huérfano no debe tumbar la operación del usuario.
            log.warn("No fue posible eliminar el blob {} de Azure Blob", urlArchivo, ex);
        }
    }

    private BlobContainerClient contenedor(String nombre) {
        BlobContainerClient cliente = serviceClient.getBlobContainerClient(nombre);
        if (!contenedoresListos.contains(nombre)) {
            if (!cliente.exists()) {
                crearContenedor(cliente, nombre);
            }
            contenedoresListos.add(nombre);
        }
        return cliente;
    }

    /**
     * URL legible: codifica cada segmento pero conserva las barras (el SDK las devuelve como {@code %2F}).
     */
    private static String codificarRuta(String ruta) {
        return Arrays.stream(ruta.split("/", -1))
                .map(segmento -> URLEncoder.encode(segmento, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    private void crearContenedor(BlobContainerClient cliente, String nombre) {
        if (!contenedoresPublicos.contains(nombre)) {
            cliente.createIfNotExists();
            return;
        }
        try {
            cliente.createWithResponse(null, PublicAccessType.BLOB, null, Context.NONE);
        } catch (BlobStorageException ex) {
            if (cliente.exists()) {
                return;
            }
            // La cuenta puede tener deshabilitado el acceso público: se crea privado y se avisa.
            log.warn("La cuenta no permite acceso público; el contenedor '{}' se crea privado", nombre);
            cliente.createIfNotExists();
        }
    }
}
