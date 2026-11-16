package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.response.ConfiguracionCatalogoAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.UmbralUltimasUnidades;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Caso de Uso: Consultar Umbral Últimas Unidades - SR-RF-59")
class ConsultarUmbralUltimasUnidadesUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @InjectMocks
    private ConsultarUmbralUltimasUnidadesUseCaseImpl useCase;

    private static final String NIT = "900123456";

    @Test
    @DisplayName("Criterio: Dado un almacén sin umbral propio, cuando se consulta, entonces aplica el valor por defecto de 3")
    void debeRetornarValorPorDefectoDe3CuandoNoTieneUmbralPropio() {
        Almacen almacenSinUmbral = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Norte")
                .umbralUltimasUnidades(null)
                .diasNuevaLlegada(null)
                .fechaActualizacionCatalogo(null)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenSinUmbral));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.consultarConfiguracion(NIT);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(UmbralUltimasUnidades.VALOR_POR_DEFECTO);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(3);
        assertThat(response.diasNuevaLlegada()).isEqualTo(7);
        assertThat(response.fechaActualizacion()).isNotNull();
    }

    @Test
    @DisplayName("Debe retornar el umbral configurado cuando el almacén tiene un valor propio establecido")
    void debeRetornarUmbralConfiguradoPropio() {
        Instant fechaAct = Instant.parse("2026-11-08T15:30:00Z");
        Almacen almacenConUmbral = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Premium")
                .umbralUltimasUnidades(8)
                .diasNuevaLlegada(21)
                .fechaActualizacionCatalogo(fechaAct)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenConUmbral));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.consultarConfiguracion(NIT);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(8);
        assertThat(response.diasNuevaLlegada()).isEqualTo(21);
        assertThat(response.fechaActualizacion()).isEqualTo(fechaAct);
    }

    @Test
    @DisplayName("Debe lanzar AlmacenNoEncontradoException cuando el almacén consultado no existe")
    void debeLanzarExcepcionCuandoAlmacenNoExiste() {
        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.consultarConfiguracion("000000000"))
                .isInstanceOf(AlmacenNoEncontradoException.class)
                .hasMessageContaining("No se encontró ningún almacén registrado con el NIT: 000000000");
    }
}
