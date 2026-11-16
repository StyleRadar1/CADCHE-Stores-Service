package edu.dosw.styleradar.tiendas.infrastructure.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.storage.blob.BlobServiceClientBuilder;

import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage.AzureBlobStorageAdapter;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage.BlobNoDisponibleAdapter;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage.MemoriaBlobStorageAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Elige el adaptador de archivos (CADCHE-117): Azure Blob real cuando hay cadena de conexión (QA, PROD y Azurite en
 * docker), memoria solo si el perfil lo permite (dev/test) y, si no, un adaptador que responde 503.
 */
@Configuration
@Slf4j
public class AlmacenamientoBlobConfig {

    @Bean
    public AlmacenamientoBlobPort almacenamientoBlobPort(
            @Value("${azure.storage.connection-string:}") String cadenaConexion,
            @Value("${azure.storage.contenedores-publicos:tiendas,documentos-almacenes,documentos-fundaciones}")
            String contenedoresPublicos,
            @Value("${azure.storage.memoria-permitida:false}") boolean memoriaPermitida,
            @Value("${azure.storage.blob-endpoint:http://localhost:10000/devstoreaccount1}") String endpointLocal) {
        if (cadenaConexion != null && !cadenaConexion.isBlank()) {
            log.info("Almacenamiento de archivos: Azure Blob Storage");
            return new AzureBlobStorageAdapter(
                    new BlobServiceClientBuilder().connectionString(cadenaConexion).buildClient(),
                    aConjunto(contenedoresPublicos));
        }
        if (memoriaPermitida) {
            log.warn("Almacenamiento de archivos en memoria: solo válido para desarrollo y pruebas");
            return new MemoriaBlobStorageAdapter(endpointLocal);
        }
        log.error("Falta AZURE_STORAGE_CONNECTION_STRING: las subidas de archivos responderán 503");
        return new BlobNoDisponibleAdapter();
    }

    private static Set<String> aConjunto(String valores) {
        return Arrays.stream(valores.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
