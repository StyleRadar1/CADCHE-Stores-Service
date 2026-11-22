package edu.dosw.styleradar.tiendas.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarNuevaLlegadaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.EvaluarUltimasUnidadesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DistintivoPrendaAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.PrendaNoCumpleDistintivoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Caso de Uso - MarcarDistintivoPrendaUseCase (SR-RF-59 / SR-RF-60)")
class MarcarDistintivoPrendaUseCaseTest {

    @Mock
    private AlmacenRepositoryPort repositoryPort;

    @Mock
    private AutorizacionAlmacenValidator autorizacionValidator;

    @InjectMocks
    private MarcarDistintivoPrendaUseCaseImpl useCase;

    private static final String NIT = "900123456-1";

    @Test
    @DisplayName("SR-RF-59: Debe marcar prenda como LOW_STOCK usando el umbral real del almacén y rechazar stock superior con 422")
    void debeMarcarUltimasUnidadesSegunUmbralDelAlmacen() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(4)
                .diasNuevaLlegada(10)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        DistintivoPrendaAlmacenResponseDTO resp = useCase.marcarUltimasUnidades(NIT, 104L, 2, 10L);

        assertThat(resp.nit()).isEqualTo(NIT);
        assertThat(resp.itemId()).isEqualTo(104L);
        assertThat(resp.ultimasUnidades()).isTrue();
        assertThat(resp.umbralUltimasUnidades()).isEqualTo(4);
        assertThat(resp.diasNuevaLlegada()).isEqualTo(10);
        assertThat(resp.distintivoActivo()).isEqualTo("LOW_STOCK");
        verify(autorizacionValidator).validarAcceso(NIT, 10L, repositoryPort);

        assertThatThrownBy(() -> useCase.marcarUltimasUnidades(NIT, 104L, 8, 10L))
                .isInstanceOf(PrendaNoCumpleDistintivoException.class);
        assertThatThrownBy(() -> useCase.marcarUltimasUnidades(NIT, 0L, 2, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("SR-RF-59 B-1.2 y B-1.4: evaluarUltimasUnidades evalúa stock=umbral, stock=umbral+1, stock=0 y disponible=false")
    void debeEvaluarUltimasUnidadesConBodyJson() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(7)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        DistintivoPrendaAlmacenResponseDTO enUmbral = useCase.evaluarUltimasUnidades(
                NIT, 104L, new EvaluarUltimasUnidadesRequestDTO(3, true), 10L);
        assertThat(enUmbral.ultimasUnidades()).isTrue();
        assertThat(enUmbral.distintivoActivo()).isEqualTo("LOW_STOCK");

        DistintivoPrendaAlmacenResponseDTO sobreUmbral = useCase.evaluarUltimasUnidades(
                NIT, 104L, new EvaluarUltimasUnidadesRequestDTO(4, true), 10L);
        assertThat(sobreUmbral.ultimasUnidades()).isFalse();
        assertThat(sobreUmbral.distintivoActivo()).isEqualTo("NONE");

        DistintivoPrendaAlmacenResponseDTO agotada = useCase.evaluarUltimasUnidades(
                NIT, 104L, new EvaluarUltimasUnidadesRequestDTO(0, true), 10L);
        assertThat(agotada.ultimasUnidades()).isFalse();
        assertThat(agotada.distintivoActivo()).isEqualTo("NONE");

        DistintivoPrendaAlmacenResponseDTO noDisponible = useCase.evaluarUltimasUnidades(
                NIT, 104L, new EvaluarUltimasUnidadesRequestDTO(2, false), 10L);
        assertThat(noDisponible.ultimasUnidades()).isFalse();
        assertThat(noDisponible.distintivoActivo()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("SR-RF-60: Debe marcar prenda como NEW_ARRIVAL usando los días configurados y rechazar fecha expirada con 422")
    void debeMarcarNuevaLlegadaSegunPeriodoDelAlmacen() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(7)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        DistintivoPrendaAlmacenResponseDTO resp = useCase.marcarNuevaLlegada(
                NIT, 104L, Instant.now().minus(2, ChronoUnit.DAYS), 10L);

        assertThat(resp.nuevaLlegada()).isTrue();
        assertThat(resp.diasNuevaLlegada()).isEqualTo(7);
        assertThat(resp.distintivoActivo()).isEqualTo("NEW_ARRIVAL");

        assertThatThrownBy(() -> useCase.marcarNuevaLlegada(
                NIT, 104L, Instant.now().minus(15, ChronoUnit.DAYS), 10L))
                .isInstanceOf(PrendaNoCumpleDistintivoException.class);
        assertThatThrownBy(() -> useCase.marcarNuevaLlegada(NIT, null, null, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("SR-RF-60 B-1.2 y B-1.4: evaluarNuevaLlegada evalúa último día, día + 1, fecha futura y retirada=true")
    void debeEvaluarNuevaLlegadaConBodyJson() {
        Almacen almacen = Almacen.builder()
                .nit(NIT)
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(7)
                .build();

        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));

        Instant ahora = Instant.now();
        DistintivoPrendaAlmacenResponseDTO vigente = useCase.evaluarNuevaLlegada(
                NIT, 104L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(7, ChronoUnit.DAYS).plusSeconds(60), false), 10L);
        assertThat(vigente.nuevaLlegada()).isTrue();
        assertThat(vigente.distintivoActivo()).isEqualTo("NEW_ARRIVAL");

        DistintivoPrendaAlmacenResponseDTO expirada = useCase.evaluarNuevaLlegada(
                NIT, 104L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(8, ChronoUnit.DAYS), false), 10L);
        assertThat(expirada.nuevaLlegada()).isFalse();
        assertThat(expirada.distintivoActivo()).isEqualTo("NONE");

        DistintivoPrendaAlmacenResponseDTO retirada = useCase.evaluarNuevaLlegada(
                NIT, 104L, new EvaluarNuevaLlegadaRequestDTO(ahora.minus(1, ChronoUnit.DAYS), true), 10L);
        assertThat(retirada.nuevaLlegada()).isFalse();
        assertThat(retirada.distintivoActivo()).isEqualTo("NONE");

        assertThatThrownBy(() -> useCase.evaluarNuevaLlegada(
                NIT, 104L, new EvaluarNuevaLlegadaRequestDTO(ahora.plus(1, ChronoUnit.DAYS), false), 10L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La fecha de publicación no puede ser futura");
    }

    @Test
    @DisplayName("Debe lanzar 404 cuando el almacén no existe y 403 cuando el usuario no es propietario (BOLA)")
    void debeValidarExistenciaYAutorizacionBola() {
        Almacen almacen = Almacen.builder().nit(NIT).build();
        when(repositoryPort.findByNit("000000000")).thenReturn(Optional.empty());
        when(repositoryPort.findByNit(NIT)).thenReturn(Optional.of(almacen));
        doThrow(new AccesoDenegadoAlmacenException("Sin permisos"))
                .when(autorizacionValidator).validarAcceso(NIT, 999L, repositoryPort);

        assertThatThrownBy(() -> useCase.marcarUltimasUnidades("000000000", 104L, 1, 10L))
                .isInstanceOf(AlmacenNoEncontradoException.class);
        assertThatThrownBy(() -> useCase.marcarNuevaLlegada("000000000", 104L, null, 10L))
                .isInstanceOf(AlmacenNoEncontradoException.class);
        assertThatThrownBy(() -> useCase.marcarUltimasUnidades(NIT, 104L, 1, 999L))
                .isInstanceOf(AccesoDenegadoAlmacenException.class);
    }
}
