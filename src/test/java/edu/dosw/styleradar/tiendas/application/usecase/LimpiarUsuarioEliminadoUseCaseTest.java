package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.ResultadoLimpiezaUsuario;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;

@DisplayName("Cuenta eliminada en ms-usuarios: limpiar vínculos de almacenes y fundaciones")
class LimpiarUsuarioEliminadoUseCaseTest {

    private AlmacenRepositoryPort almacenes;
    private FundacionRepositoryPort fundaciones;
    private FundacionEventPublisherPort eventos;
    private LimpiarUsuarioEliminadoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        almacenes = mock(AlmacenRepositoryPort.class);
        fundaciones = mock(FundacionRepositoryPort.class);
        eventos = mock(FundacionEventPublisherPort.class);
        useCase = new LimpiarUsuarioEliminadoUseCaseImpl(almacenes, fundaciones, eventos);
    }

    private static Fundacion fundacion(boolean activa) {
        return Fundacion.builder().id(7L).nombre("Fundación Sol").representanteId(42L).activa(activa).build();
    }

    @Test
    @DisplayName("Borra sus vínculos de administrador y desactiva la fundación activa que representaba, con sus eventos")
    void limpiaAsociacionesYDesactivaFundacion() {
        when(almacenes.eliminarAsociacionesDeUsuario(42L)).thenReturn(2L);
        when(fundaciones.findByRepresentanteId(42L)).thenReturn(Optional.of(fundacion(true)));
        when(fundaciones.save(any())).thenAnswer(i -> i.getArgument(0));

        ResultadoLimpiezaUsuario r = useCase.limpiar(42L);

        assertThat(r).isEqualTo(new ResultadoLimpiezaUsuario(2, 7L));
        ArgumentCaptor<Fundacion> guardada = ArgumentCaptor.forClass(Fundacion.class);
        verify(fundaciones).save(guardada.capture());
        assertThat(guardada.getValue().isActiva()).isFalse();
        ArgumentCaptor<FundacionDesactivadaEvent> ev = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
        verify(eventos).publicarFundacionDesactivada(ev.capture());
        assertThat(ev.getValue().getFundacionId()).isEqualTo(7L);
        assertThat(ev.getValue().getRepresentanteId()).isEqualTo(42L);
        assertThat(ev.getValue().getMotivo()).isEqualTo(LimpiarUsuarioEliminadoUseCaseImpl.MOTIVO);
        assertThat(ev.getValue().getFechaDesactivacion()).isNotNull();
        verify(eventos).publicarEstadoCambiado(any(FundacionEstadoCambiadoEvent.class));
    }

    @Test
    @DisplayName("Idempotente: si la fundación ya estaba inactiva no la toca ni publica eventos")
    void fundacionYaInactiva() {
        when(fundaciones.findByRepresentanteId(42L)).thenReturn(Optional.of(fundacion(false)));

        assertThat(useCase.limpiar(42L)).isEqualTo(new ResultadoLimpiezaUsuario(0, null));
        verify(fundaciones, never()).save(any());
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("Un usuario sin almacenes ni fundación no cambia nada")
    void usuarioSinVinculos() {
        when(fundaciones.findByRepresentanteId(9L)).thenReturn(Optional.empty());

        assertThat(useCase.limpiar(9L)).isEqualTo(new ResultadoLimpiezaUsuario(0, null));
        verify(almacenes).eliminarAsociacionesDeUsuario(9L);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("Un userId nulo, cero o negativo se ignora sin tocar la base de datos")
    void usuarioInvalido() {
        for (Long id : new Long[] {null, 0L, -3L}) {
            assertThat(useCase.limpiar(id)).isEqualTo(new ResultadoLimpiezaUsuario(0, null));
        }
        verifyNoInteractions(almacenes, fundaciones, eventos);
    }
}
