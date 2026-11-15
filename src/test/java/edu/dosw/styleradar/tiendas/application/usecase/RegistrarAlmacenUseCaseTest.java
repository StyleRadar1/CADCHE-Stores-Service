package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - RegistrarAlmacenUseCase (SR-RF-62-A / SR-RF-62)")
class RegistrarAlmacenUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AlmacenamientoBlobPort almacenamientoBlobPort;

    @InjectMocks
    private RegistrarAlmacenUseCaseImpl useCase;

    private static final String NIT = "900123456-1";

    @Test
    @DisplayName("Debe registrar almacén en estado PENDIENTE y asociar administrador cuando se envía usuarioId")
    void debeRegistrarAlmacenExitosamente() {
        RegistrarAlmacenRequestDTO req = RegistrarAlmacenRequestDTO.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano")
                .descripcion("Moda independiente")
                .telefono("+57 601 312 4455")
                .correoContacto("CONTACTO@ATELIER.CO")
                .latitud(4.6533)
                .longitud(-74.0602)
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        when(repositoryPort.existeAlmacenParaUsuario(15L)).thenReturn(false);
        when(repositoryPort.existsByNit(NIT)).thenReturn(false);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlmacenResponseDTO resp = useCase.registrarAlmacen(req, 15L, "ROLE_TIENDA");

        assertThat(resp.nit()).isEqualTo(NIT);
        assertThat(resp.estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
        assertThat(resp.correoContacto()).isEqualTo("contacto@atelier.co");
        assertThat(resp.representante()).isEqualTo("Usuario #15");
        verify(repositoryPort).asociarAdministrador(NIT, 15L);
    }

    @Test
    @DisplayName("FA-02 y FA-04: Debe fallar con 403 si no tiene rol tienda y 409 si el usuario ya administra un almacén")
    void debeFallarPorRolOAlmacenPrevioDelUsuario() {
        RegistrarAlmacenRequestDTO req = RegistrarAlmacenRequestDTO.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano")
                .correoContacto("contacto@atelier.co")
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        assertThatThrownBy(() -> useCase.registrarAlmacen(req, 15L, "ROLE_COMPRADOR"))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessage("Solo una cuenta con rol de tienda puede registrar un almacén");

        when(repositoryPort.existeAlmacenParaUsuario(15L)).thenReturn(true);
        assertThatThrownBy(() -> useCase.registrarAlmacen(req, 15L, "ROLE_TIENDA"))
                .isInstanceOf(AlmacenYaRegistradoException.class)
                .hasMessage("El usuario ya administra un almacén");
    }

    @Test
    @DisplayName("Debe lanzar AlmacenYaRegistradoException (409) cuando el NIT ya existe y ReglaDeNegocioException cuando request es nulo")
    void debeFallarConNitDuplicadoORequestNulo() {
        RegistrarAlmacenRequestDTO req = RegistrarAlmacenRequestDTO.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano")
                .correoContacto("contacto@atelier.co")
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        when(repositoryPort.existsByNit(NIT)).thenReturn(true);

        assertThatThrownBy(() -> useCase.registrarAlmacen(req, null))
                .isInstanceOf(AlmacenYaRegistradoException.class)
                .hasMessage("Ya existe un almacén registrado con el NIT: " + NIT);
        assertThatThrownBy(() -> useCase.registrarAlmacen(null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("B-1.1 y B-3A.2: Debe obtener mi almacén y permitir subir documento RUT PDF válido")
    void debeObtenerMiAlmacenYSubirRutPdf() {
        RegistrarAlmacenUseCaseImpl ucConBlob = new RegistrarAlmacenUseCaseImpl(
                repositoryPort, almacenamientoBlobPort, new AutorizacionAlmacenValidator());

        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano")
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .build();
        when(repositoryPort.findByAdministradorUsuarioId(15L)).thenReturn(Optional.of(almacen));

        VerificacionAlmacenResponseDTO miAlmacen = ucConBlob.obtenerMiAlmacen(15L);
        assertThat(miAlmacen.nit()).isEqualTo(NIT);
        assertThatThrownBy(() -> ucConBlob.obtenerMiAlmacen(null))
                .isInstanceOf(FalloAutenticacionException.class);

        when(almacenamientoBlobPort.subirArchivo(eq("documentos-almacenes"), any(), any(), eq("application/pdf"), eq(12L)))
                .thenReturn("https://blob/rut-subido.pdf");

        DocumentoSubidoResponseDTO doc = ucConBlob.subirDocumentoRut(
                "rut.pdf", "application/pdf", 12L, new ByteArrayInputStream("%PDF-1.4".getBytes()), 15L, "ROLE_TIENDA");
        assertThat(doc.url()).isEqualTo("https://blob/rut-subido.pdf");

        assertThatThrownBy(() -> ucConBlob.subirDocumentoRut(
                "foto.jpg", "image/jpeg", 12L, new ByteArrayInputStream("img".getBytes()), 15L, "ROLE_TIENDA"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El RUT debe ser un archivo PDF de máximo 5 MB");

        assertThatThrownBy(() -> ucConBlob.subirDocumentoRut(
                "rut.pdf", "application/pdf", 6L * 1024L * 1024L, new ByteArrayInputStream("big".getBytes()), 15L, "ROLE_TIENDA"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El RUT debe ser un archivo PDF de máximo 5 MB");
    }

    @Test
    @DisplayName("Debe obtener almacén por NIT o lanzar AlmacenNoEncontradoException")
    void debeObtenerPorNitYFallarSiNoExiste() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier Urbano")
                .reputacion(4.9)
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));
        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());

        AlmacenResponseDTO dto = useCase.obtenerPorNit(NIT);
        assertThat(dto.nit()).isEqualTo(NIT);
        assertThat(dto.reputacion()).isEqualTo(4.9);

        assertThatThrownBy(() -> useCase.obtenerPorNit("000000000"))
                .isInstanceOf(AlmacenNoEncontradoException.class);
    }

    @Test
    @DisplayName("Debe consultar almacenes aprobados de forma paginada con filtro opcional por categoría")
    void debeConsultarAlmacenesAprobados() {
        Almacen a1 = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier")
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .build();
        Almacen a2 = Almacen.builder()
                .nit("900999888-2")
                .nombreComercial("Deportes")
                .categorias(Set.of(CategoriaAlmacen.DEPORTIVA))
                .estadoVerificacion(EstadoVerificacionAlmacen.APROBADO)
                .build();

        when(repositoryPort.findByEstadoVerificacion(eq(EstadoVerificacionAlmacen.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2)));

        ConsultaAlmacenRequestDTO req = new ConsultaAlmacenRequestDTO();
        req.setCategoria(CategoriaAlmacen.CASUAL);
        req.setPage(0);
        req.setSize(10);

        PageResponseDTO<AlmacenResponseDTO> page = useCase.consultarAlmacenes(req);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).nit()).isEqualTo(NIT);

        ConsultaAlmacenRequestDTO reqInvalido = new ConsultaAlmacenRequestDTO();
        reqInvalido.setPage(-1);
        assertThatThrownBy(() -> useCase.consultarAlmacenes(reqInvalido))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
