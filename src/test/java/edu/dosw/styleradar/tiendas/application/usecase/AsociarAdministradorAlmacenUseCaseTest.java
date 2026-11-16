package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - AsociarAdministradorAlmacenUseCase (SR-RF-62 / BOLA)")
class AsociarAdministradorAlmacenUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAdminStyleRadarValidator adminValidator;

    @Mock
    private AutorizacionAlmacenValidator almacenValidator;

    @InjectMocks
    private AsociarAdministradorAlmacenUseCaseImpl useCase;

    private static final String NIT = "900123456-1";

    @Test
    @DisplayName("Debe asociar administrador de forma idempotente y listar administradores vinculados al NIT")
    void debeAsociarYListarAdministradores() {
        AdministradorAlmacen vinculo = AdministradorAlmacen.builder()
                .id(10L)
                .usuarioId(15L)
                .almacenNit(NIT)
                .asignadoPorId(1L)
                .fechaAsignacion(Instant.now())
                .build();

        when(repositoryPort.existsByNit(NIT)).thenReturn(true);
        when(repositoryPort.guardarAsociacionAdministrador(NIT, 15L)).thenReturn(vinculo);
        when(repositoryPort.listarAdministradoresPorNit(NIT)).thenReturn(List.of(vinculo));

        AdministradorAlmacenResponseDTO creado = useCase.asociar(NIT, 15L, 2L, "ROLE_ADMIN_STYLERADAR");
        assertThat(creado.id()).isEqualTo(10L);
        assertThat(creado.usuarioId()).isEqualTo(15L);
        assertThat(creado.asignadoPorId()).isEqualTo(2L);
        verify(adminValidator).validarEsAdministradorStyleRadar("ROLE_ADMIN_STYLERADAR");

        List<AdministradorAlmacenResponseDTO> lista = useCase.listarAdministradores(NIT, 15L, null);
        assertThat(lista).hasSize(1);
        verify(almacenValidator).validarAcceso(NIT, 15L, repositoryPort);
    }

    @Test
    @DisplayName("Debe fallar cuando el usuarioId es inválido o el almacén no existe")
    void debeValidarUsuarioYExistenciaDeAlmacen() {
        when(repositoryPort.existsByNit("000000000")).thenReturn(false);

        assertThatThrownBy(() -> useCase.asociar(NIT, 0L, null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.asociar("000000000", 15L, null, null))
                .isInstanceOf(AlmacenNoEncontradoException.class);
        assertThatThrownBy(() -> useCase.listarAdministradores("000000000", 15L, "ROLE_ADMIN_STYLERADAR"))
                .isInstanceOf(AlmacenNoEncontradoException.class);
    }
}

