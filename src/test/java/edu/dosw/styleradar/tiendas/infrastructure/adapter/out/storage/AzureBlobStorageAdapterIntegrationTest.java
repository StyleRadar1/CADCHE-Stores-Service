package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenamientoNoDisponibleException;

/**
 * CADCHE-117: el archivo queda guardado de verdad en Blob Storage (Azurite) y la URL devuelta abre.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Integración - AzureBlobStorageAdapter contra Azurite (CADCHE-117)")
class AzureBlobStorageAdapterIntegrationTest {

    private static final String CUENTA_DEV = "devstoreaccount1";
    private static final String CLAVE_DEV =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> AZURITE = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--blobPort", "10000", "--skipApiVersionCheck")
            .withExposedPorts(10000);

    private BlobServiceClient serviceClient;
    private AzureBlobStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        String endpoint = "http://" + AZURITE.getHost() + ":" + AZURITE.getMappedPort(10000) + "/" + CUENTA_DEV;
        serviceClient = new BlobServiceClientBuilder()
                .connectionString("DefaultEndpointsProtocol=http;AccountName=" + CUENTA_DEV + ";AccountKey=" + CLAVE_DEV
                        + ";BlobEndpoint=" + endpoint + ";")
                .buildClient();
        adapter = new AzureBlobStorageAdapter(serviceClient, Set.of("tiendas"));
    }

    @Test
    @DisplayName("Sube la foto a un contenedor público, la URL abre con el mismo contenido y tipo, y se elimina")
    void subeFotoPublicaYLaUrlAbre() throws Exception {
        byte[] foto = "contenido-jpeg".getBytes(StandardCharsets.UTF_8);

        String url = adapter.subirArchivo("tiendas", "900123456-1/foto.jpg", new ByteArrayInputStream(foto),
                "image/jpeg", foto.length);

        HttpResponse<byte[]> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(foto);
        assertThat(respuesta.headers().firstValue("Content-Type")).contains("image/jpeg");

        adapter.eliminarArchivo(url);
        assertThat(serviceClient.getBlobContainerClient("tiendas").getBlobClient("900123456-1/foto.jpg").exists())
                .isFalse();
    }

    @Test
    @DisplayName("El RUT se guarda en un contenedor privado y existe en Blob Storage")
    void guardaRutEnContenedorPrivado() {
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);

        String url = adapter.subirArchivo("documentos-almacenes", "900123456-1/rut.pdf", new ByteArrayInputStream(pdf),
                "application/pdf", pdf.length);

        assertThat(url).endsWith("/documentos-almacenes/900123456-1/rut.pdf");
        assertThat(serviceClient.getBlobContainerClient("documentos-almacenes")
                .getBlobClient("900123456-1/rut.pdf").getProperties().getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Ignora URLs ajenas a la cuenta y responde 503 si Blob Storage no está disponible")
    void ignoraUrlsAjenasYFallaComoNoDisponible() {
        assertThatCode(() -> adapter.eliminarArchivo("https://otra-cuenta.blob.core.windows.net/tiendas/x.jpg"))
                .doesNotThrowAnyException();
        assertThatCode(() -> adapter.eliminarArchivo(null)).doesNotThrowAnyException();

        AzureBlobStorageAdapter caido = new AzureBlobStorageAdapter(new BlobServiceClientBuilder()
                .connectionString("DefaultEndpointsProtocol=http;AccountName=" + CUENTA_DEV + ";AccountKey=" + CLAVE_DEV
                        + ";BlobEndpoint=http://127.0.0.1:1/" + CUENTA_DEV + ";")
                .buildClient(), Set.of());
        byte[] datos = {1, 2, 3};
        assertThatThrownBy(() -> caido.subirArchivo("tiendas", "a.jpg", new ByteArrayInputStream(datos), "image/jpeg", 3))
                .isInstanceOf(AlmacenamientoNoDisponibleException.class);
    }
}
