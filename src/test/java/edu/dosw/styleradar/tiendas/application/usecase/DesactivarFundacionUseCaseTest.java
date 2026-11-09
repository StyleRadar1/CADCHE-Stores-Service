package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaDesactivadaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - DesactivarFundacionUseCase (SR-RF-65)")
class DesactivarFundacionUseCaseTest {

    @Mock
    private FundacionRepositoryPort repositoryPort;

    @Mock
    private FundacionEventPublisherPort eventPublisherPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @InjectMocks
    private DesactivarFundacionUseCaseImpl useCase;

    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";
    private Fundacion fundacionActiva;

    @BeforeEach
    void setUp() {
        fundacionActiva = Fundacion.builder()
                .id(5L)
                .nombre("Fundación Abrigo y Esperanza")
                .descripcion("Recepción de vestuario y calzado")
                .direccion("Avenida 68 # 45-12")
                .telefono("+57 601 555 4433")
                .email("esperanza@fundacion.org")
                .latitud(4.6800)
                .longitud(-74.0700)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(18L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .causasApoyadas("Adultos mayores en condición de calle")
                .horarioAtencion("Lunes a Sábado 08:00 - 17:00")
                .politicasDonacion("Prendas limpias")
                .activa(true)
                .fechaRegistro(Instant.now().minusSeconds(86400))
                .build();
    }

    @Test
    @DisplayName("Criterio 1 y Criterio 2: Dado una fundación activa, cuando la desactivo, queda inactiva, conserva historial y publica evento RabbitMQ")
    void debeDesactivarFundacionActivaYPublicarEvento() {
        when(repositoryPort.findById(5L)).thenReturn(Optional.of(fundacionActiva));
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FundacionResponseDTO response = useCase.desactivar(5L, "Reestructuración operativa", ADMIN_ROLE);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertFalse(response.isActiva());
        assertEquals("Fundación Abrigo y Esperanza", response.getNombre());

        verify(repositoryPort).save(fundacionActiva);
        assertFalse(fundacionActiva.isActiva());

        ArgumentCaptor<FundacionDesactivadaEvent> eventCaptor = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
        verify(eventPublisherPort).publicarFundacionDesactivada(eventCaptor.capture());

        FundacionDesactivadaEvent eventoPublicado = eventCaptor.getValue();
        assertEquals(5L, eventoPublicado.getFundacionId());
        assertEquals("Fundación Abrigo y Esperanza", eventoPublicado.getNombreFundacion());
        assertEquals(18L, eventoPublicado.getRepresentanteId());
        assertEquals("Reestructuración operativa", eventoPublicado.getMotivo());
        assertNotNull(eventoPublicado.getFechaDesactivacion());
    }

    @Test
    @DisplayName("Criterio 1: Desactivación sin motivo explícito debe usar el motivo por defecto")
    void debeDesactivarConMotivoDefault() {
        when(repositoryPort.findById(5L)).thenReturn(Optional.of(fundacionActiva));
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FundacionResponseDTO response = useCase.desactivar(5L, ADMIN_ROLE);

        assertNotNull(response);
        assertFalse(response.isActiva());

        ArgumentCaptor<FundacionDesactivadaEvent> eventCaptor = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
        verify(eventPublisherPort).publicarFundacionDesactivada(eventCaptor.capture());
        assertEquals(DesactivarFundacionUseCaseImpl.MOTIVO_DEFAULT, eventCaptor.getValue().getMotivo());
    }

    @Test
    @DisplayName("Criterio 3 (409 Conflict): Dado una fundación ya inactiva, cuando la desactivo, recibo 409 y no publica evento")
    void debeLanzarConflictoCuandoFundacionYaEstaInactiva() {
        fundacionActiva.setActiva(false);
        when(repositoryPort.findById(5L)).thenReturn(Optional.of(fundacionActiva));

        assertThrows(FundacionYaDesactivadaException.class,
                () -> useCase.desactivar(5L, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publicarFundacionDesactivada(any());
    }

    @Test
    @DisplayName("Criterio 4 (404 Not Found): Dado una fundación inexistente, cuando la desactivo, recibo 404")
    void debeLanzar404CuandoFundacionNoExiste() {
        when(repositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(FundacionNoEncontradaException.class,
                () -> useCase.desactivar(999L, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publicarFundacionDesactivada(any());
    }

    @Test
    @DisplayName("Criterio 5 (403 Forbidden): Dado que no soy administrador, cuando llamo al endpoint, recibo 403")
    void debeLanzar403CuandoUsuarioNoEsAdmin() {
        doThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR"))
                .when(adminValidator).validarEsAdministradorStyleRadar("ROLE_COMPRADOR");

        assertThrows(AccesoDenegadoAlmacenException.class,
                () -> useCase.desactivar(5L, "ROLE_COMPRADOR"));

        verify(repositoryPort, never()).findById(any());
        verify(repositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publicarFundacionDesactivada(any());
    }

    @Test
    @DisplayName("Validación: Debe lanzar ReglaDeNegocioException cuando el ID es nulo")
    void debeLanzarExcepcionCuandoIdEsNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.desactivar(null, ADMIN_ROLE));
    }
}

