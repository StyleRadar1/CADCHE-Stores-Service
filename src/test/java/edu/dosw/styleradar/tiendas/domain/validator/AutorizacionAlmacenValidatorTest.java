package edu.dosw.styleradar.tiendas.domain.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de AutorizacionAlmacenValidator - Control de Acceso BOLA/IDOR y Rol Tienda")
class AutorizacionAlmacenValidatorTest {

    private AutorizacionAlmacenValidator validator;

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    private static final String NIT = "900123456";
    private static final Long USUARIO_ADMIN = 10L;
    private static final Long USUARIO_INTRUSO = 99L;

    @BeforeEach
    void setUp() {
        validator = new AutorizacionAlmacenValidator();
    }

    @Test
    @DisplayName("Debe permitir acceso cuando el usuario es administrador del almacén")
    void debePermitirAccesoCuandoUsuarioEsAdmin() {
        when(repositoryPort.esAdministradorDeAlmacen(NIT, USUARIO_ADMIN)).thenReturn(true);

        assertThatCode(() -> validator.validarAcceso(NIT, USUARIO_ADMIN, repositoryPort))
                .doesNotThrowAnyException();

        verify(repositoryPort).esAdministradorDeAlmacen(NIT, USUARIO_ADMIN);
    }

    @Test
    @DisplayName("Debe lanzar AccesoDenegadoAlmacenException cuando el usuario NO es administrador del almacén (403 BOLA)")
    void debeLanzarExcepcionCuandoUsuarioNoEsAdmin() {
        when(repositoryPort.esAdministradorDeAlmacen(NIT, USUARIO_INTRUSO)).thenReturn(false);

        assertThatThrownBy(() -> validator.validarAcceso(NIT, USUARIO_INTRUSO, repositoryPort))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessageContaining("no tiene permisos para administrar el almacén con NIT " + NIT);

        verify(repositoryPort).esAdministradorDeAlmacen(NIT, USUARIO_INTRUSO);
    }

    @Test
    @DisplayName("B-0.1: Debe lanzar FalloAutenticacionException (401) cuando el usuarioId es nulo")
    void debeLanzar401CuandoUsuarioIdEsNulo() {
        assertThatThrownBy(() -> validator.validarAcceso(NIT, null, repositoryPort))
                .isInstanceOf(FalloAutenticacionException.class);

        verify(repositoryPort, never()).esAdministradorDeAlmacen(NIT, null);
    }

    @Test
    @DisplayName("SR-RF-62-A FA-02: Debe validar que el usuario tenga rol de tienda o administrador de almacén")
    void debeValidarRolTienda() {
        assertThatCode(() -> validator.validarRolTienda("ROLE_TIENDA")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarRolTienda("ROLE_ADMIN_ALMACEN")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarRolTienda("TIENDA")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validarRolTienda("ADMIN_ALMACEN")).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validarRolTienda("ROLE_COMPRADOR"))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessage("Solo una cuenta con rol de tienda puede registrar un almacén");
        assertThatThrownBy(() -> validator.validarRolTienda(null))
                .isInstanceOf(FalloAutenticacionException.class);
        assertThatThrownBy(() -> validator.validarRolTienda("   "))
                .isInstanceOf(FalloAutenticacionException.class);
    }
}
