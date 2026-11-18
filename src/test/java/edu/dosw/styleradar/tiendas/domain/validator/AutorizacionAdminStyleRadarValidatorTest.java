package edu.dosw.styleradar.tiendas.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;

@DisplayName("Pruebas de Validador - Autorización Administrador StyleRadar (SR-RF-62 / SR-RF-63 / SR-RNF-02)")
class AutorizacionAdminStyleRadarValidatorTest {

    private AutorizacionAdminStyleRadarValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AutorizacionAdminStyleRadarValidator();
    }

    @Test
    @DisplayName("Debe permitir acceso con ROLE_STYLERADAR_ADMIN")
    void debePermitirAccesoConRoleAdmin() {
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("ROLE_STYLERADAR_ADMIN"));
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("ROLE_USER,ROLE_STYLERADAR_ADMIN"));
        assertDoesNotThrow(() -> validator.validarEsAdministradorParaFundaciones("ROLE_STYLERADAR_ADMIN"));
        assertDoesNotThrow(() -> validator.validarEsAdministradorParaFundaciones("STYLERADAR_ADMIN"));
    }

    @Test
    @DisplayName("Debe permitir acceso con formato corto STYLERADAR_ADMIN")
    void debePermitirAccesoConFormatoCorto() {
        assertDoesNotThrow(() -> validator.validarEsAdministradorStyleRadar("STYLERADAR_ADMIN"));
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
                validator.validarEsAdministradorStyleRadar("ROLE_STORE_ADMIN"));
    }

    @Test
    @DisplayName("Criterio 4c (B-0.1): sin identidad (roles nulos o vacíos) responde 401, no 403")
    void debeDenegarAccesoConRolesNulosOVacios() {
        assertThrows(FalloAutenticacionException.class, () ->
                validator.validarEsAdministradorStyleRadar(null));
        assertThrows(FalloAutenticacionException.class, () ->
                validator.validarEsAdministradorStyleRadar("   "));
    }

    @Test
    @DisplayName("B-4.3: En fundaciones el mensaje de 403 debe referirse a fundaciones aliadas")
    void debeEmitirMensajeDeFundacionesCuandoNoEsAdmin() {
        AccesoDenegadoAlmacenException ex = assertThrows(AccesoDenegadoAlmacenException.class, () ->
                validator.validarEsAdministradorParaFundaciones("ROLE_COMPRADOR"));
        assertEquals("Se requiere el rol ROLE_STYLERADAR_ADMIN para gestionar fundaciones aliadas", ex.getMessage());

        assertThrows(FalloAutenticacionException.class, () ->
                validator.validarEsAdministradorParaFundaciones(null));
    }
}
