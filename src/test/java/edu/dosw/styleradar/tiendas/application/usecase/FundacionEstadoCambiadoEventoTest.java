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

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent.Accion;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@DisplayName("Evento foundation.status.changed: +1 al registrar o reactivar, -1 al desactivar o eliminar una activa")
class FundacionEstadoCambiadoEventoTest {

    private static final String ADMIN = "ROLE_STYLERADAR_ADMIN";

    private FundacionRepositoryPort repositorio;
    private FundacionEventPublisherPort eventos;
    private final AutorizacionAdminStyleRadarValidator validador = new AutorizacionAdminStyleRadarValidator();

    @BeforeEach
    void setUp() {
        repositorio = mock(FundacionRepositoryPort.class);
        eventos = mock(FundacionEventPublisherPort.class);
        when(repositorio.save(any(Fundacion.class))).thenAnswer(inv -> {
            Fundacion f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(7L);
            }
            return f;
        });
    }

    private Fundacion fundacion(boolean activa) {
        return Fundacion.builder().id(7L).nombre("Fundación Abrigo").nit("900215884-1").representanteId(24L)
                .activa(activa).build();
    }

    private FundacionEstadoCambiadoEvent publicado() {
        ArgumentCaptor<FundacionEstadoCambiadoEvent> c = ArgumentCaptor.forClass(FundacionEstadoCambiadoEvent.class);
        verify(eventos).publicarEstadoCambiado(c.capture());
        return c.getValue();
    }

    private static void esperar(FundacionEstadoCambiadoEvent e, Accion accion, boolean activa, int delta) {
        assertThat(e.getFundacionId()).isEqualTo(7L);
        assertThat(e.getAccion()).isEqualTo(accion);
        assertThat(e.isActiva()).isEqualTo(activa);
        assertThat(e.getDelta()).isEqualTo(delta);
        assertThat(e.getEventoId()).isNotBlank();
        assertThat(e.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("Registrar una fundación publica REGISTERED con delta +1")
    void registrar() {
        RegistrarFundacionUseCaseImpl uc = new RegistrarFundacionUseCaseImpl(repositorio, validador,
                mock(AlmacenamientoBlobPort.class), eventos);
        FundacionRequestDTO req = FundacionRequestDTO.builder().nit("900215884-1").nombre("Fundación Abrigo")
                .descripcion("Recolección de prendas").direccion("Calle 85 # 15-32").barrio("Chapinero")
                .telefono("+57 601 742 8899").email("contacto@abrigo.org").latitud(4.67).longitud(-74.05)
                .categoriasAceptadas(Set.of(TipoPrenda.TOP)).representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf").causasApoyadas("Niñez").build();

        uc.registrar(req, ADMIN);

        esperar(publicado(), Accion.REGISTERED, true, 1);
    }

    @Test
    @DisplayName("Desactivar publica DEACTIVATED con delta -1; si ya estaba inactiva no publica")
    void desactivar() {
        DesactivarFundacionUseCaseImpl uc = new DesactivarFundacionUseCaseImpl(repositorio, eventos, validador);
        when(repositorio.findById(7L)).thenReturn(Optional.of(fundacion(true)));
        uc.desactivar(7L, ADMIN);
        esperar(publicado(), Accion.DEACTIVATED, false, -1);

        FundacionEventPublisherPort otros = mock(FundacionEventPublisherPort.class);
        DesactivarFundacionUseCaseImpl uc2 = new DesactivarFundacionUseCaseImpl(repositorio, otros, validador);
        when(repositorio.findById(7L)).thenReturn(Optional.of(fundacion(false)));
        assertThatThrownBy(() -> uc2.desactivar(7L, ADMIN)).isInstanceOf(FundacionYaDesactivadaException.class);
        verify(otros, never()).publicarEstadoCambiado(any());
    }

    @Test
    @DisplayName("Reactivar publica REACTIVATED con delta +1")
    void reactivar() {
        ReactivarFundacionUseCaseImpl uc = new ReactivarFundacionUseCaseImpl(repositorio, eventos, validador);
        when(repositorio.findById(7L)).thenReturn(Optional.of(fundacion(false)));
        uc.reactivar(7L, ADMIN);
        esperar(publicado(), Accion.REACTIVATED, true, 1);
    }

    @Test
    @DisplayName("Eliminar una fundación activa publica DELETED con delta -1; una inactiva no cambia el conteo")
    void eliminar() {
        EliminarFundacionUseCaseImpl uc = new EliminarFundacionUseCaseImpl(repositorio,
                mock(AlmacenamientoBlobPort.class), eventos, validador);
        when(repositorio.findById(7L)).thenReturn(Optional.of(fundacion(true)));
        uc.eliminar(7L, ADMIN);
        esperar(publicado(), Accion.DELETED, false, -1);

        FundacionEventPublisherPort otros = mock(FundacionEventPublisherPort.class);
        EliminarFundacionUseCaseImpl uc2 = new EliminarFundacionUseCaseImpl(repositorio,
                mock(AlmacenamientoBlobPort.class), otros, validador);
        when(repositorio.findById(7L)).thenReturn(Optional.of(fundacion(false)));
        uc2.eliminar(7L, ADMIN);
        verify(otros).publicarFundacionEliminada(any());
        verify(otros, never()).publicarEstadoCambiado(any());
    }

    @Test
    @DisplayName("Registrar sin publicador configurado sigue funcionando")
    void registrarSinPublicador() {
        RegistrarFundacionUseCaseImpl uc = new RegistrarFundacionUseCaseImpl(repositorio, validador,
                mock(AlmacenamientoBlobPort.class));
        FundacionRequestDTO req = FundacionRequestDTO.builder().nit("900215884-1").nombre("Fundación Abrigo")
                .descripcion("d").direccion("Calle 1").barrio("Chapinero").telefono("+57 601 742 8899")
                .email("c@abrigo.org").latitud(4.67).longitud(-74.05).categoriasAceptadas(Set.of(TipoPrenda.TOP))
                .representanteId(24L).urlCertificadoPersoneriaJuridica("https://blob/cert.pdf").causasApoyadas("x").build();
        assertThat(uc.registrar(req, ADMIN).isActiva()).isTrue();
    }
}
