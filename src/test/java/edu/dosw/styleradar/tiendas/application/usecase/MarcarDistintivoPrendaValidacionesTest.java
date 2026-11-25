package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarNuevaLlegadaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarUltimasUnidadesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarcarDistintivoPrenda: identificador de prenda inválido y solicitudes sin datos (SR-RF-59 / SR-RF-60)")
class MarcarDistintivoPrendaValidacionesTest {

    private static final String NIT = "900123456-1";

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAlmacenValidator autorizacionValidator;

    @InjectMocks
    private MarcarDistintivoPrendaUseCaseImpl useCase;

    @ParameterizedTest(name = "itemId={0}")
    @NullSource
    @ValueSource(longs = {0L, -5L})
    void itemIdInvalidoSeRechazaAntesDeConsultarElAlmacen(Long itemId) {
        EvaluarUltimasUnidadesRequestDTO stock = new EvaluarUltimasUnidadesRequestDTO(2, true);
        EvaluarNuevaLlegadaRequestDTO fecha = new EvaluarNuevaLlegadaRequestDTO(Instant.now(), false);

        assertThatThrownBy(() -> useCase.marcarUltimasUnidades(NIT, itemId, 2, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.evaluarUltimasUnidades(NIT, itemId, stock, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.marcarNuevaLlegada(NIT, itemId, Instant.now(), 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.evaluarNuevaLlegada(NIT, itemId, fecha, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        verifyNoInteractions(repositoryPort);
    }

    @Test
    void sinStockSeUsaElUmbralDelAlmacen() {
        Almacen almacen = Almacen.builder().nit(NIT).umbralUltimasUnidades(3).diasNuevaLlegada(7).build();
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        DistintivoPrendaAlmacenResponseDTO sinRequest = useCase.evaluarUltimasUnidades(NIT, 7L, null, 10L);
        DistintivoPrendaAlmacenResponseDTO sinStock =
                useCase.evaluarUltimasUnidades(NIT, 7L, new EvaluarUltimasUnidadesRequestDTO(null, true), 10L);

        assertThat(sinRequest.distintivoActivo()).isEqualTo("LOW_STOCK");
        assertThat(sinStock.distintivoActivo()).isEqualTo("LOW_STOCK");
    }

    @Test
    void sinFechaSeTomaComoPublicadaAhora() {
        Almacen almacen = Almacen.builder().nit(NIT).umbralUltimasUnidades(3).diasNuevaLlegada(7).build();
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        DistintivoPrendaAlmacenResponseDTO sinRequest = useCase.evaluarNuevaLlegada(NIT, 7L, null, 10L);
        DistintivoPrendaAlmacenResponseDTO sinFecha =
                useCase.evaluarNuevaLlegada(NIT, 7L, new EvaluarNuevaLlegadaRequestDTO(null, false), 10L);
        DistintivoPrendaAlmacenResponseDTO marcada =
                useCase.marcarNuevaLlegada(NIT, 7L, Instant.now().minus(1, ChronoUnit.DAYS), 10L);

        assertThat(sinRequest.distintivoActivo()).isEqualTo("NEW_ARRIVAL");
        assertThat(sinFecha.distintivoActivo()).isEqualTo("NEW_ARRIVAL");
        assertThat(marcada.distintivoActivo()).isEqualTo("NEW_ARRIVAL");
    }

    @Test
    void stockCeroNoMarcaUltimasUnidades() {
        Almacen almacen = Almacen.builder().nit(NIT).umbralUltimasUnidades(3).build();
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        assertThat(useCase.marcarUltimasUnidades(NIT, 7L, 0, 10L).distintivoActivo()).isEqualTo("NONE");
    }
}
