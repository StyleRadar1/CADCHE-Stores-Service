package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaActivaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - ReactivarFundacionUseCase (SR-RF-65 B-6.1)")
class ReactivarFundacionUseCaseTest {

    @Mock
    private FundacionRepositoryPort repositoryPort;

    @Mock
    private FundacionEventPublisherPort eventPublisherPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @InjectMocks
    private ReactivarFundacionUseCaseImpl useCase;

    private static final String ADMIN_ROLE = "ROLE_STYLERADAR_ADMIN";

    @Test
    @DisplayName("SR-RF-65 B-6.1: Debe reactivar una fundación inactiva, publicar FundacionReactivadaEvent y fallar con 409 si ya está activa")
    void debeReactivarFundacionInactivaYValidarYaActiva() {
        Fundacion inactiva = Fundacion.builder()
                .id(5L)
                .nit("900215884-1")
                .nombre("Fundación Abrigo")
                .barrio("Chapinero")
                .representanteId(18L)
                .activa(false)
                .fechaRegistro(Instant.now())
                .build();

        when(repositoryPort.findById(5L)).thenReturn(Optional.of(inactiva));
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(inv -> inv.getArgument(0));

        FundacionResponseDTO resp = useCase.reactivar(5L, ADMIN_ROLE);
        assertTrue(resp.isActiva());

        ArgumentCaptor<FundacionReactivadaEvent> captor = ArgumentCaptor.forClass(FundacionReactivadaEvent.class);
        verify(eventPublisherPort).publicarFundacionReactivada(captor.capture());
        assertEquals(5L, captor.getValue().getFundacionId());
        assertNotNull(captor.getValue().getEventoId());

        // Ahora que inactiva quedó activa=true, un segundo intento debe lanzar FundacionYaActivaException (409)
        assertThrows(FundacionYaActivaException.class, () -> useCase.reactivar(5L, "Reapertura", ADMIN_ROLE));

        when(repositoryPort.findById(999L)).thenReturn(Optional.empty());
        assertThrows(FundacionNoEncontradaException.class, () -> useCase.reactivar(999L, ADMIN_ROLE));
        assertThrows(ReglaDeNegocioException.class, () -> useCase.reactivar(null, ADMIN_ROLE));
    }
}

