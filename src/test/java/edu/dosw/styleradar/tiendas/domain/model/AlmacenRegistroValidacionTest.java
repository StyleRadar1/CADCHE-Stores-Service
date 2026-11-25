package edu.dosw.styleradar.tiendas.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;

@DisplayName("Almacen.validarDatosObligatoriosRegistro: cada dato obligatorio del registro")
class AlmacenRegistroValidacionTest {

    private static Almacen valido() {
        return Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Almacén Centro")
                .correoContacto("tienda@styleradar.com")
                .latitud(4.65)
                .longitud(-74.06)
                .categorias(new HashSet<>(Set.of(CategoriaAlmacen.values()[0])))
                .urlRutPdf("https://blob/rut.pdf")
                .build();
    }

    static Object[][] invalidos() {
        return new Object[][] {
                {"nit nulo", (Consumer<Almacen>) a -> a.setNit(null)},
                {"nit en blanco", (Consumer<Almacen>) a -> a.setNit("  ")},
                {"nit con formato inválido", (Consumer<Almacen>) a -> a.setNit("ABC-1")},
                {"nombre nulo", (Consumer<Almacen>) a -> a.setNombreComercial(null)},
                {"nombre de 1 carácter", (Consumer<Almacen>) a -> a.setNombreComercial(" A ")},
                {"correo nulo", (Consumer<Almacen>) a -> a.setCorreoContacto(null)},
                {"correo en blanco", (Consumer<Almacen>) a -> a.setCorreoContacto(" ")},
                {"correo sin arroba", (Consumer<Almacen>) a -> a.setCorreoContacto("tienda.styleradar.com")},
                {"latitud < -90", (Consumer<Almacen>) a -> a.setLatitud(-90.5)},
                {"latitud > 90", (Consumer<Almacen>) a -> a.setLatitud(90.5)},
                {"longitud < -180", (Consumer<Almacen>) a -> a.setLongitud(-180.5)},
                {"longitud > 180", (Consumer<Almacen>) a -> a.setLongitud(180.5)},
                {"categorías nulas", (Consumer<Almacen>) a -> a.setCategorias(null)},
                {"categorías vacías", (Consumer<Almacen>) a -> a.setCategorias(new HashSet<>())},
                {"RUT nulo", (Consumer<Almacen>) a -> a.setUrlRutPdf(null)},
                {"RUT en blanco", (Consumer<Almacen>) a -> a.setUrlRutPdf(" ")},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void rechazaDatoInvalido(String caso, Consumer<Almacen> cambio) {
        Almacen almacen = valido();
        cambio.accept(almacen);
        assertThrows(ReglaDeNegocioException.class, almacen::validarDatosObligatoriosRegistro);
    }

    @Test
    void aceptaRegistroCompletoYCoordenadasOpcionales() {
        assertDoesNotThrow(valido()::validarDatosObligatoriosRegistro);
        Almacen sinCoordenadas = valido();
        sinCoordenadas.setLatitud(null);
        sinCoordenadas.setLongitud(null);
        assertDoesNotThrow(sinCoordenadas::validarDatosObligatoriosRegistro);
        Almacen nitSinDigito = valido();
        nitSinDigito.setNit("900123456");
        assertDoesNotThrow(nitSinDigito::validarDatosObligatoriosRegistro);
    }
}
