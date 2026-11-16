package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenamientoNoDisponibleException;
import edu.dosw.styleradar.tiendas.infrastructure.config.AlmacenamientoBlobConfig;

@DisplayName("CADCHE-117: selección del adaptador de archivos y 503 sin configuración")
class BlobNoDisponibleAdapterTest {

    private final AlmacenamientoBlobConfig config = new AlmacenamientoBlobConfig();

    @Test
    @DisplayName("Sin cadena de conexión en QA/PROD responde 503 y nunca devuelve una URL")
    void sinConfiguracionRespondeNoDisponible() {
        var port = config.almacenamientoBlobPort("", "tiendas", false, "http://localhost");

        assertThat(port).isInstanceOf(BlobNoDisponibleAdapter.class);
        assertThatThrownBy(() -> port.subirArchivo("tiendas", "a.jpg", new ByteArrayInputStream(new byte[]{1}), "image/jpeg", 1))
                .isInstanceOf(AlmacenamientoNoDisponibleException.class);
        assertThatCode(() -> port.eliminarArchivo("https://x/y")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Usa memoria solo si el perfil lo permite y Azure real si hay cadena de conexión")
    void eligeAdaptadorSegunConfiguracion() {
        assertThat(config.almacenamientoBlobPort(null, "tiendas", true, "http://localhost"))
                .isInstanceOf(MemoriaBlobStorageAdapter.class);
        assertThat(config.almacenamientoBlobPort(
                "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=a2V5;BlobEndpoint=http://localhost:10000/devstoreaccount1;",
                "tiendas, documentos-almacenes", false, "http://localhost"))
                .isInstanceOf(AzureBlobStorageAdapter.class);
    }
}
