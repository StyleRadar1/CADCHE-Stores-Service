package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - RegistrarAlmacenUseCase (SR-RF-62)")
class RegistrarAlmacenUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

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

        when(repositoryPort.existsByNit(NIT)).thenReturn(false);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionAlmacenResponseDTO resp = useCase.registrarAlmacen(req, 15L);

        assertThat(resp.nit()).isEqualTo(NIT);
        assertThat(resp.estadoVerificacion()).isEqualTo(EstadoVerificacionAlmacen.PENDIENTE);
        assertThat(resp.correoContacto()).isEqualTo("contacto@atelier.co");
        verify(repositoryPort).asociarAdministrador(NIT, 15L);
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
                .isInstanceOf(AlmacenYaRegistradoException.class);
        assertThatThrownBy(() -> useCase.registrarAlmacen(null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
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

