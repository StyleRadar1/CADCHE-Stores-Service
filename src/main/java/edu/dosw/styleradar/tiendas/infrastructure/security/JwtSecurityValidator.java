package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Valida los JWT emitidos por ms-auth (HS256, secreto Base64 compartido {@code JWT_SECRET_BASE64}).
 * <p>
 * Formato de ms-auth: {@code sub} = id del usuario y {@code roles} = lista de roles. La firma se verifica
 * en tiempo constante con jjwt (OWASP A04/A07:2025). En qa/prod se exige un secreto propio: los secretos
 * de ejemplo publicados en los repositorios se rechazan al arrancar (fail-closed).
 */
@Component
@Slf4j
public final class JwtSecurityValidator {

    public static final int MINIMA_LONGITUD_SECRETO_BYTES = 32;

    private static final Set<String> HUELLAS_SECRETOS_DE_EJEMPLO = Set.of(
            "2cf5e6ec387461b4bf954f587ad4d957753fcbc48bf892b5e49996b90cf3b476",
            "d555cde718645d420c533072c69f70469efa375df07fa8c63ee180b4d8b86d99");

    private final SecretKey clave;

    public JwtSecurityValidator(
            @Value("${styleradar.security.jwt.secret-base64:}") String secretoBase64,
            Environment environment) {
        this.clave = construirClave(secretoBase64, environment);
    }

    /**
     * Identidad verificada contenida en el token.
     */
    public record IdentidadToken(Long usuarioId, Set<String> roles) {

        public IdentidadToken {
            roles = Set.copyOf(roles);
        }

        public String rolesCsv() {
            return String.join(",", roles);
        }
    }

    public IdentidadToken validar(String token) {
        if (token == null || token.isBlank()) {
            throw new FalloAutenticacionException("El token de autenticación no puede estar vacío");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token.trim()).getPayload();
        } catch (ExpiredJwtException ex) {
            throw new FalloAutenticacionException("El token de autenticación ha expirado");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Firma digital JWT alterada o token mal formado");
            throw new FalloAutenticacionException("La firma criptográfica del token es inválida o ha sido alterada");
        }
        return new IdentidadToken(leerUsuarioId(claims), leerRoles(claims.get("roles")));
    }

    private static Long leerUsuarioId(Claims claims) {
        String sub = claims.getSubject();
        if (sub == null) {
            throw new FalloAutenticacionException("El token no contiene un identificador de usuario válido (sub)");
        }
        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException ex) {
            throw new FalloAutenticacionException("El token no contiene un identificador de usuario válido (sub)");
        }
    }

    private static Set<String> leerRoles(Object valor) {
        Set<String> roles = new LinkedHashSet<>();
        if (valor instanceof Collection<?> lista) {
            lista.forEach(rol -> agregarRol(roles, String.valueOf(rol)));
        } else if (valor != null) {
            for (String rol : valor.toString().split(",")) {
                agregarRol(roles, rol);
            }
        }
        if (roles.isEmpty()) {
            throw new FalloAutenticacionException("El token no contiene roles");
        }
        return roles;
    }

    private static void agregarRol(Set<String> roles, String rol) {
        if (rol != null && !rol.isBlank()) {
            roles.add(rol.trim());
        }
    }

    private static SecretKey construirClave(String secretoBase64, Environment environment) {
        if (secretoBase64 == null || secretoBase64.isBlank()) {
            throw new IllegalStateException(
                    "Falla criptográfica (OWASP A04:2025): JWT_SECRET_BASE64 es obligatorio y debe ser el mismo de ms-auth");
        }
        String valor = secretoBase64.trim();
        if (environment != null && environment.acceptsProfiles(Profiles.of("qa", "prod")) && esSecretoDeEjemplo(valor)) {
            throw new IllegalStateException(
                    "Falla criptográfica (OWASP A04:2025): no se permite un secreto JWT de ejemplo en qa/prod");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(valor);
        } catch (DecodingException | IllegalArgumentException ex) {
            throw new IllegalStateException("Falla criptográfica (OWASP A04:2025): JWT_SECRET_BASE64 no es Base64 válido", ex);
        }
        if (bytes.length < MINIMA_LONGITUD_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "Falla criptográfica (OWASP A04:2025): el secreto JWT debe tener al menos 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static boolean esSecretoDeEjemplo(String valor) {
        try {
            byte[] huella = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HUELLAS_SECRETOS_DE_EJEMPLO.contains(HexFormat.of().formatHex(huella));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", ex);
        }
    }
}
