package edu.dosw.styleradar.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;

@DisplayName("Pruebas de Dominio - Entidad Almacen, Umbral Efectivo (SR-RF-59) y Periodo Nueva Llegada (SR-RF-60)")
class AlmacenTest {

    @Test
    @DisplayName("Dado un almacén sin umbral propio (null), cuando se consulta el umbral efectivo, entonces aplica 3")
    void debeRetornarValorPorDefectoDe3CuandoNoTieneUmbralPropio() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .umbralUltimasUnidades(null)
                .build();

        assertThat(almacen.getUmbralUltimasUnidadesEfectivo()).isEqualTo(3);
    }

    @Test
    @DisplayName("Dado un almacén sin periodo propio (null), cuando se consulta dias_nueva_llegada efectivo, aplica 7")
    void debeRetornarValorPorDefectoDe7CuandoNoTienePeriodoPropio() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .diasNuevaLlegada(null)
                .build();

        assertThat(almacen.getDiasNuevaLlegadaEfectivo()).isEqualTo(7);
    }

    @Test
    @DisplayName("Dado un periodo válido, cuando se configura en el almacén, entonces queda registrado con timestamp")
    void debeConfigurarPeriodoValido() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        almacen.configurarDiasNuevaLlegada(14);

        assertThat(almacen.getDiasNuevaLlegada()).isEqualTo(14);
        assertThat(almacen.getDiasNuevaLlegadaEfectivo()).isEqualTo(14);
        assertThat(almacen.getFechaActualizacionCatalogo()).isNotNull();
    }

    @Test
    @DisplayName("Dado un periodo de 0 días o menos, cuando se intenta configurar en el almacén, lanza PeriodoInvalidoException")
    void debeRechazarPeriodoInvalidoEnAlmacen() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        assertThatThrownBy(() -> almacen.configurarDiasNuevaLlegada(0))
                .isInstanceOf(PeriodoInvalidoException.class)
                .hasMessageContaining("debe ser de al menos 1 día");
    }

    @Test
    @DisplayName("Dado un umbral válido, cuando se configura en el almacén, entonces queda registrado con timestamp")
    void debeConfigurarUmbralValido() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        almacen.configurarUmbralUltimasUnidades(5);

        assertThat(almacen.getUmbralUltimasUnidades()).isEqualTo(5);
        assertThat(almacen.getUmbralUltimasUnidadesEfectivo()).isEqualTo(5);
        assertThat(almacen.getFechaActualizacionCatalogo()).isNotNull();
    }

    @Test
    @DisplayName("Dado un umbral negativo, cuando se intenta configurar en el almacén, lanza UmbralInvalidoException")
    void debeRechazarUmbralNegativo() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        assertThatThrownBy(() -> almacen.configurarUmbralUltimasUnidades(-2))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @Test
    @DisplayName("Debe configurar simultáneamente catálogo con umbral y días válidos")
    void debeConfigurarCatalogoSimultaneamente() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        almacen.configurarCatalogo(4, 21);

        assertThat(almacen.getUmbralUltimasUnidades()).isEqualTo(4);
        assertThat(almacen.getDiasNuevaLlegada()).isEqualTo(21);
        assertThat(almacen.getFechaActualizacionCatalogo()).isNotNull();
    }
}

