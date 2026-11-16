package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
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

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.model.TipoPrenda;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - RegistrarFundacionUseCase (SR-RF-63)")
class RegistrarFundacionUseCaseTest {

    @Mock
    private FundacionRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @Mock
    private AlmacenamientoBlobPort almacenamientoBlobPort;

    @InjectMocks
    private RegistrarFundacionUseCaseImpl useCase;

    private FundacionRequestDTO requestValido;
    private static final String ADMIN_ROLE = "ROLE_ADMIN_STYLERADAR";

    @BeforeEach
    void setUp() {
        requestValido = FundacionRequestDTO.builder()
                .nit("900215884-1")
                .nombre("Fundación Abrigo Solidario")
                .descripcion("Recolección de prendas")
                .direccion("Calle 85 # 15-32")
                .barrio("Chapinero")
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
        when(repositoryPort.existsByNitIgnoreCase(requestValido.getNit())).thenReturn(false);
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(false);
        when(repositoryPort.existsByRepresentanteId(requestValido.getRepresentanteId())).thenReturn(false);
        when(repositoryPort.save(any(Fundacion.class))).thenAnswer(inv -> {
            Fundacion f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        FundacionResponseDTO response = useCase.registrar(requestValido, ADMIN_ROLE);

        verify(adminValidator).validarEsAdministradorParaFundaciones(ADMIN_ROLE);
        verify(repositoryPort).save(any(Fundacion.class));
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("900215884-1", response.getNit());
        assertEquals("Chapinero", response.getBarrio());
        assertEquals("Fundación Abrigo Solidario", response.getNombre());
        assertTrue(response.isActiva());
    }

    @Test
    @DisplayName("Criterio 3: Debe lanzar FundacionYaRegistradaException (409) cuando el NIT o el nombre ya existe")
    void debeLanzar409CuandoNitONombreYaExiste() {
        when(repositoryPort.existsByNitIgnoreCase(requestValido.getNit())).thenReturn(true);
        assertThrows(FundacionYaRegistradaException.class, () ->
                useCase.registrar(requestValido, ADMIN_ROLE));

        when(repositoryPort.existsByNitIgnoreCase(requestValido.getNit())).thenReturn(false);
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class, () ->
                useCase.registrar(requestValido, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Criterio 3b: Debe lanzar FundacionYaRegistradaException (409) cuando el representante ya está asignado")
    void debeLanzar409CuandoRepresentanteYaEstaAsignado() {
        when(repositoryPort.existsByNitIgnoreCase(requestValido.getNit())).thenReturn(false);
        when(repositoryPort.existsByNombreIgnoreCase(requestValido.getNombre())).thenReturn(false);
        when(repositoryPort.existsByRepresentanteId(requestValido.getRepresentanteId())).thenReturn(true);

        assertThrows(FundacionYaRegistradaException.class, () ->
                useCase.registrar(requestValido, ADMIN_ROLE));
    }

    @Test
    @DisplayName("Criterio 4: Debe propagar AccesoDenegadoAlmacenException (403) si no es administrador")
    void debePropagar403CuandoNoEsAdministrador() {
        org.mockito.Mockito.doThrow(new AccesoDenegadoAlmacenException("Requiere rol ADMIN"))
                .when(adminValidator).validarEsAdministradorParaFundaciones("ROLE_COMPRADOR");

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
    @DisplayName("Debe listar todas las fundaciones, obtener por ID y subir certificado PDF")
    void debeListarObtenerYSubirCertificado() {
        Fundacion f = Fundacion.builder()
                .id(1L)
                .nit("900215884-1")
                .nombre("Fundación Semillas")
                .barrio("Chapinero")
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        when(repositoryPort.findAll()).thenReturn(List.of(f));
        when(repositoryPort.findById(1L)).thenReturn(Optional.of(f));
        when(repositoryPort.findById(99L)).thenReturn(Optional.empty());

        List<FundacionResponseDTO> lista = useCase.listarTodas(ADMIN_ROLE);
        assertEquals(1, lista.size());
        assertEquals("Fundación Semillas", lista.get(0).getNombre());

        FundacionResponseDTO porId = useCase.obtenerPorId(1L, ADMIN_ROLE);
        assertEquals(1L, porId.getId());
        assertThrows(FundacionNoEncontradaException.class, () -> useCase.obtenerPorId(99L, ADMIN_ROLE));

        when(almacenamientoBlobPort.subirArchivo(eq("documentos-fundaciones"), anyString(), any(), eq("application/pdf"), eq(1024L)))
                .thenReturn("https://blob/documentos-fundaciones/certificados/c.pdf");
        DocumentoSubidoResponseDTO cert = useCase.subirCertificado(
                "certificado.pdf", "application/pdf", 1024L, new ByteArrayInputStream("%PDF".getBytes()), ADMIN_ROLE);
        assertEquals("https://blob/documentos-fundaciones/certificados/c.pdf", cert.url());
        assertThrows(ReglaDeNegocioException.class, () -> useCase.subirCertificado(
                "foto.jpg", "image/jpeg", 1024L, new ByteArrayInputStream("img".getBytes()), ADMIN_ROLE));
    }

    @Test
    @DisplayName("SR-RF-43: Debe consultar fundaciones activas ordenadas por cercanía y filtradas por tipo de prenda")
    void debeConsultarFundacionesCercanasActivas() {
        Fundacion cercana = Fundacion.builder()
                .id(1L).nombre("Cercana").activa(true).latitud(4.67).longitud(-74.05)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR)).build();
        Fundacion lejana = Fundacion.builder()
                .id(2L).nombre("Lejana").activa(true).latitud(6.24).longitud(-75.58)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR)).build();
        Fundacion inactiva = Fundacion.builder()
                .id(3L).nombre("Inactiva").activa(false).latitud(4.67).longitud(-74.05)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR)).build();

        when(repositoryPort.findAll()).thenReturn(List.of(lejana, inactiva, cercana));

        ConsultaFundacionesRequestDTO req = ConsultaFundacionesRequestDTO.builder()
                .latitud(4.6712).longitud(-74.0531).tipo(TipoPrenda.SUPERIOR).build();

        List<FundacionResponseDTO> resultado = useCase.consultarCercanas(req);

        assertEquals(2, resultado.size());
        assertEquals("Cercana", resultado.get(0).getNombre());
        assertEquals("Lejana", resultado.get(1).getNombre());
        assertThrows(ReglaDeNegocioException.class, () -> useCase.consultarCercanas(null));
    }
}
