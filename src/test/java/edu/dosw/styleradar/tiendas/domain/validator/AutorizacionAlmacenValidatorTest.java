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
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de AutorizacionAlmacenValidator - Control de Acceso BOLA/IDOR")
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
    @DisplayName("Debe omitir la validación si el usuarioId es nulo (llamada interna sin contexto de usuario)")
    void debeOmitirValidacionCuandoUsuarioIdEsNulo() {
        assertThatCode(() -> validator.validarAcceso(NIT, null, repositoryPort))
                .doesNotThrowAnyException();

        verify(repositoryPort, never()).esAdministradorDeAlmacen(NIT, null);
    }
}
