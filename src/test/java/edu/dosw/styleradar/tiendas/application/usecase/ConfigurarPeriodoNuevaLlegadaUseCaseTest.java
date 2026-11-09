package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.PeriodoNuevaLlegadaValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Caso de Uso: Configurar Periodo Nueva Llegada - SR-RF-60")
class ConfigurarPeriodoNuevaLlegadaUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private PeriodoNuevaLlegadaValidator periodoValidator;

    @Mock
    private AutorizacionAlmacenValidator autorizacionValidator;

    @InjectMocks
    private ConfigurarPeriodoNuevaLlegadaUseCaseImpl useCase;

    private static final String NIT = "900123456";
    private static final Long USUARIO_ID = 5L;

    private Almacen almacenExistente;

    @BeforeEach
    void setUp() {
        almacenExistente = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Centro")
                .telefono("3001234567")
                .correoContacto("tienda@styleradar.com")
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(7)
                .build();
    }

    @Test
    @DisplayName("Criterio: Dado un periodo válido, cuando lo guardo, entonces queda configurado para mi almacén")
    void debeConfigurarPeriodoValidoExitosamente() {
        int nuevoPeriodo = 14;
        doNothing().when(periodoValidator).validar(nuevoPeriodo);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenExistente));
        doNothing().when(autorizacionValidator).validarAcceso(NIT, USUARIO_ID, repositoryPort);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.configurarPeriodo(NIT, nuevoPeriodo, USUARIO_ID);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.diasNuevaLlegada()).isEqualTo(nuevoPeriodo);
        verify(repositoryPort).save(any(Almacen.class));
    }

    @Test
    @DisplayName("Criterio: Dado un periodo de 0 días o menos, cuando lo guardo, entonces recibo 400 (PeriodoInvalidoException)")
    void debeRechazarPeriodoInvalidoCeroONegativo() {
        int periodoInvalido = 0;
        doThrow(new PeriodoInvalidoException("El periodo de nueva llegada debe ser de al menos 1 día"))
                .when(periodoValidator).validar(periodoInvalido);

        assertThatThrownBy(() -> useCase.configurarPeriodo(NIT, periodoInvalido, USUARIO_ID))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("debe ser de al menos 1 día");

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Criterio: Dado un almacén que no es mío, cuando cambio su periodo, entonces recibo 403 (AccesoDenegadoAlmacenException)")
    void debeRechazarAccesoNoAutorizadoAlmacenAjeno() {
        int nuevoPeriodo = 10;
        Long intrusoId = 999L;
        doNothing().when(periodoValidator).validar(nuevoPeriodo);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenExistente));
        doThrow(new AccesoDenegadoAlmacenException("El usuario autenticado no tiene permisos para este almacén"))
                .when(autorizacionValidator).validarAcceso(NIT, intrusoId, repositoryPort);

        assertThatThrownBy(() -> useCase.configurarPeriodo(NIT, nuevoPeriodo, intrusoId))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessageContaining("no tiene permisos");

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar AlmacenNoEncontradoException cuando el almacén consultado no existe")
    void debeFallarSiAlmacenNoExiste() {
        int nuevoPeriodo = 10;
        String nitInexistente = "000000000";
        doNothing().when(periodoValidator).validar(nuevoPeriodo);
        when(repositoryPort.findByNit(nitInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.configurarPeriodo(nitInexistente, nuevoPeriodo, USUARIO_ID))
                .isInstanceOf(AlmacenNoEncontradoException.class)
                .hasMessageContaining("No se encontró ningún almacén registrado");

        verify(repositoryPort, never()).save(any());
    }
}
