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
import edu.dosw.styleradar.tiendas.domain.model.PeriodoNuevaLlegada;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias del Caso de Uso: Consultar Periodo Nueva Llegada - SR-RF-60")
class ConsultarPeriodoNuevaLlegadaUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @InjectMocks
    private ConsultarPeriodoNuevaLlegadaUseCaseImpl useCase;

    private static final String NIT = "900123456";

    @Test
    @DisplayName("Criterio: Dado un almacén sin periodo propio, cuando se consulta, entonces aplica el valor por defecto de 7 días")
    void debeRetornarValorPorDefectoDe7DiasCuandoNoTienePeriodoPropio() {
        Almacen almacenSinPeriodo = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Norte")
                .umbralUltimasUnidades(null)
                .diasNuevaLlegada(null)
                .fechaActualizacionCatalogo(null)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenSinPeriodo));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.consultarPeriodo(NIT);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.diasNuevaLlegada()).isEqualTo(PeriodoNuevaLlegada.VALOR_POR_DEFECTO);
        assertThat(response.diasNuevaLlegada()).isEqualTo(7);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe retornar el periodo configurado cuando el almacén tiene un valor propio establecido")
    void debeRetornarPeriodoConfiguradoPropio() {
        Instant fechaAct = Instant.parse("2026-11-08T18:00:00Z");
        Almacen almacenConPeriodo = Almacen.builder()
                .nit(NIT)
                .nombreComercial("Tienda Trendy")
                .umbralUltimasUnidades(5)
                .diasNuevaLlegada(20)
                .fechaActualizacionCatalogo(fechaAct)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacenConPeriodo));

        ConfiguracionCatalogoAlmacenResponseDTO response = useCase.consultarPeriodo(NIT);

        assertThat(response).isNotNull();
        assertThat(response.nit()).isEqualTo(NIT);
        assertThat(response.diasNuevaLlegada()).isEqualTo(20);
        assertThat(response.umbralUltimasUnidades()).isEqualTo(5);
        assertThat(response.fechaActualizacion()).isEqualTo(fechaAct);
    }

    @Test
    @DisplayName("Debe lanzar AlmacenNoEncontradoException cuando el almacén consultado no existe")
    void debeLanzarExcepcionCuandoAlmacenNoExiste() {
        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.consultarPeriodo("000000000"))
                .isInstanceOf(AlmacenNoEncontradoException.class)
                .hasMessageContaining("No se encontró ningún almacén registrado con el NIT: 000000000");
    }
}
