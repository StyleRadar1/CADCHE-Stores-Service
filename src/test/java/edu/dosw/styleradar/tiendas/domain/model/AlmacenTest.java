package edu.dosw.styleradar.tiendas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.PeriodoInvalidoException;
import edu.dosw.styleradar.tiendas.domain.exception.PrendaNoCumpleDistintivoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.UmbralInvalidoException;

@DisplayName("Pruebas de Dominio - Entidad Almacen, Umbral Efectivo (SR-RF-59), Periodo Nueva Llegada (SR-RF-60) y Galería (SR-RF-74)")
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
    @DisplayName("SR-RF-74 B-2.2: Reordenar fotografías actualiza orden y asigna la primera como portada")
    void debeReordenarFotografiasYValidarListaCompleta() {
        Almacen almacen = Almacen.builder().nit("900123456-1").build();
        FotografiaTienda f1 = almacen.agregarFotografia("https://blob/foto1.jpg", null);
        f1.setId(10L);
        FotografiaTienda f2 = almacen.agregarFotografia("https://blob/foto2.jpg", null);
        f2.setId(20L);

        almacen.reordenarFotografias(List.of(20L, 10L));

        assertThat(almacen.getFotos().get(0).getId()).isEqualTo(20L);
        assertThat(almacen.getFotos().get(0).isPortada()).isTrue();
        assertThat(almacen.getFotos().get(0).getOrden()).isEqualTo(1);
        assertThat(almacen.getFotos().get(1).getId()).isEqualTo(10L);
        assertThat(almacen.getFotos().get(1).isPortada()).isFalse();
        assertThat(almacen.getFotos().get(1).getOrden()).isEqualTo(2);
        assertThat(almacen.getFotoPortadaUrl()).isEqualTo("https://blob/foto2.jpg");

        assertThatThrownBy(() -> almacen.reordenarFotografias(List.of(20L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El nuevo orden debe incluir todas las fotografías actuales del almacén");
        assertThatThrownBy(() -> almacen.reordenarFotografias(List.of(20L, 99L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El nuevo orden debe incluir todas las fotografías actuales del almacén");
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

    @Test
    @DisplayName("SR-RF-62 Dominio: validarDatosObligatoriosRegistro valida NIT, nombre, correo, coordenadas, categorías y RUT")
    void debeValidarDatosObligatoriosRegistro() {
        Almacen valido = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Tienda Valida")
                .correoContacto("tienda@correo.com")
                .latitud(4.65)
                .longitud(-74.06)
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .urlRutPdf("https://blob/rut.pdf")
                .build();
        valido.validarDatosObligatoriosRegistro();

        assertThatThrownBy(() -> Almacen.builder().nit("invalido").build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("A").build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("Tienda").correoContacto("invalido").build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("Tienda").correoContacto("a@b.c").latitud(95.0).build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("Tienda").correoContacto("a@b.c").longitud(195.0).build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("Tienda").correoContacto("a@b.c").categorias(Set.of()).build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Almacen.builder().nit("900123456-1").nombreComercial("Tienda").correoContacto("a@b.c").categorias(Set.of(CategoriaAlmacen.CASUAL)).urlRutPdf(" ").build().validarDatosObligatoriosRegistro())
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("SR-RF-59 / SR-RF-60 B-1.4: Casos de borde en evaluarUltimasUnidades, calcularUltimasUnidades y calcularNuevaLlegada")
    void debeEvaluarDistintivosDePrendaEnDominioYCasosDeBorde() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .umbralUltimasUnidades(3)
                .diasNuevaLlegada(7)
                .build();

        // evaluarUltimasUnidades (con excepción 422 al no cumplir)
        assertThat(almacen.evaluarUltimasUnidades(3)).isTrue();
        assertThat(almacen.evaluarUltimasUnidades(1)).isTrue();
        assertThat(almacen.evaluarUltimasUnidades(0)).isFalse();
        assertThatThrownBy(() -> almacen.evaluarUltimasUnidades(-1))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> almacen.evaluarUltimasUnidades(4))
                .isInstanceOf(PrendaNoCumpleDistintivoException.class);

        // calcularUltimasUnidades (RN-01, RN-02: stock=umbral, stock=umbral+1, stock=0, disponible=false)
        assertThat(almacen.calcularUltimasUnidades(3, true)).isTrue();
        assertThat(almacen.calcularUltimasUnidades(4, true)).isFalse();
        assertThat(almacen.calcularUltimasUnidades(0, true)).isFalse();
        assertThat(almacen.calcularUltimasUnidades(2, false)).isFalse();
        assertThatThrownBy(() -> almacen.calcularUltimasUnidades(-1, true))
                .isInstanceOf(ReglaDeNegocioException.class);

        // calcularNuevaLlegada (RN-01, RN-02: último día vigente, día + 1 expirado, fecha futura, retirada=true)
        Instant ahora = Instant.now();
        assertThat(almacen.calcularNuevaLlegada(ahora.minus(7, ChronoUnit.DAYS), false, ahora)).isTrue();
        assertThat(almacen.calcularNuevaLlegada(ahora.minus(8, ChronoUnit.DAYS), false, ahora)).isFalse();
        // Duración exacta (contrato 1.2): pasada una hora del último día ya no es nueva llegada.
        assertThat(almacen.calcularNuevaLlegada(ahora.minus(7, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS), false, ahora))
                .isFalse();
        assertThat(almacen.calcularNuevaLlegada(ahora.minus(2, ChronoUnit.DAYS), true, ahora)).isFalse();
        assertThatThrownBy(() -> almacen.calcularNuevaLlegada(ahora.plus(1, ChronoUnit.DAYS), false, ahora))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La fecha de publicación no puede ser futura");

        assertThat(almacen.evaluarNuevaLlegada(ahora.minus(3, ChronoUnit.DAYS), ahora)).isTrue();
        assertThatThrownBy(() -> almacen.evaluarNuevaLlegada(null, ahora))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> almacen.evaluarNuevaLlegada(ahora.minus(10, ChronoUnit.DAYS), ahora))
                .isInstanceOf(PrendaNoCumpleDistintivoException.class);
    }

    @Test
    @DisplayName("SR-RF-62 B-3.2: decidirVerificacion exige motivo entre 10 y 1000 caracteres al rechazar")
    void debeValidarLongitudMotivoAlRechazar() {
        Almacen almacen = Almacen.builder()
                .nit("900123456-1")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf("https://blob/rut.pdf")
                .build();

        assertThatThrownBy(() -> almacen.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "Corto", 1L, Instant.now()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("entre 10 y 1000 caracteres");
    }
}
