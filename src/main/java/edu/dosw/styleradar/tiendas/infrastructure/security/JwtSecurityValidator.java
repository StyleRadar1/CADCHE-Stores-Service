package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import lombok.extern.slf4j.Slf4j;

/**
 * Validador criptográfico de tokens JWT HMAC-SHA256 (OWASP A04:2025 y A07:2025).
 * Implementa verificación estricta de firma digital, expiración temporal y entropía
 * mínima de clave secreta (mínimo 256 bits).
 */
@Component
@Slf4j
public class JwtSecurityValidator {

    public static final int MINIMA_LONGITUD_SECRETO_BYTES = 32; // 256 bits
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String defaultSecret;

    public JwtSecurityValidator(
            ObjectMapper objectMapper,
            @Value("${JWT_SECRET:change_this_secret_in_production_minimum_32_characters_hmac256}") String defaultSecret) {
        this.objectMapper = objectMapper;
        this.defaultSecret = defaultSecret;
    }

    /**
     * Valida el formato, firma criptográfica HMAC-SHA256 y vigencia temporal de un JWT.
     *
     * @param token Cadena del token en formato header.payload.signature
     * @param secret Clave secreta simétrica (mínimo 32 caracteres)
     * @return Map con los claims decodificados del token
     */
    public Map<String, Object> validarToken(String token, String secret) {
        if (token == null || token.isBlank()) {
            throw new FalloAutenticacionException("El token de autenticación no puede estar vacío");
        }

        String effectiveSecret = (secret != null && !secret.isBlank()) ? secret : defaultSecret;
        validarEntropiaClave(effectiveSecret);

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new FalloAutenticacionException("Estructura de token JWT inválida (se esperan 3 segmentos)");
        }

        String headerB64 = parts[0];
        String payloadB64 = parts[1];
        String signatureB64 = parts[2];

        // 1. Verificar firma HMAC-SHA256
        String firmaEsperada = calcularFirmaHmac(headerB64 + "." + payloadB64, effectiveSecret);
        if (!firmaEsperada.equals(signatureB64)) {
            log.warn("Firma digital JWT alterada o inválida detectada");
            throw new FalloAutenticacionException("La firma criptográfica del token es inválida o ha sido alterada");
        }

        // 2. Deserializar claims de forma segura
        Map<String, Object> claims = parsePayload(payloadB64);

        // 3. Validar expiración temporal (exp)
        validarExpiracion(claims);

        return claims;
    }

    public Map<String, Object> validarToken(String token) {
        return validarToken(token, defaultSecret);
    }

    /**
     * Genera un token JWT HMAC-SHA256 firmado para pruebas o emisión interna.
     */
    public String generarToken(Long userId, String roles, long duracionSegundos, String secret) {
        String effectiveSecret = (secret != null && !secret.isBlank()) ? secret : defaultSecret;
        validarEntropiaClave(effectiveSecret);

        long ahora = Instant.now().getEpochSecond();
        long exp = ahora + duracionSegundos;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"userId\":%d,\"roles\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId != null ? userId.toString() : "anonymous",
                userId != null ? userId : 0L,
                roles != null ? roles : "",
                ahora,
                exp
        );

        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signatureB64 = calcularFirmaHmac(headerB64 + "." + payloadB64, effectiveSecret);

        return headerB64 + "." + payloadB64 + "." + signatureB64;
    }

    public Long extraerUserId(String token) {
        Map<String, Object> claims = validarToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number num) {
            return num.longValue();
        }
        if (userIdObj instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }

    public String extraerRoles(String token) {
        Map<String, Object> claims = validarToken(token);
        Object rolesObj = claims.get("roles");
        return rolesObj != null ? rolesObj.toString() : "";
    }

    private void validarEntropiaClave(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMA_LONGITUD_SECRETO_BYTES) {
            throw new IllegalArgumentException(
                    "Falla criptográfica (OWASP A04:2025): La clave secreta JWT debe tener una longitud mínima de "
                            + MINIMA_LONGITUD_SECRETO_BYTES + " bytes (256 bits) para garantizar entropía suficiente");
        }
    }

    private String calcularFirmaHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Error al calcular firma HMAC-SHA256: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> parsePayload(String payloadB64) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
            return objectMapper.readValue(decoded, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new FalloAutenticacionException("El payload del token no contiene un JSON válido: " + ex.getMessage());
        }
    }

    private void validarExpiracion(Map<String, Object> claims) {
        Object expObj = claims.get("exp");
        if (expObj == null) {
            throw new FalloAutenticacionException("El token no contiene el claim obligatorio de expiración (exp)");
        }
        long expSeconds;
        if (expObj instanceof Number num) {
            expSeconds = num.longValue();
        } else {
            expSeconds = Long.parseLong(expObj.toString());
        }

        long currentSeconds = Instant.now().getEpochSecond();
        if (currentSeconds > expSeconds) {
            log.warn("Rechazo de token JWT expirado (exp={}, now={})", expSeconds, currentSeconds);
            throw new FalloAutenticacionException("El token de autenticación ha expirado");
        }
    }
}

