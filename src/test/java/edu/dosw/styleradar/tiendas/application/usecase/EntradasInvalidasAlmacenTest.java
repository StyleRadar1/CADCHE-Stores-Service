package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@DisplayName("Entradas inválidas: archivo RUT (B-3A.2) y decisión de verificación (SR-RF-62)")
class EntradasInvalidasAlmacenTest {

    private final AlmacenRepositoryPort repositoryPort = mock(AlmacenRepositoryPort.class);
    private final AlmacenamientoBlobPort blob = mock(AlmacenamientoBlobPort.class);
    private final RegistrarAlmacenUseCaseImpl registrar =
            new RegistrarAlmacenUseCaseImpl(repositoryPort, blob, new AutorizacionAlmacenValidator());
    private final VerificarAlmacenUseCaseImpl verificar =
            new VerificarAlmacenUseCaseImpl(repositoryPort, new AutorizacionAdminStyleRadarValidator());

    private static final long MB = 1024L * 1024L;

    @ParameterizedTest(name = "{0} · {1} · {2} bytes")
    @CsvSource(value = {
            "rut.pdf, application/pdf, 0",
            "rut.pdf, application/pdf, 5242881",
            "rut.txt, text/plain, 1024",
            "NULL, NULL, 1024",
            "foto.png, image/png, 1024"
    }, nullValues = "NULL")
    void rutQueNoEsPdfDeHasta5MbSeRechazaSinSubirAlBlob(String nombre, String tipo, long tamano) {
        ByteArrayInputStream contenido = new ByteArrayInputStream(new byte[0]);
        assertThatThrownBy(() -> registrar.subirDocumentoRut(nombre, tipo, tamano, contenido, 15L, "STORE_ADMIN"))
                .isInstanceOf(ReglaDeNegocioException.class);
        verifyNoInteractions(blob);
    }

    @Test
    void rutSeAceptaPorExtensionOPorTipoMime() {
        when(blob.subirArchivo(eq("documentos-almacenes"), anyString(), any(), eq("application/pdf"), anyLong()))
                .thenReturn("https://blob/rut.pdf");
        ByteArrayInputStream contenido = new ByteArrayInputStream(new byte[] {1});

        assertThat(registrar.subirDocumentoRut("RUT.PDF", null, MB, contenido, 15L, "STORE_ADMIN").url())
                .isEqualTo("https://blob/rut.pdf");
        assertThat(registrar.subirDocumentoRut(null, "Application/PDF", 5 * MB, contenido, 15L, "STORE_ADMIN").url())
                .isEqualTo("https://blob/rut.pdf");
    }

    @Test
    void rutSinTokenEsFalloDeAutenticacion() {
        ByteArrayInputStream contenido = new ByteArrayInputStream(new byte[] {1});
        assertThatThrownBy(() -> registrar.subirDocumentoRut("rut.pdf", "application/pdf", 10, contenido, null, "STORE_ADMIN"))
                .isInstanceOf(FalloAutenticacionException.class);
    }

    @Test
    void decisionInvalidaSeRechazaAntesDeBuscarElAlmacen() {
        String admin = AutorizacionAdminStyleRadarValidator.ROL_ADMIN_STYLERADAR;
        DecisionVerificacionRequestDTO sinDecision = new DecisionVerificacionRequestDTO();
        DecisionVerificacionRequestDTO rechazoSinMotivo = new DecisionVerificacionRequestDTO();
        rechazoSinMotivo.setDecision(DecisionVerificacionAlmacen.REJECTED);
        DecisionVerificacionRequestDTO rechazoMotivoCorto = new DecisionVerificacionRequestDTO();
        rechazoMotivoCorto.setDecision(DecisionVerificacionAlmacen.REJECTED);
        rechazoMotivoCorto.setMotivo("corto");

        assertThatThrownBy(() -> verificar.decidir("900123456-1", null, 4L, admin))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> verificar.decidir("900123456-1", sinDecision, 4L, admin))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> verificar.decidir("900123456-1", rechazoSinMotivo, 4L, admin))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> verificar.decidir("900123456-1", rechazoMotivoCorto, 4L, admin))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> verificar.decidir("900123456-1", sinDecision, null, admin))
                .isInstanceOf(FalloAutenticacionException.class);
        verifyNoInteractions(repositoryPort);
    }
}
