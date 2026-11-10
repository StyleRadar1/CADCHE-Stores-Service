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

    @Test
    @DisplayName("SR-RF-74: Debe usar endpoint por defecto si se provee null o cadena en blanco")
    void debeUsarEndpointPorDefectoConValoresNulosOVacios() {
        AzureBlobStorageAdapter adapterNull = new AzureBlobStorageAdapter(null);
        AzureBlobStorageAdapter adapterBlanco = new AzureBlobStorageAdapter("   ");

        InputStream stream = new ByteArrayInputStream("datos".getBytes());
        String urlNull = adapterNull.subirArchivo("fotos", "1.webp", stream, "image/webp", 5);
        String urlBlanco = adapterBlanco.subirArchivo("fotos", "2.webp", stream, "image/webp", 5);

        assertThat(urlNull).startsWith("https://styleradarblob.blob.core.windows.net/fotos");
        assertThat(urlBlanco).startsWith("https://styleradarblob.blob.core.windows.net/fotos");
    }

    @Test
    @DisplayName("SR-RF-74: Debe eliminar slashes finales del endpoint configurado")
    void debeNormalizarSlashesFinales() {
        AzureBlobStorageAdapter adapterConSlashes = new AzureBlobStorageAdapter("https://customblob.azure.com///");
        InputStream stream = new ByteArrayInputStream("datos".getBytes());
        String url = adapterConSlashes.subirArchivo("fotos", "1.webp", stream, "image/webp", 5);

        assertThat(url).isEqualTo("https://customblob.azure.com/fotos/1.webp");
    }
}

