package edu.dosw.styleradar.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
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

    @Test
    @DisplayName("SR-RF-74 Dominio C1: Dado hasta 6 fotos válidas, quedan en el almacén y la primera se usa como portada")
    void debeCargarHasta6FotosYAsignarPrimeraComoPortada() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        FotografiaTienda foto1 = almacen.agregarFotografia("https://blob/foto1.jpg", null);
        assertThat(foto1.isPortada()).isTrue();
        assertThat(foto1.getOrden()).isEqualTo(1);
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto1.jpg");

        for (int i = 2; i <= 6; i++) {
            FotografiaTienda fotoExtra = almacen.agregarFotografia("https://blob/foto" + i + ".jpg", null);
            assertThat(fotoExtra.isPortada()).isFalse();
            assertThat(fotoExtra.getOrden()).isEqualTo(i);
        }

        assertThat(almacen.getFotos()).hasSize(6);
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto1.jpg");
    }

    @Test
    @DisplayName("SR-RF-74 Dominio C2: Dado que ya tengo 6 fotos, cuando agrego otra, lanza LimiteFotografiasSuperadoException")
    void debeLanzarLimiteFotografiasSuperadoExceptionAlSuperar6Fotos() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        for (int i = 1; i <= 6; i++) {
            almacen.agregarFotografia("https://blob/foto" + i + ".jpg", null);
        }

        assertThatThrownBy(() -> almacen.agregarFotografia("https://blob/foto7.jpg", null))
                .isInstanceOf(LimiteFotografiasSuperadoException.class)
                .hasMessageContaining("máximo permitido de 6 fotografías");
    }

    @Test
    @DisplayName("SR-RF-74 Dominio: Reasignación de portada al eliminar la fotografía de portada")
    void debeReasignarPortadaAlEliminarFotoPortada() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        FotografiaTienda f1 = almacen.agregarFotografia("https://blob/foto1.jpg", null);
        f1.setId(10L);
        FotografiaTienda f2 = almacen.agregarFotografia("https://blob/foto2.jpg", null);
        f2.setId(20L);

        almacen.eliminarFotografia(10L);

        assertThat(almacen.getFotos()).hasSize(1);
        assertThat(almacen.getFotos().get(0).getId()).isEqualTo(20L);
        assertThat(almacen.getFotos().get(0).isPortada()).isTrue();
        assertThat(almacen.getFotos().get(0).getOrden()).isEqualTo(1);
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto2.jpg");
    }

    @Test
    @DisplayName("SR-RF-74 Dominio: Debe rechazar URL de fotografía vacía o nula")
    void debeRechazarUrlInvalida() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Boho Chic")
                .build();

        assertThatThrownBy(() -> almacen.agregarFotografia(null, null))
                .isInstanceOf(ReglaDeNegocioException.class);

        assertThatThrownBy(() -> almacen.agregarFotografia("   ", null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}

