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

import edu.dosw.styleradar.tiendas.application.dto.request.ConfiguracionCatalogoAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.PeriodoNuevaLlegadaValidator;
import edu.dosw.styleradar.tiendas.domain.validator.UmbralInventarioValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Caso de Uso: Configurar Umbral Últimas Unidades - SR-RF-59")
class ConfigurarUmbralUltimasUnidadesUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private UmbralInventarioValidator umbralValidator;

    @Mock
    private PeriodoNuevaLlegadaValidator periodoValidator;

    @Mock
    private AutorizacionAlmacenValidator autorizacionValidator;

    @InjectMocks
    private ConfigurarUmbralUltimasUnidadesUseCaseImpl useCase;

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
                .diasNuevaLlegada(15)
                .build();
    }

    @Test
    @DisplayName("Criterio: Dado un umbral válido, cuando lo guardo, entonces queda configurado para mi almacén")
    void debeConfigurarUmbralValidoExitosamente() {
        int nuevoUmbral = 5;
        doNothing().when(umbralValidator).validar(nuevoUmbral);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenExistente));
        doNothing().when(autorizacionValidator).validarAcceso(NIT, USUARIO_ID, repositoryPort);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.configurarUmbral(NIT, nuevoUmbral, USUARIO_ID);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(nuevoUmbral);
        verify(repositoryPort).save(any(Almacen.class));
    }

    @Test
    @DisplayName("Criterio: Dado un umbral negativo, cuando lo guardo, entonces recibo 400 (UmbralInvalidoException)")
    void debeRechazarUmbralNegativo() {
        int umbralNegativo = -2;
        doThrow(new UmbralInvalidoException("El umbral de últimas unidades no puede ser negativo"))
                .when(umbralValidator).validar(umbralNegativo);

        assertThatThrownBy(() -> useCase.configurarUmbral(NIT, umbralNegativo, USUARIO_ID))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede ser negativo");

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Criterio: Dado un almacén que no es mío, cuando cambio su umbral, entonces recibo 403 (AccesoDenegadoAlmacenException)")
    void debeRechazarAccesoNoAutorizadoAlmacenAjeno() {
        int nuevoUmbral = 4;
        Long intrusoId = 999L;
        doNothing().when(umbralValidator).validar(nuevoUmbral);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenExistente));
        doThrow(new AccesoDenegadoAlmacenException("El usuario autenticado no tiene permisos para este almacén"))
                .when(autorizacionValidator).validarAcceso(NIT, intrusoId, repositoryPort);

        assertThatThrownBy(() -> useCase.configurarUmbral(NIT, nuevoUmbral, intrusoId))
                .isInstanceOf(AccesoDenegadoAlmacenException.class)
                .hasMessageContaining("no tiene permisos");

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar AlmacenNoEncontradoException cuando el almacén consultado no existe")
    void debeFallarSiAlmacenNoExiste() {
        int nuevoUmbral = 4;
        String nitInexistente = "000000000";
        doNothing().when(umbralValidator).validar(nuevoUmbral);
        when(repositoryPort.findByNit(nitInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.configurarUmbral(nitInexistente, nuevoUmbral, USUARIO_ID))
                .isInstanceOf(AlmacenNoEncontradoException.class)
                .hasMessageContaining("No se encontró ningún almacén registrado");

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe permitir configurar catálogo completo actualizando umbral y días")
    void debeConfigurarCatalogoCompletoExitosamente() {
        ConfiguracionCatalogoAlmacenRequestDTO request = new ConfiguracionCatalogoAlmacenRequestDTO(7, 20);
        doNothing().when(umbralValidator).validar(7);
        doNothing().when(periodoValidator).validar(20);
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenExistente));
        doNothing().when(autorizacionValidator).validarAcceso(NIT, USUARIO_ID, repositoryPort);
        when(repositoryPort.save(any(Almacen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.configurarCatalogo(NIT, request, USUARIO_ID);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(7);
        assertThat(response.diasNuevaLlegada()).isEqualTo(20);
        verify(repositoryPort).save(any(Almacen.class));
    }
}
