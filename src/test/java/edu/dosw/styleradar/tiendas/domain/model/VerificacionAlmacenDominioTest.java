package edu.dosw.styleradar.tiendas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaDecididoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaPendienteException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;

@DisplayName("Pruebas de Dominio - Verificación de Almacén Aliado (SR-RF-62)")
class VerificacionAlmacenDominioTest {

    private Almacen almacenPendienteConRut;
    private Almacen almacenPendienteSinRut;

    @BeforeEach
    void setUp() {
        almacenPendienteConRut = Almacen.builder()
                .nit("900123456-1")
                .nombreComercial("Atelier Urbano Chapinero")
                .descripcion("Tienda de moda circular")
                .correoContacto("contacto@atelierurbano.co")
                .telefono("+57 601 312 4455")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf("https://styleradarblob.blob.core.windows.net/documentos-rut/900123456-1/rut.pdf")
                .categorias(Set.of(CategoriaAlmacen.CASUAL))
                .build();

        almacenPendienteSinRut = Almacen.builder()
                .nit("900999888-2")
                .nombreComercial("Boutique Sin Rut")
                .descripcion("Tienda sin soporte legal")
                .correoContacto("info@sinrut.co")
                .telefono("+57 601 999 0000")
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf(null)
                .categorias(Set.of(CategoriaAlmacen.FORMAL))
                .build();
    }

    @Test
    @DisplayName("Criterio 1: Debe aprobar almacén pendiente con NIT y RUT válido")
    void debeAprobarAlmacenPendienteConRut() {
        Instant ahora = Instant.now();
        almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "RUT y NIT verificados", 10L, ahora);

        assertEquals(EstadoVerificacionAlmacen.APROBADO, almacenPendienteConRut.getEstadoVerificacion());
        assertEquals("RUT y NIT verificados", almacenPendienteConRut.getMotivoVerificacion());
        assertEquals(10L, almacenPendienteConRut.getAdministradorDecisionId());
        assertEquals(ahora, almacenPendienteConRut.getFechaDecisionVerificacion());
    }

    @Test
    @DisplayName("Criterio 1b: Debe rechazar almacén pendiente con motivo explicativo")
    void debeRechazarAlmacenPendienteConMotivo() {
        Instant ahora = Instant.now();
        almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "Actividad económica del RUT no coincide", 10L, ahora);

        assertEquals(EstadoVerificacionAlmacen.RECHAZADO, almacenPendienteConRut.getEstadoVerificacion());
        assertEquals("Actividad económica del RUT no coincide", almacenPendienteConRut.getMotivoVerificacion());
        assertEquals(10L, almacenPendienteConRut.getAdministradorDecisionId());
        assertNotNull(almacenPendienteConRut.getFechaDecisionVerificacion());
    }

    @Test
    @DisplayName("Criterio 2: Dado un almacén sin RUT, cuando intento aprobarlo, entonces lanza RutRequeridoParaAprobacionException (422)")
    void debeLanzarExcepcionAlAprobarAlmacenSinRut() {
        assertThrows(RutRequeridoParaAprobacionException.class, () ->
                almacenPendienteSinRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "Aprobación sin RUT", 10L, Instant.now()));
    }

    @Test
    @DisplayName("Criterio 2b: Dado un almacén con RUT en blanco, cuando intento aprobarlo, entonces lanza RutRequeridoParaAprobacionException (422)")
    void debeLanzarExcepcionAlAprobarAlmacenConRutEnBlanco() {
        almacenPendienteSinRut.setUrlRutPdf("   ");
        assertThrows(RutRequeridoParaAprobacionException.class, () ->
                almacenPendienteSinRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "Aprobación", 10L, Instant.now()));
    }

    @Test
    @DisplayName("Criterio 3: Dado un almacén ya APROBADO, cuando lo decido otra vez, entonces lanza AlmacenYaDecididoException (409)")
    void debeLanzarExcepcionAlDecidirAlmacenYaAprobado() {
        almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "Primera decisión", 10L, Instant.now());

        assertThrows(AlmacenYaDecididoException.class, () ->
                almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "Cambio de opinión", 10L, Instant.now()));
    }

    @Test
    @DisplayName("Criterio 3b: Dado un almacén ya RECHAZADO, cuando lo decido otra vez, entonces lanza AlmacenYaDecididoException (409)")
    void debeLanzarExcepcionAlDecidirAlmacenYaRechazado() {
        almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "RUT ilegible", 10L, Instant.now());

        assertThrows(AlmacenYaDecididoException.class, () ->
                almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "Reintento", 10L, Instant.now()));
    }

    @Test
    @DisplayName("Negativo: El rechazo exige motivo explicativo no vacío")
    void debeLanzarExcepcionAlRechazarSinMotivo() {
        assertThrows(ReglaDeNegocioException.class, () ->
                almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "", 10L, Instant.now()));

        assertThrows(ReglaDeNegocioException.class, () ->
                almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, "   ", 10L, Instant.now()));

        assertThrows(ReglaDeNegocioException.class, () ->
                almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.RECHAZADO, null, 10L, Instant.now()));
    }

    @Test
    @DisplayName("Reabrir revisión: Pasa de APROBADO a PENDIENTE")
    void debeReabrirRevisionDeAlmacenDecidido() {
        almacenPendienteConRut.decidirVerificacion(DecisionVerificacionAlmacen.APROBADO, "Aprobado", 10L, Instant.now());
        almacenPendienteConRut.solicitarRevision();

        assertEquals(EstadoVerificacionAlmacen.PENDIENTE, almacenPendienteConRut.getEstadoVerificacion());
    }

    @Test
    @DisplayName("Reabrir revisión: Si ya está PENDIENTE lanza AlmacenYaPendienteException (422)")
    void debeLanzarExcepcionAlReabrirAlmacenYaPendiente() {
        assertThrows(AlmacenYaPendienteException.class, () -> almacenPendienteConRut.solicitarRevision());
    }
}
