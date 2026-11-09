package edu.dosw.styleradar.tiendas.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;

@DisplayName("Pruebas de Dominio - Fundación Aliada (SR-RF-63)")
class FundacionDominioTest {

    private Fundacion fundacionValida;

    @BeforeEach
    void setUp() {
        fundacionValida = Fundacion.builder()
                .nombre("Fundación Abrigo Solidario")
                .descripcion("Recolección y entrega de ropa a población vulnerable")
                .direccion("Calle 85 # 15-32, Chapinero, Bogotá")
                .telefono("+57 601 742 8899")
                .email("contacto@abrigosolidario.org")
                .latitud(4.6712)
                .longitud(-74.0531)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.CALZADO))
                .representanteId(24L)
                .urlCertificadoPersoneriaJuridica("https://styleradarblob.blob.core.windows.net/fundaciones/cert.pdf")
                .causasApoyadas("Niñez vulnerable y adultos mayores")
                .activa(true)
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Debe validar correctamente una fundación con datos completos y certificado de personería jurídica")
    void debeValidarFundacionCompletaExitosamente() {
        assertDoesNotThrow(() -> fundacionValida.validarDatosObligatoriosRegistro());
        assertTrue(fundacionValida.isActiva());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando el nombre es nulo o en blanco")
    void debeFallarCuandoNombreEsInvalido() {
        fundacionValida.setNombre(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());

        fundacionValida.setNombre("   ");
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando la dirección es nula o en blanco")
    void debeFallarCuandoDireccionEsInvalida() {
        fundacionValida.setDireccion(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando el email es nulo o en blanco")
    void debeFallarCuandoEmailEsInvalido() {
        fundacionValida.setEmail(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando las coordenadas están fuera de rango")
    void debeFallarCuandoCoordenadasSonInvalidas() {
        fundacionValida.setLatitud(95.0);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());

        fundacionValida.setLatitud(4.0);
        fundacionValida.setLongitud(-190.0);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando no acepta categorías de prenda")
    void debeFallarCuandoCategoriasEstanVacias() {
        fundacionValida.setCategoriasAceptadas(Set.of());
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());

        fundacionValida.setCategoriasAceptadas(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando el representanteId es nulo")
    void debeFallarCuandoRepresentanteEsNulo() {
        fundacionValida.setRepresentanteId(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Criterio 2: Debe lanzar ReglaDeNegocioException cuando no incluye certificado de personería jurídica")
    void debeFallarCuandoCertificadoEsNuloOEnBlanco() {
        fundacionValida.setUrlCertificadoPersoneriaJuridica(null);
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());

        fundacionValida.setUrlCertificadoPersoneriaJuridica("   ");
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.validarDatosObligatoriosRegistro());
    }

    @Test
    @DisplayName("Desactivación: Debe marcar la fundación como inactiva")
    void debeDesactivarFundacion() {
        fundacionValida.desactivar();
        assertFalse(fundacionValida.isActiva());
    }

    @Test
    @DisplayName("SR-RF-64 Dominio: Debe actualizar los datos editables (representante, teléfono, horarios, políticas)")
    void debeActualizarDatosEditablesExitosamente() {
        Fundacion cambios = Fundacion.builder()
                .nombre("Fundación Abrigo Solidario - Actualizada")
                .descripcion("Nueva descripción ampliada")
                .direccion("Carrera 7 # 72-10, Bogotá")
                .telefono("+57 310 999 8877")
                .email("nuevo_contacto@abrigosolidario.org")
                .latitud(4.6534)
                .longitud(-74.0621)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR, TipoPrenda.INFERIOR))
                .representanteId(99L)
                .horarioAtencion("Lunes a Viernes 08:00 - 18:00")
                .politicasDonacion("Prendas limpias y planchadas")
                .causasApoyadas("Familias campesinas y madres comunitarias")
                .urlCertificadoPersoneriaJuridica("https://styleradarblob.blob.core.windows.net/certificados/nuevo.pdf")
                .build();

        fundacionValida.actualizarDatosInstitucionales(cambios);

        org.junit.jupiter.api.Assertions.assertEquals("Fundación Abrigo Solidario - Actualizada", fundacionValida.getNombre());
        org.junit.jupiter.api.Assertions.assertEquals("+57 310 999 8877", fundacionValida.getTelefono());
        org.junit.jupiter.api.Assertions.assertEquals(99L, fundacionValida.getRepresentanteId());
        org.junit.jupiter.api.Assertions.assertEquals("Lunes a Viernes 08:00 - 18:00", fundacionValida.getHorarioAtencion());
        org.junit.jupiter.api.Assertions.assertEquals("Prendas limpias y planchadas", fundacionValida.getPoliticasDonacion());
        org.junit.jupiter.api.Assertions.assertEquals("Carrera 7 # 72-10, Bogotá", fundacionValida.getDireccion());
        org.junit.jupiter.api.Assertions.assertEquals("nuevo_contacto@abrigosolidario.org", fundacionValida.getEmail());
    }

    @Test
    @DisplayName("SR-RF-64 Dominio: Debe lanzar ReglaDeNegocioException al actualizar con objeto nulo")
    void debeFallarAlActualizarConCambiosNulos() {
        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.actualizarDatosInstitucionales(null));
    }

    @Test
    @DisplayName("SR-RF-64 Dominio: Debe lanzar ReglaDeNegocioException al editar con nombre vacío")
    void debeFallarAlEditarConNombreVacio() {
        Fundacion cambios = Fundacion.builder()
                .nombre("   ")
                .direccion("Calle 100")
                .email("info@fundacion.org")
                .latitud(4.5)
                .longitud(-74.1)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(10L)
                .build();

        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.actualizarDatosInstitucionales(cambios));
    }

    @Test
    @DisplayName("SR-RF-64 Dominio: Debe lanzar ReglaDeNegocioException al editar con representante nulo")
    void debeFallarAlEditarConRepresentanteNulo() {
        Fundacion cambios = Fundacion.builder()
                .nombre("Fundación Válida")
                .direccion("Calle 100")
                .email("info@fundacion.org")
                .latitud(4.5)
                .longitud(-74.1)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(null)
                .build();

        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.actualizarDatosInstitucionales(cambios));
    }

    @Test
    @DisplayName("SR-RF-64 Dominio: Debe lanzar ReglaDeNegocioException al editar con coordenadas inválidas")
    void debeFallarAlEditarConCoordenadasInvalidas() {
        Fundacion cambios = Fundacion.builder()
                .nombre("Fundación Válida")
                .direccion("Calle 100")
                .email("info@fundacion.org")
                .latitud(-95.0)
                .longitud(-74.1)
                .categoriasAceptadas(Set.of(TipoPrenda.SUPERIOR))
                .representanteId(10L)
                .build();

        assertThrows(ReglaDeNegocioException.class, () -> fundacionValida.actualizarDatosInstitucionales(cambios));
    }
}
