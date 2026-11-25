package edu.dosw.styleradar.tiendas.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;

@DisplayName("Fundacion: validación de registro y edición, y actualización de datos institucionales")
class FundacionValidacionTest {

    private static Fundacion valida() {
        return Fundacion.builder()
                .nombre("Fundación Abrigo")
                .nit("900215884-1")
                .barrio("Chapinero")
                .direccion("Calle 1 # 2-3")
                .email("contacto@abrigo.org")
                .latitud(4.65)
                .longitud(-74.06)
                .categoriasAceptadas(new HashSet<>(Set.of(TipoPrenda.values()[0])))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://blob/cert.pdf")
                .build();
    }

    static Object[][] invalidas() {
        return new Object[][] {
                {"nombre nulo", (Consumer<Fundacion>) f -> f.setNombre(null)},
                {"nombre en blanco", (Consumer<Fundacion>) f -> f.setNombre("  ")},
                {"nombre de 3 caracteres", (Consumer<Fundacion>) f -> f.setNombre("Abc")},
                {"nombre de 121 caracteres", (Consumer<Fundacion>) f -> f.setNombre("F".repeat(121))},
                {"nit en blanco", (Consumer<Fundacion>) f -> f.setNit(" ")},
                {"nit con formato inválido", (Consumer<Fundacion>) f -> f.setNit("NIT-1")},
                {"barrio en blanco", (Consumer<Fundacion>) f -> f.setBarrio(" ")},
                {"barrio de 81 caracteres", (Consumer<Fundacion>) f -> f.setBarrio("B".repeat(81))},
                {"dirección nula", (Consumer<Fundacion>) f -> f.setDireccion(null)},
                {"dirección en blanco", (Consumer<Fundacion>) f -> f.setDireccion(" ")},
                {"correo nulo", (Consumer<Fundacion>) f -> f.setEmail(null)},
                {"correo en blanco", (Consumer<Fundacion>) f -> f.setEmail(" ")},
                {"latitud nula", (Consumer<Fundacion>) f -> f.setLatitud(null)},
                {"latitud < -90", (Consumer<Fundacion>) f -> f.setLatitud(-91.0)},
                {"latitud > 90", (Consumer<Fundacion>) f -> f.setLatitud(91.0)},
                {"longitud nula", (Consumer<Fundacion>) f -> f.setLongitud(null)},
                {"longitud < -180", (Consumer<Fundacion>) f -> f.setLongitud(-181.0)},
                {"longitud > 180", (Consumer<Fundacion>) f -> f.setLongitud(181.0)},
                {"categorías nulas", (Consumer<Fundacion>) f -> f.setCategoriasAceptadas(null)},
                {"categorías vacías", (Consumer<Fundacion>) f -> f.setCategoriasAceptadas(new HashSet<>())},
                {"representante nulo", (Consumer<Fundacion>) f -> f.setRepresentanteId(null)},
                {"certificado nulo", (Consumer<Fundacion>) f -> f.setUrlCertificadoPersoneriaJuridica(null)},
                {"certificado en blanco", (Consumer<Fundacion>) f -> f.setUrlCertificadoPersoneriaJuridica(" ")},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidas")
    void registroRechazaDatoInvalido(String caso, Consumer<Fundacion> cambio) {
        Fundacion fundacion = valida();
        cambio.accept(fundacion);
        assertThrows(ReglaDeNegocioException.class, fundacion::validarDatosObligatoriosRegistro);
    }

    @Test
    void registroAceptaNitYBarrioOpcionales() {
        assertDoesNotThrow(valida()::validarDatosObligatoriosRegistro);
        Fundacion sinNitNiBarrio = valida();
        sinNitNiBarrio.setNit(null);
        sinNitNiBarrio.setBarrio(null);
        assertDoesNotThrow(sinNitNiBarrio::validarDatosObligatoriosRegistro);
        Fundacion edicionSinCertificado = valida();
        edicionSinCertificado.setUrlCertificadoPersoneriaJuridica(null);
        assertDoesNotThrow(edicionSinCertificado::validarDatosEdicion);
    }

    @Test
    void motivoDeCambioDeEstadoTieneMaximo500Caracteres() {
        assertDoesNotThrow(() -> Fundacion.validarMotivoCambioEstado(null));
        assertDoesNotThrow(() -> Fundacion.validarMotivoCambioEstado("x".repeat(500)));
        assertThrows(ReglaDeNegocioException.class, () -> Fundacion.validarMotivoCambioEstado("x".repeat(501)));
    }

    @Test
    void actualizarRecortaTextosYConservaNitBarrioYCertificadoSiNoLlegan() {
        Fundacion actual = valida();
        Fundacion cambios = valida();
        cambios.setNombre("  Fundación Nueva  ");
        cambios.setNit(null);
        cambios.setBarrio(null);
        cambios.setUrlCertificadoPersoneriaJuridica(null);
        cambios.setEmail(" Nuevo@Abrigo.ORG ");
        cambios.setDescripcion(" Ropa para todos ");
        cambios.setTelefono(" 3001234567 ");
        cambios.setCausasApoyadas(" Niñez ");
        cambios.setHorarioAtencion(" L-V 8-5 ");
        cambios.setPoliticasDonacion(" Ropa limpia ");

        actual.actualizarDatosInstitucionales(cambios);

        assertEquals("Fundación Nueva", actual.getNombre());
        assertEquals("900215884-1", actual.getNit());
        assertEquals("Chapinero", actual.getBarrio());
        assertEquals("https://blob/cert.pdf", actual.getUrlCertificadoPersoneriaJuridica());
        assertEquals("nuevo@abrigo.org", actual.getEmail());
        assertEquals("Ropa para todos", actual.getDescripcion());
        assertEquals("3001234567", actual.getTelefono());
        assertEquals("Niñez", actual.getCausasApoyadas());
        assertEquals("L-V 8-5", actual.getHorarioAtencion());
        assertEquals("Ropa limpia", actual.getPoliticasDonacion());
    }

    @Test
    void actualizarReemplazaNitBarrioYCertificadoYLimpiaOpcionalesNulos() {
        Fundacion actual = valida();
        actual.setDescripcion("vieja");
        Fundacion cambios = valida();
        cambios.setNit(" 800111222-3 ");
        cambios.setBarrio(" Usaquén ");
        cambios.setUrlCertificadoPersoneriaJuridica(" https://blob/nuevo.pdf ");

        actual.actualizarDatosInstitucionales(cambios);

        assertEquals("800111222-3", actual.getNit());
        assertEquals("Usaquén", actual.getBarrio());
        assertEquals("https://blob/nuevo.pdf", actual.getUrlCertificadoPersoneriaJuridica());
        assertNull(actual.getDescripcion());
        assertNull(actual.getTelefono());
        assertNull(actual.getCausasApoyadas());
        assertTrue(actual.getCategoriasAceptadas().contains(TipoPrenda.values()[0]));
        assertThrows(ReglaDeNegocioException.class, () -> actual.actualizarDatosInstitucionales(null));
    }
}
