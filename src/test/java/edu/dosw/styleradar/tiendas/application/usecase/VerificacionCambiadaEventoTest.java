package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.domain.event.VerificacionAlmacenCambiadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaPendienteException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@DisplayName("Evento store.verification.changed en registro, decisión y reapertura de la verificación")
class VerificacionCambiadaEventoTest {

    private static final String NIT = "900123456-1";
    private static final String ADMIN = "ROLE_STYLERADAR_ADMIN";

    private AlmacenRepositoryPort repositorio;
    private AlmacenEventPublisherPort eventos;
    private VerificarAlmacenUseCaseImpl verificar;

    @BeforeEach
    void setUp() {
        repositorio = mock(AlmacenRepositoryPort.class);
        eventos = mock(AlmacenEventPublisherPort.class);
        verificar = new VerificarAlmacenUseCaseImpl(repositorio, new AutorizacionAdminStyleRadarValidator(), eventos);
        when(repositorio.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Almacen almacen(EstadoVerificacionAlmacen estado) {
        return Almacen.builder().nit(NIT).nombreComercial("Atelier Urbano")
                .estadoVerificacion(estado).urlRutPdf("https://blob/rut.pdf").build();
    }

    private VerificacionAlmacenCambiadaEvent publicado() {
        ArgumentCaptor<VerificacionAlmacenCambiadaEvent> captor = ArgumentCaptor.forClass(VerificacionAlmacenCambiadaEvent.class);
        verify(eventos).publicarVerificacionCambiada(captor.capture());
        return captor.getValue();
    }

    private static DecisionVerificacionRequestDTO decision(DecisionVerificacionAlmacen d, String motivo) {
        DecisionVerificacionRequestDTO r = new DecisionVerificacionRequestDTO();
        r.setDecision(d);
        r.setMotivo(motivo);
        return r;
    }

    @Test
    @DisplayName("Aprobar publica PENDING -> APPROVED con eventId y fecha")
    void aprobarPublica() {
        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.PENDING)));

        verificar.decidir(NIT, decision(DecisionVerificacionAlmacen.APPROVED, "RUT validado"), 4L, ADMIN);

        VerificacionAlmacenCambiadaEvent e = publicado();
        assertThat(e.getAlmacenNit()).isEqualTo(NIT);
        assertThat(e.getEstadoAnterior()).isEqualTo(EstadoVerificacionAlmacen.PENDING);
        assertThat(e.getEstado()).isEqualTo(EstadoVerificacionAlmacen.APPROVED);
        assertThat(e.getEventoId()).isNotBlank();
        assertThat(e.getFechaCambio()).isNotNull();
    }

    @Test
    @DisplayName("Rechazar publica PENDING -> REJECTED")
    void rechazarPublica() {
        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.PENDING)));

        verificar.decidir(NIT, decision(DecisionVerificacionAlmacen.REJECTED, "El RUT no corresponde al NIT"), 4L, ADMIN);

        VerificacionAlmacenCambiadaEvent e = publicado();
        assertThat(e.getEstadoAnterior()).isEqualTo(EstadoVerificacionAlmacen.PENDING);
        assertThat(e.getEstado()).isEqualTo(EstadoVerificacionAlmacen.REJECTED);
    }

    @Test
    @DisplayName("Reabrir la revisión publica REJECTED -> PENDING")
    void reabrirPublica() {
        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.REJECTED)));

        verificar.solicitarRevision(NIT, 4L, ADMIN);

        VerificacionAlmacenCambiadaEvent e = publicado();
        assertThat(e.getEstadoAnterior()).isEqualTo(EstadoVerificacionAlmacen.REJECTED);
        assertThat(e.getEstado()).isEqualTo(EstadoVerificacionAlmacen.PENDING);
    }

    @Test
    @DisplayName("Sin cambio de estado no se publica nada")
    void sinCambioNoPublica() {
        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.APPROVED)));
        assertThatThrownBy(() -> verificar.decidir(NIT, decision(DecisionVerificacionAlmacen.APPROVED, "ok"), 4L, ADMIN))
                .isInstanceOf(AlmacenYaDecididoException.class);

        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.PENDING)));
        assertThatThrownBy(() -> verificar.solicitarRevision(NIT, 4L, ADMIN))
                .isInstanceOf(AlmacenYaPendienteException.class);

        verify(eventos, never()).publicarVerificacionCambiada(any());
    }

    @Test
    @DisplayName("Registrar un almacén publica el estado PENDING sin estado anterior")
    void registrarPublica() {
        RegistrarAlmacenUseCaseImpl registrar = new RegistrarAlmacenUseCaseImpl(
                repositorio, mock(AlmacenamientoBlobPort.class), new AutorizacionAlmacenValidator(), eventos);
        when(repositorio.existsByNit(NIT)).thenReturn(false);
        when(repositorio.existeAlmacenParaUsuario(15L)).thenReturn(false);
        RegistrarAlmacenRequestDTO req = new RegistrarAlmacenRequestDTO(
                NIT, "Moda Urbana", "Ropa exclusiva", "+57 300 123 4567", "contacto@modaurbana.com", 4.65, -74.05,
                Set.of(CategoriaAlmacen.CASUAL),
                "https://styleradarblob.blob.core.windows.net/documentos-almacenes/rut/15/doc.pdf");

        registrar.registrarAlmacen(req, 15L, "ROLE_STORE_ADMIN");

        VerificacionAlmacenCambiadaEvent e = publicado();
        assertThat(e.getAlmacenNit()).isEqualTo(NIT);
        assertThat(e.getEstadoAnterior()).isNull();
        assertThat(e.getEstado()).isEqualTo(EstadoVerificacionAlmacen.PENDING);
    }

    @Test
    @DisplayName("Sin publicador configurado los casos de uso siguen funcionando")
    void sinPublicador() {
        VerificarAlmacenUseCaseImpl sinEventos = new VerificarAlmacenUseCaseImpl(
                repositorio, new AutorizacionAdminStyleRadarValidator());
        when(repositorio.findByNit(NIT)).thenReturn(Optional.of(almacen(EstadoVerificacionAlmacen.PENDING)));

        assertThat(sinEventos.decidir(NIT, decision(DecisionVerificacionAlmacen.APPROVED, "ok"), 4L, ADMIN)
                .estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.APPROVED);
    }
}
