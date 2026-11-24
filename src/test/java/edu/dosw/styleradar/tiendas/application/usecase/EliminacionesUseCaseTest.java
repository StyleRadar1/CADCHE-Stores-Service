package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@DisplayName("SR-RF-119 y SR-RF-120 (análisis v6): eliminar almacén y fundación")
class EliminacionesUseCaseTest {

    private static final String ADMIN = "ROLE_STYLERADAR_ADMIN";
    private static final String NIT = "900123456-1";

    private AlmacenRepositoryPort almacenes;
    private FundacionRepositoryPort fundaciones;
    private AlmacenamientoBlobPort blob;
    private AlmacenEventPublisherPort eventosAlmacen;
    private FundacionEventPublisherPort eventosFundacion;
    private EliminarAlmacenUseCaseImpl eliminarAlmacen;
    private EliminarFundacionUseCaseImpl eliminarFundacion;

    @BeforeEach
    void setUp() {
        almacenes = mock(AlmacenRepositoryPort.class);
        fundaciones = mock(FundacionRepositoryPort.class);
        blob = mock(AlmacenamientoBlobPort.class);
        eventosAlmacen = mock(AlmacenEventPublisherPort.class);
        eventosFundacion = mock(FundacionEventPublisherPort.class);
        AutorizacionAdminStyleRadarValidator validador = new AutorizacionAdminStyleRadarValidator();
        eliminarAlmacen = new EliminarAlmacenUseCaseImpl(almacenes, blob, eventosAlmacen, validador);
        eliminarFundacion = new EliminarFundacionUseCaseImpl(fundaciones, blob, eventosFundacion, validador);
    }

    @Test
    @DisplayName("SR-RF-119: borra el almacén y sus fotos del Blob y publica store.deleted aunque una foto falle")
    void eliminaAlmacen() {
        when(almacenes.existsByNit(NIT)).thenReturn(true);
        when(almacenes.findFotosByAlmacenNit(NIT)).thenReturn(List.of(
                FotografiaTienda.builder().url("https://blob/a.png").build(),
                FotografiaTienda.builder().url("https://blob/b.png").build(),
                FotografiaTienda.builder().url(" ").build()));
        doThrow(new IllegalStateException("blob caído")).when(blob).eliminarArchivo("https://blob/a.png");

        eliminarAlmacen.eliminar(NIT, ADMIN);

        verify(almacenes).eliminarPorNit(NIT);
        verify(blob).eliminarArchivo("https://blob/b.png");
        ArgumentCaptor<AlmacenEliminadoEvent> ev = ArgumentCaptor.forClass(AlmacenEliminadoEvent.class);
        verify(eventosAlmacen).publicarAlmacenEliminado(ev.capture());
        assertThat(ev.getValue().getAlmacenNit()).isEqualTo(NIT);
        assertThat(ev.getValue().getEventoId()).isNotBlank();
        assertThat(ev.getValue().getFechaEliminacion()).isNotNull();
    }

    @Test
    @DisplayName("SR-RF-119 FA-01 a FA-03: sin token 401, sin rol 403 «para esta operación» y NIT inexistente 404")
    void erroresAlmacen() {
        assertThatThrownBy(() -> eliminarAlmacen.eliminar(NIT, null)).isInstanceOf(FalloAutenticacionException.class);
        assertThatThrownBy(() -> eliminarAlmacen.eliminar(NIT, "ROLE_STORE_ADMIN"))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessage(AutorizacionAdminStyleRadarValidator.MENSAJE_OPERACION);
        assertThatThrownBy(() -> eliminarAlmacen.eliminar(NIT, ADMIN)).isInstanceOf(AlmacenNoEncontradoException.class)
                .hasMessage("No se encontró ningún almacén registrado con el NIT: " + NIT);
        assertThatThrownBy(() -> eliminarAlmacen.eliminar(null, ADMIN)).isInstanceOf(AlmacenNoEncontradoException.class);
        verify(almacenes, never()).eliminarPorNit(any());
        verify(eventosAlmacen, never()).publicarAlmacenEliminado(any());
    }

    @Test
    @DisplayName("SR-RF-120: borra la fundación y su certificado y publica foundation.deleted aunque el Blob falle")
    void eliminaFundacion() {
        when(fundaciones.findById(7L)).thenReturn(Optional.of(
                Fundacion.builder().id(7L).urlCertificadoPersoneriaJuridica("https://blob/cert.pdf").build()));
        doThrow(new IllegalStateException("blob caído")).when(blob).eliminarArchivo("https://blob/cert.pdf");

        eliminarFundacion.eliminar(7L, ADMIN);

        verify(fundaciones).eliminarPorId(7L);
        ArgumentCaptor<FundacionEliminadaEvent> ev = ArgumentCaptor.forClass(FundacionEliminadaEvent.class);
        verify(eventosFundacion).publicarFundacionEliminada(ev.capture());
        assertThat(ev.getValue().getFundacionId()).isEqualTo(7L);
        assertThat(ev.getValue().getEventoId()).isNotBlank();
    }

    @Test
    @DisplayName("SR-RF-120: sin certificado no toca el Blob")
    void fundacionSinCertificado() {
        when(fundaciones.findById(8L)).thenReturn(Optional.of(Fundacion.builder().id(8L).build()));
        eliminarFundacion.eliminar(8L, ADMIN);
        verify(blob, never()).eliminarArchivo(any());
        verify(fundaciones).eliminarPorId(8L);
    }

    @Test
    @DisplayName("SR-RF-120 FA-02 y FA-03: sin rol 403 y fundación inexistente 404")
    void erroresFundacion() {
        assertThatThrownBy(() -> eliminarFundacion.eliminar(7L, "ROLE_BUYER"))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessage(AutorizacionAdminStyleRadarValidator.MENSAJE_OPERACION);
        assertThatThrownBy(() -> eliminarFundacion.eliminar(99L, ADMIN)).isInstanceOf(FundacionNoEncontradaException.class)
                .hasMessage("No existe una fundación aliada con ID: 99");
        assertThatThrownBy(() -> eliminarFundacion.eliminar(null, ADMIN)).isInstanceOf(FundacionNoEncontradaException.class);
        verify(fundaciones, never()).eliminarPorId(any());
    }
}
