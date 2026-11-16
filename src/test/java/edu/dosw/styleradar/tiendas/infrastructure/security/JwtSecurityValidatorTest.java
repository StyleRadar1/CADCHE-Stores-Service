package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;

@DisplayName("Pruebas Unitarias de Cobertura Exhaustiva para JwtSecurityValidator (OWASP A04/A07)")
class JwtSecurityValidatorTest {

    private static final String DEFAULT_SECRET = "styleradar_super_secret_jwt_key_2025_at_least_32_bytes_long";
    private static final String CUSTOM_SECRET = "another_custom_secret_key_minimum_32_bytes_for_testing!";
    private static final String SHORT_SECRET = "short_key";

    private ObjectMapper objectMapper;
    private JwtSecurityValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        validator = new JwtSecurityValidator(objectMapper, DEFAULT_SECRET);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Debe lanzar FalloAutenticacionException cuando el token es nulo o vacío")
    void debeRechazarTokenVacio(String token) {
        assertThatThrownBy(() -> validator.validarToken(token))
                .isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("El token de autenticación no puede estar vacío");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalido", "header.payload", "a.b.c.d", "solo-un-segmento"})
    @DisplayName("Debe lanzar FalloAutenticacionException cuando la estructura no tiene exactamente 3 partes")
    void debeRechazarEstructuraInvalida(String token) {
        assertThatThrownBy(() -> validator.validarToken(token))
                .isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("Estructura de token JWT inválida");
    }

    @Test
    @DisplayName("Debe validar token exitosamente usando la clave por defecto")
    void debeValidarTokenConClavePorDefecto() {
        String token = validator.generarToken(15L, "ROLE_ADMIN_ALMACEN", 3600, null);
        Map<String, Object> claims = validator.validarToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("userId")).isEqualTo(15);
        assertThat(claims.get("roles")).isEqualTo("ROLE_ADMIN_ALMACEN");
    }

    @Test
    @DisplayName("Debe validar token exitosamente usando una clave personalizada")
    void debeValidarTokenConClavePersonalizada() {
        String token = validator.generarToken(20L, "ROLE_ADMIN_STYLERADAR", 1800, CUSTOM_SECRET);
        Map<String, Object> claims = validator.validarToken(token, CUSTOM_SECRET);

        assertThat(claims).isNotNull();
        assertThat(claims.get("userId")).isEqualTo(20);
        assertThat(claims.get("roles")).isEqualTo("ROLE_ADMIN_STYLERADAR");
    }

    @Test
    @DisplayName("Debe usar clave por defecto si el secret proporcionado en validarToken es nulo o en blanco")
    void debeUsarClavePorDefectoSiSecretEsNuloOEnBlanco() {
        String token = validator.generarToken(25L, "ROLE_COMPRADOR", 1200, DEFAULT_SECRET);

        Map<String, Object> claims1 = validator.validarToken(token, null);
        Map<String, Object> claims2 = validator.validarToken(token, "   ");

        assertThat(claims1).isNotNull();
        assertThat(claims2).isNotNull();
        assertThat(claims1.get("userId")).isEqualTo(25);
    }

    @Test
    @DisplayName("Debe rechazar token cuando la firma criptográfica es incorrecta")
    void debeRechazarFirmaInvalida() {
        String token = validator.generarToken(10L, "ROLE_USER", 3600, DEFAULT_SECRET);
        String tokenFirmaCorrupta = token.substring(0, token.lastIndexOf('.') + 1) + "FirmaInvalida123";

        assertThatThrownBy(() -> validator.validarToken(tokenFirmaCorrupta))
                .isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("La firma criptográfica del token es inválida");
    }

