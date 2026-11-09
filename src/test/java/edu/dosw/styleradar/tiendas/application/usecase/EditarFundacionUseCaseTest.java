package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - EditarFundacionUseCase (SR-RF-64)")
class EditarFundacionUseCaseTest {

    @Mock
    private FundacionRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @InjectMocks
    private EditarFundacionUseCaseImpl useCase;

    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";
    private Fundacion fundacionExistente;
    private FundacionRequestDTO requestEdicionValido;

    @BeforeEach
    void setUp() {
        fundacionExistente = Fundacion.builder()
                .id(1L)
                .nombre("Fundación Manos Unidas")
                .descripcion("Apoyo social básico")
                .direccion("Calle 10 # 5-20")
                .telefono("+57 601 222 3344")
                .email("contacto@manosunidas.org")
                .latitud(4.6097)
                .longitud(-74.0817)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(15L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .causasApoyadas("Comedores comunitarios")
                .horarioAtencion("Lunes a Viernes 08:00 - 16:00")
                .politicasDonacion("Prendas limpias")
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        requestEdicionValido = FundacionRequestDTO.builder()
                .nombre("Fundación Manos Unidas Internacional")
                .descripcion("Programa ampliado de donaciones")
                .direccion("Carrera 15 # 85-30")
                .telefono("+57 311 555 6677")
                .email("nuevo@manosunidas.org")
                .latitud(4.6710)
                .longitud(-74.0550)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(25L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert-actualizado.pdf")
                .causasApoyadas("Infancia vulnerable")
                .horarioAtencion("Lunes a Sábado 07:00 - 18:00")
                .politicasDonacion("Ropa en perfecto estado")
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Dado una fundación existente y datos válidos, cuando la edito, entonces los cambios quedan guardados")
    void debeEditarFundacionExitosamente() {
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(fundacionExistente));
        when(repositoryPort.existsByNombreIgnoreCaseAndIdNot("Fundación Manos Unidas Internacional", 1L)).thenReturn(false);
        when(repositoryPort.existsByRepresentanteIdAndIdNot(25L, 1L)).thenReturn(false);
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FundacionResponseDTO response = useCase.editar(1L, requestEdicionValido, ADMIN_ROLE);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Fundación Manos Unidas Internacional", response.getNombre());
        assertEquals("+57 311 555 6677", response.getTelefono());
        assertEquals("Lunes a Sábado 07:00 - 18:00", response.getHorarioAtencion());
        assertEquals("Ropa en perfecto estado", response.getPoliticasDonacion());
        assertEquals(25L, response.getRepresentanteId());
        verify(repositoryPort).save(any(Fundacion.class));
    }

    @Test
    @DisplayName("Criterio 2: Dado una fundación inexistente, cuando la edito, entonces recibo 404 (FundacionNoEncontradaException)")
    void debeLanzarExcepcionCuandoFundacionNoExiste() {
        when(repositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThrows(FundacionNoEncontradaException.class,
                () -> useCase.editar(999L, requestEdicionValido, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: Dado datos inválidos (nombre nulo), cuando la edito, entonces recibo 400 y no cambia nada")
    void debeLanzarExcepcionCuandoNombreEsNulo() {
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(fundacionExistente));
        requestEdicionValido.setNombre(null);

        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.editar(1L, requestEdicionValido, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: Dado datos inválidos (representanteId nulo), cuando la edito, entonces recibo 400")
    void debeLanzarExcepcionCuandoRepresentanteEsNulo() {
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(fundacionExistente));
        requestEdicionValido.setRepresentanteId(null);

        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.editar(1L, requestEdicionValido, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 4: Dado que no soy administrador, cuando llamo al endpoint, entonces recibo 403 (AccesoDenegadoAlmacenException)")
    void debeLanzarExcepcionCuandoNoEsAdmin() {
        doThrow(new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR"))
                .when(adminValidator).validarEsAdministradorStyleRadar("ROLE_COMPRADOR");

        assertThrows(AccesoDenegadoAlmacenException.class,
                () -> useCase.editar(1L, requestEdicionValido, "ROLE_COMPRADOR"));

        verify(repositoryPort, never()).findById(any());
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Conflicto 409: Dado un nombre ya existente en otra fundación, debe lanzar FundacionYaRegistradaException")
    void debeLanzarConflictoCuandoNombreYaExisteEnOtraFundacion() {
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(fundacionExistente));
        when(repositoryPort.existsByNombreIgnoreCaseAndIdNot(eq("Fundación Manos Unidas Internacional"), eq(1L)))
                .thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class,
                () -> useCase.editar(1L, requestEdicionValido, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Conflicto 409: Dado un representante ya asignado a otra fundación, debe lanzar FundacionYaRegistradaException")
    void debeLanzarConflictoCuandoRepresentanteYaPerteneceAOtraFundacion() {
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(fundacionExistente));
        when(repositoryPort.existsByNombreIgnoreCaseAndIdNot(any(), eq(1L))).thenReturn(false);
        when(repositoryPort.existsByRepresentanteIdAndIdNot(eq(25L), eq(1L))).thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class,
                () -> useCase.editar(1L, requestEdicionValido, ADMIN_ROLE));

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Validación: Debe lanzar ReglaDeNegocioException cuando el ID es nulo")
    void debeLanzarExcepcionCuandoIdEsNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.editar(null, requestEdicionValido, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Validación: Debe lanzar ReglaDeNegocioException cuando el request es nulo")
    void debeLanzarExcepcionCuandoRequestEsNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> useCase.editar(1L, null, ADMIN_ROLE));
    }
}

