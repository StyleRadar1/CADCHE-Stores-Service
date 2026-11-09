package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - RegistrarFundacionUseCase (SR-RF-63)")
class RegistrarFundacionUseCaseTest {

    @Mock
    private FundacionRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @InjectMocks
    private RegistrarFundacionUseCaseImpl useCase;

    private FundacionRequestDTO requestValido;
    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";

    @BeforeEach
    void setUp() {
        requestValido = FundacionRequestDTO.builder()
                .nombre("Fundación Abrigo Solidario")
                .descripcion("Recolección de prendas")
                .direccion("Calle 85 # 15-32")
                .telefono("+57 601 742 8899")
                .email("contacto@abrigosolidario.org")
                .latitud(4.6712)
                .longitud(-74.0531)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .causasApoyadas("Niñez y adultos mayores")
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Debe registrar fundación exitosamente retornando DTO con ID asignado")
    void debeRegistrarFundacionExitosamente() {
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(false);
        when(repositoryPort.existsByRepresentanteId(requestValido.getRepresentanteId())).thenReturn(false);
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(inv -> {
            Fundacion f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        FundacionResponseDTO response = useCase.registrar(requestValido, ADMIN_ROLE);

        verify(adminValidator).validarEsAdministradorStyleRadar(ADMIN_ROLE);
        verify(repositoryPort).save(any(Fundacion.class));
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Fundación Abrigo Solidario", response.getNombre());
        assertTrue(response.isActiva());
    }

    @Test
    @DisplayName("Criterio 3: Debe lanzar FundacionYaRegistradaException (409) cuando el nombre ya existe")
    void debeLanzar409CuandoNombreYaExiste() {
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class, () ->
                useCase.registrar(requestValido, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Criterio 3b: Debe lanzar FundacionYaRegistradaException (409) cuando el representante ya está asignado")
    void debeLanzar409CuandoRepresentanteYaEstaAsignado() {
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(false);
        when(repositoryPort.existsByRepresentanteId(requestValido.getRepresentanteId())).thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class, () ->
                useCase.registrar(requestValido, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Criterio 4: Debe propagar AccesoDenegadoAlmacenException (403) si no es administrador")
    void debePropagar403CuandoNoEsAdministrador() {
        org.mockito.Mockito.doThrow(new AccesoDenegadoAlmacenException("Requiere rol ADMIN"))
                .when(adminValidator).validarEsAdministradorStyleRadar("ROLE_COMPRADOR");

        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                useCase.registrar(requestValido, "ROLE_COMPRADOR"));
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando el request es nulo")
    void debeFallarCuandoRequestEsNulo() {
        assertThrows(ReglaDeNegocioException.class, () ->
                useCase.registrar(null, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Debe listar todas las fundaciones aliadas para rol administrador")
    void debeListarTodasLasFundaciones() {
        Fundacion f = Fundacion.builder()
                .id(1L)
                .nombre("Fundación Semillas")
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        when(repositoryPort.findAll()).thenReturn(List.of(f));

        List<FundacionResponseDTO> lista = useCase.listarTodas(ADMIN_ROLE);

        assertEquals(1, lista.size());
        assertEquals("Fundación Semillas", lista.get(0).getNombre());
    }
}