    @Test
    @DisplayName("Debe rechazar clave secreta si tiene menos de 32 bytes de entropía")
    void debeRechazarClaveCorta() {
        assertThatThrownBy(() -> validator.generarToken(1L, "ROLE_USER", 3600, SHORT_SECRET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La clave secreta JWT debe tener una longitud mínima de 32 bytes");
    }

    @Test
    @DisplayName("Debe rechazar clave nula en validarEntropiaClave")
    void debeRechazarClaveNulaEnConstructorOValidacion() {
        JwtSecurityValidator validatorSinClave = new JwtSecurityValidator(objectMapper, null);

        assertThatThrownBy(() -> validatorSinClave.generarToken(1L, "ROLE_USER", 3600, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La clave secreta JWT debe tener una longitud mínima de 32 bytes");
    }

    @Test
    @DisplayName("Debe rechazar token cuando el payload no es un JSON válido")
    void debeRechazarPayloadNoJson() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("EstoNoEsUnJsonValido".getBytes(StandardCharsets.UTF_8));
        String firma = "DummySignature";

        // Usamos un validador con firma simulada que falle en parsePayload
        String token = header + "." + payload + "." + firma;

        assertThatThrownBy(() -> validator.validarToken(token))
                .isInstanceOf(FalloAutenticacionException.class);
    }

    @Test
    @DisplayName("Debe rechazar token cuando no incluye el claim 'exp'")
    void debeRechazarTokenSinClaimExp() {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"123\",\"userId\":123,\"roles\":\"ROLE_USER\"}";

        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Firmar legítimamente con DEFAULT_SECRET
        String token = firmarManualmente(headerB64, payloadB64, DEFAULT_SECRET);

        assertThatThrownBy(() -> validator.validarToken(token))
                .isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("El token no contiene el claim obligatorio de expiración (exp)");
    }

    @Test
    @DisplayName("Debe parsear correctamente el claim 'exp' cuando viene representado como String")
    void debeParsearClaimExpComoString() {
        long futuro = Instant.now().getEpochSecond() + 3600;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"123\",\"userId\":123,\"exp\":\"%d\"}", futuro);

        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String token = firmarManualmente(headerB64, payloadB64, DEFAULT_SECRET);
        Map<String, Object> claims = validator.validarToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo("123");
    }

    @Test
    @DisplayName("Debe rechazar token expirado con mensaje explícito")
    void debeRechazarTokenExpirado() {
        String tokenExpirado = validator.generarToken(10L, "ROLE_USER", -500, DEFAULT_SECRET);

        assertThatThrownBy(() -> validator.validarToken(tokenExpirado))
                .isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("El token de autenticación ha expirado");
    }

    @Test
    @DisplayName("Debe generar token anónimo cuando userId o roles son nulos")
    void debeGenerarTokenConParametrosNulos() {
        String token = validator.generarToken(null, null, 1800, DEFAULT_SECRET);
        Map<String, Object> claims = validator.validarToken(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo("anonymous");
        assertThat(claims.get("userId")).isEqualTo(0);
        assertThat(claims.get("roles")).isEqualTo("");
    }

    @Test
    @DisplayName("Debe extraer userId numérico y roles desde token válido")
    void debeExtraerUserIdYRolesNumericos() {
        String token = validator.generarToken(99L, "ROLE_ADMIN_ALMACEN", 3600, DEFAULT_SECRET);

        Long userId = validator.extraerUserId(token);
        String roles = validator.extraerRoles(token);

        assertThat(userId).isEqualTo(99L);
        assertThat(roles).isEqualTo("ROLE_ADMIN_ALMACEN");
    }

    @Test
    @DisplayName("Debe extraer userId cuando viene serializado como String")
    void debeExtraerUserIdComoString() {
        long futuro = Instant.now().getEpochSecond() + 3600;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"77\",\"userId\":\"77\",\"exp\":%d}", futuro);

        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String token = firmarManualmente(headerB64, payloadB64, DEFAULT_SECRET);

        Long userId = validator.extraerUserId(token);
        assertThat(userId).isEqualTo(77L);
    }

    @Test
    @DisplayName("Debe retornar null en extraerUserId y vacío en extraerRoles si no están en los claims")
    void debeRetornarValoresPorDefectoSiClaimsNoExisten() {
        long futuro = Instant.now().getEpochSecond() + 3600;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"vacio\",\"exp\":%d}", futuro);

        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String token = firmarManualmente(headerB64, payloadB64, DEFAULT_SECRET);

        Long userId = validator.extraerUserId(token);
        String roles = validator.extraerRoles(token);

        assertThat(userId).isNull();
        assertThat(roles).isEmpty();
    }

    private String firmarManualmente(String headerB64, String payloadB64, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] firma = mac.doFinal((headerB64 + "." + payloadB64).getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
            return headerB64 + "." + payloadB64 + "." + signatureB64;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
