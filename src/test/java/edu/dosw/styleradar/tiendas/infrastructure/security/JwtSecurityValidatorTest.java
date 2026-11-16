package edu.dosw.styleradar.tiendas.infrastructure.security;

import static edu.dosw.styleradar.tiendas.support.TokensDePrueba.SECRETO_AJENO;
import static edu.dosw.styleradar.tiendas.support.TokensDePrueba.SECRETO_DEV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.support.TokensDePrueba;
import io.jsonwebtoken.Jwts;

@DisplayName("JwtSecurityValidator · tokens de ms-auth (HS256, secreto Base64 compartido) · OWASP A02/A04/A07")
class JwtSecurityValidatorTest {

    private final JwtSecurityValidator validator = new JwtSecurityValidator(SECRETO_DEV, null);

    @Test
    @DisplayName("Lee sub como usuario y la lista de roles del token emitido por ms-auth")
    void debeLeerIdentidadDelTokenDeMsAuth() {
        String token = TokensDePrueba.token(15L, List.of("ADMIN_ALMACEN"), 3600);

        JwtSecurityValidator.IdentidadToken identidad = validator.validar(token);

        assertThat(identidad.usuarioId()).isEqualTo(15L);
        assertThat(identidad.roles()).containsExactly("ADMIN_ALMACEN");
        assertThat(identidad.rolesCsv()).isEqualTo("ADMIN_ALMACEN");
    }

    @Test
    @DisplayName("Acepta roles como texto separado por comas")
    void debeAceptarRolesComoTexto() {
        String token = Jwts.builder().subject("3").claim("roles", "COMPRADOR, ADMIN_STYLERADAR")
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        io.jsonwebtoken.io.Decoders.BASE64.decode(SECRETO_DEV)))
                .compact();

        assertThat(validator.validar(token).roles()).containsExactlyInAnyOrder("COMPRADOR", "ADMIN_STYLERADAR");
    }

    @Test
    @DisplayName("Rechaza token vencido, firmado con otro secreto, alterado, vacío o mal formado")
    void debeRechazarTokensInvalidos() {
        String vencido = TokensDePrueba.token(1L, List.of("COMPRADOR"), -120);
        String ajeno = TokensDePrueba.token(SECRETO_AJENO, 1L, List.of("ADMIN_STYLERADAR"), 3600);
        String valido = TokensDePrueba.token(1L, List.of("COMPRADOR"), 3600);
        String alterado = valido.substring(0, valido.length() - 3) + "abc";

        assertThatThrownBy(() -> validator.validar(vencido)).isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("ha expirado");
        assertThatThrownBy(() -> validator.validar(ajeno)).isInstanceOf(FalloAutenticacionException.class)
                .hasMessageContaining("inválida o ha sido alterada");
        assertThatThrownBy(() -> validator.validar(alterado)).isInstanceOf(FalloAutenticacionException.class);
        assertThatThrownBy(() -> validator.validar(" ")).isInstanceOf(FalloAutenticacionException.class);
        assertThatThrownBy(() -> validator.validar("no.es.jwt")).isInstanceOf(FalloAutenticacionException.class);
    }

    @Test
    @DisplayName("Rechaza tokens sin sub numérico o sin roles")
    void debeRechazarTokensSinIdentidad() {
        var clave = io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(SECRETO_DEV));
        var exp = new java.util.Date(System.currentTimeMillis() + 60_000);
        String sinSub = Jwts.builder().claim("roles", List.of("COMPRADOR")).expiration(exp).signWith(clave).compact();
        String sinRoles = Jwts.builder().subject("5").expiration(exp).signWith(clave).compact();

        assertThatThrownBy(() -> validator.validar(sinSub)).hasMessageContaining("identificador de usuario");
        assertThatThrownBy(() -> validator.validar(sinRoles)).hasMessageContaining("roles");
    }

    @Test
    @DisplayName("Falla al arrancar sin secreto, con secreto corto, no Base64 o de ejemplo en qa/prod (fail-closed)")
    void debeFallarAlArrancarConSecretoInseguro() {
        MockEnvironment qa = new MockEnvironment();
        qa.setActiveProfiles("qa");
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtSecurityValidator("", null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtSecurityValidator("Y29ydG8=", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("256 bits");
        assertThatThrownBy(() -> new JwtSecurityValidator("%%%no-base64%%%", null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtSecurityValidator(SECRETO_DEV, qa)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtSecurityValidator(
                "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdTZWNyZXRLZXlGb3JUZXN0czEyMzQ1Ng==", prod))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new JwtSecurityValidator(SECRETO_AJENO, prod)).isNotNull();
    }
}
