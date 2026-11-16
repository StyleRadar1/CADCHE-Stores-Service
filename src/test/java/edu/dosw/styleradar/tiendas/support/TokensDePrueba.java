package edu.dosw.styleradar.tiendas.support;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite tokens con el mismo formato que ms-auth (HS256, {@code sub} = usuario, {@code roles} = lista).
 */
public final class TokensDePrueba {

    /** Secreto Base64 de desarrollo compartido con ms-auth (perfil dev/test). */
    public static final String SECRETO_DEV = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    /** Otro secreto válido de 256 bits, para simular un token firmado por un tercero. */
    public static final String SECRETO_AJENO = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";

    private TokensDePrueba() {
    }

    public static String token(Long usuarioId, List<String> roles, long segundosVigencia) {
        return token(SECRETO_DEV, usuarioId, roles, segundosVigencia);
    }

    public static String token(String secretoBase64, Long usuarioId, List<String> roles, long segundosVigencia) {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("roles", roles)
                .issuedAt(Date.from(ahora.minusSeconds(Math.max(0, -segundosVigencia) + 60)))
                .expiration(Date.from(ahora.plusSeconds(segundosVigencia)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
