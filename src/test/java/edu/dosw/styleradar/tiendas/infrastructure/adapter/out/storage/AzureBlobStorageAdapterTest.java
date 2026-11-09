package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pruebas Unitarias e Integración - AzureBlobStorageAdapter (SR-RF-74)")
class AzureBlobStorageAdapterTest {

    private AzureBlobStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AzureBlobStorageAdapter("https://styleradarblob.blob.core.windows.net");
    }

    @Test
    @DisplayName("SR-RF-74 Integración: Debe generar la URL en Azure Blob Storage para un contenedor y archivo")
    void debeSubirArchivoYRetornarUrlAzureBlob() {
        InputStream stream = new ByteArrayInputStream("contenido-imagen".getBytes());

        String url = adapter.subirArchivo("tiendas", "900123456-1/foto.webp", stream, "image/webp", 16);

        assertThat(url).isEqualTo("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto.webp");
    }

    @Test
    @DisplayName("SR-RF-74 Integración: Debe permitir eliminar un blob sin arrojar excepciones")
    void debeEliminarBlobCorrectamente() {
        assertDoesNotThrow(() ->
                adapter.eliminarArchivo("https://styleradarblob.blob.core.windows.net/tiendas/900123456-1/foto.webp"));
    }
}

