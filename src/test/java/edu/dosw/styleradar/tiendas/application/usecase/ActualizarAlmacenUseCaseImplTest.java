package edu.dosw.styleradar.tiendas.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActualizarAlmacenUseCaseImpl - Edición de perfil comercial del almacén")
class ActualizarAlmacenUseCaseImplTest {

    private static final String NIT = "900123456-1";

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    private ActualizarAlmacenUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ActualizarAlmacenUseCaseImpl(repositoryPort, new AutorizacionAlmacenValidator());
    }

    private Almacen almacenBase() {
        return Almacen.builder()
                .nit(NIT)
                .nombreComercial("Atelier Original")
                .descripcion("Descripción inicial")
                .telefono("+57 601 000 0000")
                .correoContacto("inicial@atelier.co")
                .latitud(4.6533)
                .longitud(-74.0602)
                .reputacion(4.5)
                .categorias(new HashSet<>(Set.of(CategoriaAlmacen.CASUAL)))
                .estadoVerificacion(EstadoVerificacionAlmacen.APPROVED)
                .urlRutPdf("https://styleradarblob.blob.core.windows.net/documentos-almacenes/rut.pdf")
                .build();
    }

    @Test
    @DisplayName("Actualiza exitosamente el perfil comercial y persiste los 5 nuevos campos cuando el administrador está autorizado")
    void debeActualizarPerfilComercialYPersistirNuevosCampos() {
        Almacen existente = almacenBase();
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(existente));
        when(repositoryPort.esAdministradorDeAlmacen(NIT, 10L)).thenReturn(true);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlmacenRequestDTO request = ActualizarAlmacenRequestDTO.builder()
                .nombreComercial("Atelier Urbano Chapinero")
                .descripcion("Moda independiente actualizada")
                .telefono("+57 310 555 8899")
                .correoContacto("NUEVO@Atelier.co")
                .representanteLegal("Camila Gómez")
                .cuentaPagos("Bancolombia Ahorros 012-345678-90")
                .direccionComercial("Carrera 13 # 63-25")
                .barrio("Chapinero")
                .horarioAtencion("Lun-Sáb 10:00 - 20:00")
                .categorias(Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE))
                .build();

        VerificacionAlmacenResponseDTO response = useCase.actualizarAlmacen(NIT, request, 10L, "ROLE_STORE_ADMIN");

        ArgumentCaptor<Almacen> captor = ArgumentCaptor.forClass(Almacen.class);
        verify(repositoryPort).save(captor.capture());
        Almacen guardado = captor.getValue();

        assertEquals(NIT, guardado.getNit());
        assertEquals("Atelier Urbano Chapinero", guardado.getNombreComercial());
        assertEquals("nuevo@atelier.co", guardado.getCorreoContacto());
        assertEquals("Camila Gómez", guardado.getRepresentanteLegal());
        assertEquals("Bancolombia Ahorros 012-345678-90", guardado.getCuentaPagos());
        assertEquals("Carrera 13 # 63-25", guardado.getDireccionComercial());
        assertEquals("Chapinero", guardado.getBarrio());
        assertEquals("Lun-Sáb 10:00 - 20:00", guardado.getHorarioAtencion());
        assertEquals(Set.of(CategoriaAlmacen.CASUAL, CategoriaAlmacen.VINTAGE), guardado.getCategorias());

        UUID expectedId = UUID.nameUUIDFromBytes(("ms-tiendas:almacen:" + NIT).getBytes(StandardCharsets.UTF_8));
        assertNotNull(response);
        assertEquals(expectedId, response.almacenId());
        assertEquals("Camila Gómez", response.representante());
        assertEquals("Bancolombia Ahorros 012-345678-90", response.cuentaPagos());
        assertEquals("Carrera 13 # 63-25", response.direccionComercial());
        assertEquals("Chapinero", response.barrio());
        assertEquals("Lun-Sáb 10:00 - 20:00", response.horarioAtencion());
    }

    @Test
    @DisplayName("Permite a STYLERADAR_ADMIN editar cualquier almacén sin validar asociación de administrador")
    void debePermitirEdicionPorStyleRadarAdmin() {
        Almacen existente = almacenBase();
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(existente));
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAlmacenRequestDTO request = ActualizarAlmacenRequestDTO.builder()
                .barrio("Usaquén")
                .horarioAtencion("Mar-Dom 11:00 - 19:00")
                .build();

        VerificacionAlmacenResponseDTO response = useCase.actualizarAlmacen(NIT, request, 1L, "ROLE_STYLERADAR_ADMIN");

        verify(repositoryPort, never()).esAdministradorDeAlmacen(any(), any());
        assertEquals("Atelier Original", response.nombreComercial());
        assertEquals("Usaquén", response.barrio());
        assertEquals("Mar-Dom 11:00 - 19:00", response.horarioAtencion());
    }

    @Test
    @DisplayName("Rechaza con 403 (BOLA/IDOR) cuando el usuario autenticado no administra el almacén")
    void debeRechazarBolaSiUsuarioNoAdministraAlmacen() {
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenBase()));
        when(repositoryPort.esAdministradorDeAlmacen(NIT, 99L)).thenReturn(false);

        ActualizarAlmacenRequestDTO request = ActualizarAlmacenRequestDTO.builder()
                .nombreComercial("Nombre Intruso")
                .build();

        assertThrows(AccesoDenegadoAlmacenException.class,
                () -> useCase.actualizarAlmacen(NIT, request, 99L, "ROLE_STORE_ADMIN"));
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Lanza 404 cuando el almacén no existe")
    void debeLanzar404CuandoAlmacenNoExiste() {
        when(repositoryPort.findByNit("999999999-0")).thenReturn(Optional.empty());

        ActualizarAlmacenRequestDTO request = ActualizarAlmacenRequestDTO.builder()
                .nombreComercial("Inexistente")
                .build();

        assertThrows(AlmacenNoEncontradoException.class,
                () -> useCase.actualizarAlmacen("999999999-0", request, 10L, "ROLE_STORE_ADMIN"));
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Lanza 401 cuando falta identidad de usuario")
    void debeLanzar401CuandoNoHayUsuarioAutenticado() {
        ActualizarAlmacenRequestDTO request = ActualizarAlmacenRequestDTO.builder()
                .nombreComercial("Sin Auth")
                .build();

        assertThrows(FalloAutenticacionException.class,
                () -> useCase.actualizarAlmacen(NIT, request, null, "ROLE_STORE_ADMIN"));
        verify(repositoryPort, never()).findByNit(any());
    }
}
