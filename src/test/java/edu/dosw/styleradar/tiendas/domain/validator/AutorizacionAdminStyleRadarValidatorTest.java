package edu.dosw.styleradar.tiendas.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;

@DisplayName("Pruebas de Validador - Autorización Administrador StyleRadar (SR-RF-62 / SR-RNF-02)")
class AutorizacionAdminStyleRadarValidatorTest {

    private AutorizacionAdminStyleRadarValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AutorizacionAdminStyleRadarValidator();
    }

    @Test
    @DisplayName("Debe permitir acceso con ROLE_ADMIN_STYLERADAR")
    void debePermitirAccesoConRoleAdmin() {
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("ROLE_ADMIN_STYLERADAR"));
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("ROLE_USER,ROLE_ADMIN_STYLERADAR"));
    }

    @Test
    @DisplayName("Debe permitir acceso con formato corto ADMIN_STYLERADAR")
    void debePermitirAccesoConFormatoCorto() {
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("ADMIN_STYLERADAR"));
    }

    @Test
    @DisplayName("Criterio 4: Dado que no soy administrador, cuando llamo al endpoint, entonces recibo 403 (AccesoDenegadoAlmacenException)")
    void debeDenegarAccesoConRolComprador() {
        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                validator.validarEsAdministradorStyleRadar("ROLE_COMPRADOR"));
    }

    @Test
    @DisplayName("Criterio 4b: Debe denegar acceso con rol de administrador de almacén (no es Admin StyleRadar)")
    void debeDenegarAccesoConRolAdminAlmacen() {
        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                validator.validarEsAdministradorStyleRadar("ROLE_ADMIN_ALMACEN"));
    }

    @Test
    @DisplayName("Criterio 4c: Debe denegar acceso cuando el header de roles es nulo o vacío")
    void debeDenegarAccesoConRolesNulosOVacios() {
        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                validator.validarEsAdministradorStyleRadar(null));
        assertThrows(AccesoDenegadoAlmacenException.class, () ->
                validator.validarEsAdministradorStyleRadar("   "));
    }
}
