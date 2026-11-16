package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - VerificarAlmacenUseCase (SR-RF-62)")
class VerificarAlmacenUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @InjectMocks
    private VerificarAlmacenUseCaseImpl useCase;

    private Almacen almacenPendiente;
    private static final String NIT = "900123456-1";
    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";

    @BeforeEach
    void setUp() {
        almacenPendiente = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano Chapinero")
                .descripcion("Tienda urbana")
                .correoContacto("contacto@atelierurbano.co")
                .telefono("+57 601 312 4455")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf("https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut.pdf")
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .build();
    }

    @Test
    @DisplayName("Debe aprobar almacén pendiente exitosamente")
    void debeAprobarAlmacenPendienteExitosamente() {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APROBADO);
        request.setMotivo("NIT y RUT validados");

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenPendiente));
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlmacenResponseDTO response = useCase.decidir(NIT, request, 5L, ADMIN_ROLE);

        verify(adminValidator).validarEsAdministradorStyleRadar(ADMIN_ROLE);
        verify(repositoryPort).save(any(Almacen.class));
        assertEquals(EstadoVerificacionAlmacen.APROBADO, response.estadoVerificacion());
        assertEquals("NIT y RUT validados", response.motivoVerificacion());
        assertEquals(5L, response.administradorDecisionId());
    }

    @Test
    @DisplayName("Debe rechazar almacén pendiente con motivo exitosamente")
    void debeRechazarAlmacenPendienteExitosamente() {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.RECHAZADO);
        request.setMotivo("RUT desactualizado ante la DIAN");

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenPendiente));
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlmacenResponseDTO response = useCase.decidir(NIT, request, 5L, ADMIN_ROLE);

        assertEquals(EstadoVerificacionAlmacen.RECHAZADO, response.estadoVerificacion());
        assertEquals("RUT desactualizado ante la DIAN", response.motivoVerificacion());
    }

    @Test
    @DisplayName("Criterio 5: Dado un almacén inexistente, cuando lo decido, entonces lanza AlmacenNoEncontradoException (404)")
    void debeLanzar404CuandoAlmacenNoExiste() {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APROBADO);

        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());

        assertThrows(AlmacenNoEncontradoException.class, () ->
                useCase.decidir("000000000", request, 5L, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Criterio 4: Debe propagar AccesoDenegadoAlmacenException (403) si no es administrador")
    void debePropagar403CuandoNoEsAdministrador() {
        DecisionVerificacionRequestDTO request = new DecisionVerificacionRequestDTO();
        request.setDecision(DecisionVerificacionAlmacen.APROBADO);

        org.mockito.Mockito.doThrow(new AccesoDenegadoAlmacenException("Requiere rol ADMIN"))
                .when(adminValidator).validarEsAdministradorStyleRadar("ROLE_COMPRADOR");

        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                useCase.decidir(NIT, request, 5L, "ROLE_COMPRADOR"));
    }

    @Test
    @DisplayName("Debe consultar un almacén individual por NIT")
    void debeConsultarUnAlmacenPorNit() {
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenPendiente));

        VerificacionAlmacenResponseDTO response = useCase.consultarUno(NIT, ADMIN_ROLE);

        assertNotNull(response);
        assertEquals(NIT, response.nit());
        assertEquals(EstadoVerificacionAlmacen.PENDIENTE, response.estadoVerificacion());
    }

    @Test
    @DisplayName("Debe consultar almacenes paginados con filtro de estado")
    void debeConsultarAlmacenesPaginadosConFiltro() {
        VerificacionConsultaRequestDTO consulta = new VerificacionConsultaRequestDTO();
        consulta.setEstado(EstadoVerificacionAlmacen.PENDIENTE);
        consulta.setPage(0);
        consulta.setSize(10);

        when(repositoryPort.findByEstadoVerificacion(eq(EstadoVerificacionAlmacen.PENDIENTE), any()))
                .thenReturn(new PageImpl<>(List.of(almacenPendiente), PageRequest.of(0, 10), 1));

        PageResponseDTO<VerificacionAlmacenResponseDTO> response = useCase.consultar(consulta, ADMIN_ROLE);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(NIT, response.getContent().get(0).nit());
    }

    @Test
    @DisplayName("Debe fallar al consultar con paginación inválida")
    void debeFallarConPaginacionInvalida() {
        VerificacionConsultaRequestDTO consulta = new VerificacionConsultaRequestDTO();
        consulta.setPage(-1);
        consulta.setSize(20);

        assertThrows(ReglaDeNegocioException.class, () ->
                useCase.consultar(consulta, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Debe solicitar reapertura de revisión para almacén ya decidido")
    void debeSolicitarReaperturaDeRevision() {
        almacenPendiente.setEstadoVerificacion(EstadoVerificacionAlmacen.RECHAZADO);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenPendiente));
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlmacenResponseDTO response = useCase.solicitarRevision(NIT, 1L, ADMIN_ROLE);

        assertEquals(EstadoVerificacionAlmacen.PENDIENTE, response.estadoVerificacion());
    }
}
