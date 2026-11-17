package edu.dosw.styleradar.tiendas.trazabilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Trazabilidad del análisis de requerimientos de ms-tiendas (Requerimientos_ms-tiendas_completo, 13/11/2026).
 * Cada prueba corresponde a un flujo alterno (FA) o regla de negocio (RN) de SR-RF-59, 60, 62-A, 62, 63, 64, 65 y 74
 * y verifica, por la API real con PostgreSQL 16 y JWT firmados como los de ms-auth, el código HTTP y el mensaje exactos
 * del documento. El servicio corre como en QA/PROD: no confía en cabeceras X-User-*. Solo se capturan los eventos.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "styleradar.security.trust-gateway-headers=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Trazabilidad del Word de ms-tiendas (SR-RF-59 a SR-RF-74)")
class TrazabilidadRequerimientosTiendasTest {

    private static final String SECRETO = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DATOS_INVALIDOS = "La solicitud contiene datos inválidos";
    private static final String EXPIRADO = "El token de autenticación ha expirado";
    private static final String FIRMA = "La firma criptográfica del token es inválida o ha sido alterada";
    private static final String SIN_PERMISO_FUNDACIONES =
            "Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas";
    private static final String NIT = "900123456-1";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("styleradar_tiendas");

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @MockBean
    private FundacionEventPublisherPort eventos;

    private final String tienda = token(20, "ADMIN_ALMACEN");
    private final String otraTienda = token(21, "ADMIN_ALMACEN");
    private final String admin = token(1, "ADMIN_STYLERADAR");
    private final String comprador = token(30, "COMPRADOR");

    @BeforeEach
    void limpiar() {
        jdbc.execute("TRUNCATE almacen_fotos, administradores_almacenes, almacen_categorias, almacenes, "
                + "fundacion_categorias_aceptadas, fundaciones RESTART IDENTITY CASCADE");
        reset(eventos);
    }

    // ---------------------------------------------------------------- utilidades

    private static String token(long usuarioId, String rol) {
        return firmar(usuarioId, rol, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private static String firmar(long usuarioId, String rol, Instant expira) {
        SecretKey clave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRETO));
        return Jwts.builder().subject(String.valueOf(usuarioId)).claim("roles", List.of(rol))
                .issuedAt(Date.from(expira.minus(2, ChronoUnit.HOURS))).expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS256).compact();
    }

    private ResultActions json(HttpMethod metodo, String ruta, String tok, Object cuerpo) throws Exception {
        MockHttpServletRequestBuilder r = switch (metodo.name()) {
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            case "PATCH" -> patch(ruta);
            case "DELETE" -> delete(ruta);
            default -> get(ruta);
        };
        if (tok != null) {
            r.header("Authorization", "Bearer " + tok);
        }
        if (cuerpo != null) {
            r.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cuerpo));
        }
        return mockMvc.perform(r);
    }

    private ResultActions subir(String ruta, String tok, String nombre, String tipo, byte[] contenido) throws Exception {
        return mockMvc.perform(multipart(ruta).file(new MockMultipartFile("archivo", nombre, tipo, contenido))
                .header("Authorization", "Bearer " + tok));
    }

    private static ResultActions error(ResultActions r, int http, String mensaje) throws Exception {
        r.andExpect(status().is(http))
                .andExpect(jsonPath("$.status").value(http))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
        if (mensaje != null) {
            r.andExpect(jsonPath("$.message").value(mensaje));
        }
        return r;
    }

    private static void campo(ResultActions r, String campo, String mensaje) throws Exception {
        r.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(DATOS_INVALIDOS))
                .andExpect(jsonPath("$.validationErrors." + campo).value(mensaje));
    }

    /** Las dos variantes del 401 del Word: token vencido y firma alterada. */
    private void fa01(HttpMethod metodo, String ruta, Object cuerpo) throws Exception {
        error(json(metodo, ruta, firmar(20, "ADMIN_STYLERADAR", Instant.now().minusSeconds(60)), cuerpo), 401, EXPIRADO);
        String alterado = admin.substring(0, admin.length() - 3) + (admin.endsWith("AAA") ? "BBB" : "AAA");
        error(json(metodo, ruta, alterado, cuerpo), 401, FIRMA);
        error(json(metodo, ruta, null, cuerpo), 401, null);
    }

    private Map<String, Object> almacen(String nit) {
        Map<String, Object> a = new HashMap<>();
        a.put("nit", nit);
        a.put("nombreComercial", "Atelier Urbano");
        a.put("correoContacto", "contacto@atelier.co");
        a.put("categorias", List.of("CASUAL"));
        a.put("urlRutPdf", "https://blob/rut.pdf");
        return a;
    }

    private void registrarAlmacen() throws Exception {
        json(HttpMethod.POST, "/api/v1/almacenes", tienda, almacen(NIT)).andExpect(status().isCreated());
    }

    private Map<String, Object> fundacion(String nombre, String nit, long representante) {
        Map<String, Object> f = new HashMap<>();
        f.put("nombre", nombre);
        f.put("nit", nit);
        f.put("barrio", "Chapinero");
        f.put("direccion", "Calle 85 # 15-32");
        f.put("email", "contacto@fundacion.org");
        f.put("latitud", 4.67);
        f.put("longitud", -74.05);
        f.put("categoriasAceptadas", List.of("SUPERIOR"));
        f.put("representanteId", representante);
        f.put("urlCertificadoPersoneriaJuridica", "https://blob/certificado.pdf");
        return f;
    }

    private long registrarFundacion(String nombre, String nit, long representante) throws Exception {
        String r = json(HttpMethod.POST, "/api/v1/admin/fundaciones", admin, fundacion(nombre, nit, representante))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private static byte[] png() {
        byte[] b = new byte[64];
        b[0] = (byte) 0x89;
        b[1] = 'P';
        b[2] = 'N';
        b[3] = 'G';
        return b;
    }

    // ---------------------------------------------------------------- SR-RF-59

    @Nested
    @DisplayName("SR-RF-59 Marcar prenda como últimas unidades")
    class UltimasUnidades {

        private final String config = "/api/v1/almacenes/" + NIT + "/configuracion-catalogo";
        private final String prenda = "/api/v1/almacenes/" + NIT + "/prendas/7/ultimas-unidades";

        @BeforeEach
        void almacen() throws Exception {
            registrarAlmacen();
        }

        private JsonNode calcular(int stock, boolean disponible) throws Exception {
            return objectMapper.readTree(json(HttpMethod.PATCH, prenda, tienda,
                    Map.of("stockActual", stock, "disponible", disponible))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        }

        @Test
        @DisplayName("RN-03: umbral por defecto 3 y se guarda entre 0 y 50")
        void rn03() throws Exception {
            json(HttpMethod.GET, config, tienda, null).andExpect(jsonPath("$.umbralUltimasUnidades").value(3));
            json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 50, "diasNuevaLlegada", 7))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.umbralUltimasUnidades").value(50));
        }

        @Test
        @DisplayName("RN-01 y RN-02: distintivo si 1 ≤ stock ≤ umbral; stock 0 o no disponible → NINGUNO (agotado prevalece)")
        void rn01Rn02() throws Exception {
            assertThat(calcular(3, true).get("distintivoActivo").asText()).isEqualTo("ULTIMAS_UNIDADES");
            assertThat(calcular(1, true).get("ultimasUnidades").asBoolean()).isTrue();
            assertThat(calcular(4, true).get("distintivoActivo").asText()).isEqualTo("NINGUNO");
            assertThat(calcular(0, true).get("distintivoActivo").asText()).isEqualTo("NINGUNO");
            assertThat(calcular(2, false).get("distintivoActivo").asText()).isEqualTo("NINGUNO");
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, config,
                    Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 7));
        }

        @Test
        @DisplayName("FA-02 y RN-04: almacén que no administra → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PUT, config, otraTienda, Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 7)),
                    403, "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/almacenes/811111111-1/configuracion-catalogo", tienda,
                    Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 7)),
                    404, "No se encontró ningún almacén registrado con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04, FA-05 y FA-06: umbral vacío, negativo o mayor que 50 → 400")
        void fa04a06() throws Exception {
            campo(json(HttpMethod.PUT, config, tienda, Map.of("diasNuevaLlegada", 7)),
                    "umbralUltimasUnidades", "El umbral de últimas unidades es obligatorio");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", -1, "diasNuevaLlegada", 7)),
                    "umbralUltimasUnidades", "El umbral de últimas unidades no puede ser negativo");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 51, "diasNuevaLlegada", 7)),
                    "umbralUltimasUnidades", "El umbral de últimas unidades no puede superar 50");
        }

        @Test
        @DisplayName("FA-07: stock vacío o negativo → 400")
        void fa07() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("disponible", true)),
                    "stockActual", "El stock actual es obligatorio");
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("stockActual", -1, "disponible", true)),
                    "stockActual", "El stock actual no puede ser negativo");
        }
    }

    // ---------------------------------------------------------------- SR-RF-60

    @Nested
    @DisplayName("SR-RF-60 Marcar prenda como nueva llegada")
    class NuevaLlegada {

        private final String config = "/api/v1/almacenes/" + NIT + "/configuracion-catalogo";
        private final String prenda = "/api/v1/almacenes/" + NIT + "/prendas/8/nueva-llegada";

        @BeforeEach
        void almacen() throws Exception {
            registrarAlmacen();
        }

        private String calcular(Instant publicacion, boolean retirada) throws Exception {
            return objectMapper.readTree(json(HttpMethod.PATCH, prenda, tienda,
                    Map.of("fechaPublicacion", publicacion.toString(), "retirada", retirada))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                    .get("distintivoActivo").asText();
        }

        @Test
        @DisplayName("RN-03: periodo por defecto 7 días; entre 1 y 60")
        void rn03() throws Exception {
            json(HttpMethod.GET, config, tienda, null).andExpect(jsonPath("$.diasNuevaLlegada").value(7));
            json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 60))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("RN-01 y RN-02: nueva durante el periodo; al día siguiente o retirada → NINGUNO")
        void rn01Rn02() throws Exception {
            Instant ahora = Instant.now();
            assertThat(calcular(ahora.minus(6, ChronoUnit.DAYS), false)).isEqualTo("NUEVA_PRENDA");
            assertThat(calcular(ahora.minus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES), false)).isEqualTo("NUEVA_PRENDA");
            assertThat(calcular(ahora.minus(8, ChronoUnit.DAYS), false)).isEqualTo("NINGUNO");
            assertThat(calcular(ahora.minus(1, ChronoUnit.DAYS), true)).isEqualTo("NINGUNO");
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, config,
                    Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 7));
        }

        @Test
        @DisplayName("FA-02 y RN-04: almacén que no administra → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/almacenes/" + NIT + "/dias-nueva-llegada", otraTienda,
                    Map.of("diasNuevaLlegada", 14)), 403,
                    "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/almacenes/811111111-1/dias-nueva-llegada", tienda,
                    Map.of("diasNuevaLlegada", 14)), 404, "No se encontró ningún almacén registrado con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04, FA-05 y FA-06: periodo vacío, menor que 1 o mayor que 60 → 400")
        void fa04a06() throws Exception {
            campo(json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 3)),
                    "diasNuevaLlegada", "El periodo en días de nueva llegada es obligatorio");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 0)),
                    "diasNuevaLlegada", "El periodo de nueva llegada debe ser de al menos 1 día");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("umbralUltimasUnidades", 3, "diasNuevaLlegada", 61)),
                    "diasNuevaLlegada", "El periodo de nueva llegada no puede superar 60 días");
            campo(json(HttpMethod.PUT, "/api/v1/almacenes/" + NIT + "/dias-nueva-llegada", tienda, Map.of()),
                    "diasNuevaLlegada", "El periodo en días de nueva llegada es obligatorio");
        }

        @Test
        @DisplayName("FA-07: fecha de publicación vacía o futura → 400")
        void fa07() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("retirada", false)),
                    "fechaPublicacion", "La fecha de publicación es obligatoria");
            campo(json(HttpMethod.PATCH, prenda, tienda,
                    Map.of("fechaPublicacion", Instant.now().plus(2, ChronoUnit.DAYS).toString(), "retirada", false)),
                    "fechaPublicacion", "La fecha de publicación no puede ser futura");
        }
    }

    // ---------------------------------------------------------------- SR-RF-62-A

    @Nested
    @DisplayName("SR-RF-62-A Registrar almacén para verificación")
    class RegistrarAlmacen {

        @Test
        @DisplayName("Flujo básico, RN-01 y RN-04: el almacén nace PENDIENTE, con RUT y asociado a la Tienda")
        void flujoBasico() throws Exception {
            json(HttpMethod.POST, "/api/v1/almacenes", tienda, almacen(NIT))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"))
                    .andExpect(jsonPath("$.urlRutPdf").value("https://blob/rut.pdf"))
                    .andExpect(jsonPath("$.representante").isNotEmpty());
            json(HttpMethod.GET, "/api/v1/almacenes/mi-almacen", tienda, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nit").value(NIT));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.POST, "/api/v1/almacenes", almacen(NIT));
        }

        @Test
        @DisplayName("FA-02: cuenta que no es de Tienda → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.POST, "/api/v1/almacenes", comprador, almacen(NIT)),
                    403, "Solo una cuenta con rol de tienda puede registrar un almacén");
        }

        @Test
        @DisplayName("FA-03 y RN-02: NIT repetido → 409 con el campo nit")
        void fa03() throws Exception {
            registrarAlmacen();
            error(json(HttpMethod.POST, "/api/v1/almacenes", otraTienda, almacen(NIT)),
                    409, "Ya existe un almacén registrado con el NIT: " + NIT)
                    .andExpect(jsonPath("$.validationErrors.nit").exists());
        }

        @Test
        @DisplayName("FA-04 y RN-03: la Tienda ya administra un almacén → 409")
        void fa04() throws Exception {
            registrarAlmacen();
            error(json(HttpMethod.POST, "/api/v1/almacenes", tienda, almacen("800111222-3")),
                    409, "El usuario ya administra un almacén");
        }

        @Test
        @DisplayName("FA-05 a FA-08 y FA-10: NIT, razón social, correo, categorías y RUT inválidos → 400 por campo")
        void fa05a08yFa10() throws Exception {
            Map<String, Object> vacio = new HashMap<>();
            ResultActions r = json(HttpMethod.POST, "/api/v1/almacenes", tienda, vacio);
            campo(r, "nit", "El NIT del almacén es obligatorio");
            campo(r, "nombreComercial", "El nombre comercial es obligatorio");
            campo(r, "correoContacto", "El correo de contacto es obligatorio");
            campo(r, "categorias", "Debe indicar al menos una categoría comercial");
            campo(r, "urlRutPdf", "La URL o referencia del documento RUT en PDF es obligatoria para verificar la personería jurídica");
            Map<String, Object> malo = almacen("90012ABC");
            malo.put("nombreComercial", "A");
            malo.put("correoContacto", "sin-arroba");
            r = json(HttpMethod.POST, "/api/v1/almacenes", tienda, malo);
            campo(r, "nit", "El NIT debe tener un formato válido (ej. 900123456-1)");
            campo(r, "nombreComercial", "El nombre comercial debe tener entre 2 y 120 caracteres");
            campo(r, "correoContacto", "El correo de contacto debe tener un formato válido");
        }

        @Test
        @DisplayName("FA-09: RUT que no es PDF o pesa más de 5 MB → 400")
        void fa09() throws Exception {
            error(subir("/api/v1/almacenes/documentos/rut", tienda, "rut.png", "image/png", png()),
                    400, "El RUT debe ser un archivo PDF de máximo 5 MB");
            error(subir("/api/v1/almacenes/documentos/rut", tienda, "rut.pdf", "application/pdf",
                    new byte[5 * 1024 * 1024 + 1]), 400, "El RUT debe ser un archivo PDF de máximo 5 MB");
            subir("/api/v1/almacenes/documentos/rut", tienda, "rut.pdf", "application/pdf", "%PDF-1.7".getBytes())
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.url").isNotEmpty());
        }
    }

    // ---------------------------------------------------------------- SR-RF-62

    @Nested
    @DisplayName("SR-RF-62 Verificar almacén aliado")
    class VerificarAlmacen {

        private final String verificacion = "/api/v1/admin/almacenes/" + NIT + "/verificacion";

        @BeforeEach
        void almacen() throws Exception {
            registrarAlmacen();
        }

        @Test
        @DisplayName("Flujo básico y RN-01: el Admin aprueba y queda quién decidió; listado paginado por estado")
        void flujoBasico() throws Exception {
            json(HttpMethod.GET, "/api/v1/admin/almacenes/verificacion?estado=PENDIENTE&page=0&size=20", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].nit").value(NIT))
                    .andExpect(jsonPath("$.totalElements").value(1));
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoVerificacion").value("APROBADO"))
                    .andExpect(jsonPath("$.administradorDecisionId").value(1));
        }

        @Test
        @DisplayName("RN-02 y RN-05: rechazar con motivo de 10 a 1000 caracteres deja el motivo")
        void rn02Rn05() throws Exception {
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "RECHAZADO", "motivo", "La cámara de comercio está vencida."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoVerificacion").value("RECHAZADO"))
                    .andExpect(jsonPath("$.motivoVerificacion").value("La cámara de comercio está vencida."));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PATCH, verificacion, Map.of("decision", "APROBADO"));
        }

        @Test
        @DisplayName("FA-02: usuario sin rol Admin → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PATCH, verificacion, tienda, Map.of("decision", "APROBADO")),
                    403, "Se requiere el rol ROLE_ADMIN_STYLERADAR para verificar almacenes aliados");
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.GET, "/api/v1/admin/almacenes/811111111-1/verificacion", admin, null),
                    404, "No se encontró ningún almacén registrado con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04 y RN-04: decisión ya tomada → 409")
        void fa04() throws Exception {
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO")).andExpect(status().isOk());
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "RECHAZADO", "motivo", "Motivo suficiente")),
                    409, "El almacén con NIT " + NIT + " ya fue decidido previamente. Estado actual: APROBADO");
        }

        @Test
        @DisplayName("FA-05 y RN-03: aprobar sin RUT → 422")
        void fa05() throws Exception {
            jdbc.update("UPDATE almacenes SET url_rut_pdf = NULL WHERE nit = ?", NIT);
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO")), 422,
                    "No es posible aprobar el almacén con NIT " + NIT + " porque no cuenta con el documento RUT adjunto en PDF");
        }

        @Test
        @DisplayName("FA-06: rechazo con motivo vacío o de menos de 10 caracteres → 400 en el campo motivo")
        void fa06() throws Exception {
            campo(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "RECHAZADO", "motivo", "")),
                    "motivo", "El rechazo exige un motivo no vacío");
            campo(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "RECHAZADO", "motivo", "corto")),
                    "motivo", "El motivo debe tener entre 10 y 1000 caracteres");
        }

        @Test
        @DisplayName("FA-07: campos distintos a la decisión y el motivo → 400")
        void fa07() throws Exception {
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO", "estado", "APROBADO")),
                    400, "Campo no permitido en la decisión de verificación");
        }

        @Test
        @DisplayName("FA-08: retirar verificación de una tienda pendiente → 422; decidida vuelve a PENDIENTE")
        void fa08() throws Exception {
            error(json(HttpMethod.POST, verificacion + "/solicitar-revision", admin, null),
                    422, "El almacén ya se encuentra pendiente de revisión");
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO"));
            json(HttpMethod.POST, verificacion + "/solicitar-revision", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"));
        }
    }

    // ---------------------------------------------------------------- SR-RF-63 y SR-RF-64

    /** Validaciones comunes de los datos de la fundación (FA-04 a FA-14 de RF-63 y FA-05 a FA-14 de RF-64). */
    private void validacionesFundacion(HttpMethod metodo, String ruta) throws Exception {
        ResultActions r = json(metodo, ruta, admin, Map.of());
        campo(r, "nombre", "El nombre es obligatorio");
        campo(r, "nit", "El NIT es obligatorio");
        campo(r, "barrio", "El barrio es obligatorio");
        campo(r, "direccion", "La dirección es obligatoria");
        campo(r, "email", "El email es obligatorio");
        campo(r, "latitud", "La latitud es obligatoria");
        campo(r, "longitud", "La longitud es obligatoria");
        campo(r, "categoriasAceptadas", "Debe aceptar al menos una categoría de prenda");
        campo(r, "representanteId", "El ID del usuario representante es obligatorio");
        Map<String, Object> malo = fundacion("Fun", "9002", 40);
        malo.put("barrio", "b".repeat(81));
        malo.put("direccion", "d".repeat(201));
        malo.put("email", "sin-arroba");
        malo.put("latitud", 91);
        malo.put("longitud", -181);
        r = json(metodo, ruta, admin, malo);
        campo(r, "nombre", "El nombre debe tener entre 4 y 120 caracteres");
        campo(r, "nit", "El NIT debe tener un formato válido (ej. 900215884-1)");
        campo(r, "barrio", "El barrio no puede superar 80 caracteres");
        campo(r, "direccion", "La dirección no puede superar 200 caracteres");
        campo(r, "email", "El email no tiene un formato válido");
        campo(r, "latitud", "La latitud debe estar entre -90 y 90");
        campo(r, "longitud", "La longitud debe estar entre -180 y 180");
        malo = fundacion("x".repeat(121), "900215884-1", 40);
        campo(json(metodo, ruta, admin, malo), "nombre", "El nombre debe tener entre 4 y 120 caracteres");
    }

    @Nested
    @DisplayName("SR-RF-63 Registrar fundación aliada")
    class RegistrarFundacion {

        private final String ruta = "/api/v1/admin/fundaciones";

        @Test
        @DisplayName("Flujo básico y RN-05: la fundación nace activa con todos sus datos")
        void flujoBasico() throws Exception {
            json(HttpMethod.POST, ruta, admin, fundacion("Fundación Abrigo", "900215884-1", 24))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.activa").value(true))
                    .andExpect(jsonPath("$.nit").value("900215884-1"))
                    .andExpect(jsonPath("$.barrio").value("Chapinero"))
                    .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.POST, ruta, fundacion("Fundación Uno", "900215884-1", 24));
        }

        @Test
        @DisplayName("FA-02 y RN-01: usuario sin rol Admin → 403 «…para gestionar fundaciones aliadas»")
        void fa02() throws Exception {
            error(json(HttpMethod.POST, ruta, tienda, fundacion("Fundación Uno", "900215884-1", 24)), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("FA-03, FA-15, FA-16 y RN-02/RN-03: nombre, representante o NIT repetidos → 409 con el campo")
        void fa03Fa15Fa16() throws Exception {
            registrarFundacion("Fundación Abrigo", "900215884-1", 24);
            error(json(HttpMethod.POST, ruta, admin, fundacion("Fundación Abrigo", "900215885-1", 25)),
                    409, "Ya existe una fundación aliada registrada con el nombre: Fundación Abrigo")
                    .andExpect(jsonPath("$.validationErrors.nombre").exists());
            error(json(HttpMethod.POST, ruta, admin, fundacion("Otra Fundación", "900215885-1", 24)),
                    409, "El usuario con ID 24 ya se encuentra asignado como representante de otra fundación aliada")
                    .andExpect(jsonPath("$.validationErrors.representanteId").exists());
            error(json(HttpMethod.POST, ruta, admin, fundacion("Otra Fundación", "900215884-1", 25)),
                    409, "Ya existe una fundación aliada registrada con el NIT: 900215884-1")
                    .andExpect(jsonPath("$.validationErrors.nit").exists());
        }

        @Test
        @DisplayName("FA-04 y RN-04: sin certificado de personería jurídica → 400")
        void fa04() throws Exception {
            Map<String, Object> sinCertificado = fundacion("Fundación Abrigo", "900215884-1", 24);
            sinCertificado.remove("urlCertificadoPersoneriaJuridica");
            error(json(HttpMethod.POST, ruta, admin, sinCertificado), 400,
                    "El certificado de personería jurídica en PDF es obligatorio para registrar la fundación aliada");
        }

        @Test
        @DisplayName("FA-05 a FA-14: validaciones de cada campo → 400 por campo")
        void fa05aFa14() throws Exception {
            validacionesFundacion(HttpMethod.POST, ruta);
        }
    }

    @Nested
    @DisplayName("SR-RF-64 Editar fundación aliada")
    class EditarFundacion {

        private long id;
        private String ruta;

        @BeforeEach
        void fundaciones() throws Exception {
            registrarFundacion("Fundación Abrigo", "900215884-1", 24);
            id = registrarFundacion("Fundación Segunda", "900215886-1", 26);
            ruta = "/api/v1/admin/fundaciones/" + id;
        }

        @Test
        @DisplayName("Flujo básico y RN-03: editar conserva activa y fechaRegistro")
        void flujoBasico() throws Exception {
            String antes = json(HttpMethod.GET, ruta, admin, null).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            json(HttpMethod.PUT, ruta, admin, fundacion("Fundación Segunda Editada", "900215886-1", 26))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Fundación Segunda Editada"))
                    .andExpect(jsonPath("$.activa").value(true))
                    .andExpect(jsonPath("$.fechaRegistro").value(objectMapper.readTree(antes).get("fechaRegistro").asText()));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, ruta, fundacion("Fundación Dos", "900215886-1", 26));
        }

        @Test
        @DisplayName("FA-02 y RN-01: usuario sin rol Admin → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.GET, ruta, tienda, null), 403, SIN_PERMISO_FUNDACIONES);
            error(json(HttpMethod.PUT, ruta, tienda, fundacion("Fundación Dos", "900215886-1", 26)), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("FA-03: fundación inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.GET, "/api/v1/admin/fundaciones/999999", admin, null),
                    404, "No existe una fundación aliada con ID: 999999");
        }

        @Test
        @DisplayName("FA-04, FA-15 y FA-16: nombre, representante o NIT de otra fundación → 409")
        void fa04Fa15Fa16() throws Exception {
            error(json(HttpMethod.PUT, ruta, admin, fundacion("Fundación Abrigo", "900215886-1", 26)),
                    409, "Ya existe otra fundación aliada registrada con el nombre: Fundación Abrigo");
            error(json(HttpMethod.PUT, ruta, admin, fundacion("Fundación Segunda", "900215886-1", 24)),
                    409, "El usuario con ID 24 ya se encuentra asignado como representante de otra fundación aliada")
                    .andExpect(jsonPath("$.validationErrors.representanteId").exists());
            error(json(HttpMethod.PUT, ruta, admin, fundacion("Fundación Segunda", "900215884-1", 26)),
                    409, "Ya existe una fundación aliada registrada con el NIT: 900215884-1")
                    .andExpect(jsonPath("$.validationErrors.nit").exists());
        }

        @Test
        @DisplayName("FA-05 a FA-14: validaciones de cada campo → 400 por campo")
        void fa05aFa14() throws Exception {
            validacionesFundacion(HttpMethod.PUT, ruta);
        }
    }

    // ---------------------------------------------------------------- SR-RF-65

    @Nested
    @DisplayName("SR-RF-65 Desactivar y reactivar fundación aliada")
    class DesactivarFundacion {

        private long id;

        @BeforeEach
        void fundacion() throws Exception {
            id = registrarFundacion("Fundación Abrigo", "900215884-1", 24);
        }

        @Test
        @DisplayName("Flujo básico, RN-01 a RN-03 y RN-05: desactivar y reactivar publican sus eventos con todos los campos")
        void flujoBasico() throws Exception {
            json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
            ArgumentCaptor<FundacionDesactivadaEvent> desactivada = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
            verify(eventos).publicarFundacionDesactivada(desactivada.capture());
            assertThat(desactivada.getValue().getEventoId()).isNotBlank();
            assertThat(desactivada.getValue().getRepresentanteId()).isEqualTo(24L);
            assertThat(desactivada.getValue().getMotivo()).isEqualTo("Desactivación institucional por administrador StyleRadar");
            json(HttpMethod.GET, "/api/v1/admin/fundaciones/" + id, admin, null).andExpect(status().isOk());

            mockMvc.perform(patch("/api/v1/admin/fundaciones/" + id + "/activar").param("motivo", "Revisión superada")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
            ArgumentCaptor<FundacionReactivadaEvent> reactivada = ArgumentCaptor.forClass(FundacionReactivadaEvent.class);
            verify(eventos).publicarFundacionReactivada(reactivada.capture());
            assertThat(reactivada.getValue().getMotivo()).isEqualTo("Revisión superada");
            assertThat(reactivada.getValue().getFundacionId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Motivo de más de 500 caracteres → 400 y no publica evento")
        void motivoLargo() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar?motivo=" + "x".repeat(501), admin, null),
                    400, "El motivo no puede superar 500 caracteres");
            verify(eventos, never()).publicarFundacionDesactivada(any());
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar", null);
        }

        @Test
        @DisplayName("FA-02 y RN-04: usuario sin rol Admin → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/activar", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("FA-03: fundación inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/999999/desactivar", admin, null),
                    404, "No existe una fundación aliada con ID: 999999");
        }

        @Test
        @DisplayName("FA-04 y FA-05: ya desactivada o ya activa → 409")
        void fa04Fa05() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/activar", admin, null),
                    409, "La fundación aliada ya se encuentra activa");
            json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar", admin, null).andExpect(status().isOk());
            error(json(HttpMethod.PATCH, "/api/v1/admin/fundaciones/" + id + "/desactivar", admin, null),
                    409, "La fundación aliada ya se encuentra desactivada");
        }
    }

    // ---------------------------------------------------------------- SR-RF-74

    @Nested
    @DisplayName("SR-RF-74 Registrar fotografías de la tienda física")
    class Fotografias {

        private final String fotos = "/api/v1/almacenes/" + NIT + "/fotografias";

        @BeforeEach
        void almacen() throws Exception {
            registrarAlmacen();
        }

        private long subirFoto(String nombre) throws Exception {
            return objectMapper.readTree(subir(fotos, tienda, nombre, "image/png", png())
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        }

        @Test
        @DisplayName("RN-01 a RN-03: máximo 6; la primera es portada; al reordenar o eliminar la nueva primera es portada")
        void reglas() throws Exception {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                ids.add(subirFoto("f" + i + ".png"));
            }
            json(HttpMethod.GET, fotos, tienda, null)
                    .andExpect(jsonPath("$[0].portada").value(true)).andExpect(jsonPath("$[0].orden").value(1))
                    .andExpect(jsonPath("$[1].portada").value(false));
            List<Long> invertido = new ArrayList<>(ids);
            java.util.Collections.reverse(invertido);
            json(HttpMethod.PUT, fotos + "/orden", tienda, Map.of("fotoIds", invertido))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(invertido.get(0)))
                    .andExpect(jsonPath("$[0].portada").value(true));
            json(HttpMethod.DELETE, fotos + "/" + invertido.get(0), tienda, null).andExpect(status().isNoContent());
            json(HttpMethod.GET, fotos, tienda, null)
                    .andExpect(jsonPath("$.length()").value(5))
                    .andExpect(jsonPath("$[0].id").value(invertido.get(1)))
                    .andExpect(jsonPath("$[0].portada").value(true));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, fotos + "/orden", Map.of("fotoIds", List.of(1)));
        }

        @Test
        @DisplayName("FA-02 y RN-04: almacén que no administra → 403")
        void fa02() throws Exception {
            error(subir(fotos, otraTienda, "f.png", "image/png", png()), 403,
                    "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(subir("/api/v1/almacenes/811111111-1/fotografias", tienda, "f.png", "image/png", png()),
                    404, "No se encontró ningún almacén con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04: séptima foto → 422")
        void fa04() throws Exception {
            for (int i = 0; i < 6; i++) {
                subirFoto("f" + i + ".png");
            }
            error(subir(fotos, tienda, "f7.png", "image/png", png()), 422,
                    "El almacén ya cuenta con el máximo permitido de 6 fotografías de tienda física");
        }

        @Test
        @DisplayName("FA-05: archivo vacío → 400")
        void fa05() throws Exception {
            error(subir(fotos, tienda, "vacia.png", "image/png", new byte[0]), 400,
                    "Se requiere adjuntar un archivo de imagen válido");
        }

        @Test
        @DisplayName("FA-06: formato distinto de JPEG, PNG o WebP (incluido GIF) → 400")
        void fa06() throws Exception {
            error(subir(fotos, tienda, "doc.pdf", "application/pdf", "%PDF".getBytes()), 400,
                    "El archivo 'doc.pdf' no es una imagen válida. Formatos permitidos: JPEG, PNG, WebP");
            error(subir(fotos, tienda, "a.gif", "image/gif", "GIF89a".getBytes()), 400,
                    "El archivo 'a.gif' no es una imagen válida. Formatos permitidos: JPEG, PNG, WebP");
        }

        @Test
        @DisplayName("FA-07: imagen de más de 6 MB → 413")
        void fa07() throws Exception {
            byte[] grande = new byte[6 * 1024 * 1024 + 1];
            grande[0] = (byte) 0x89;
            error(subir(fotos, tienda, "grande.png", "image/png", grande), 413, "La imagen supera el tamaño máximo de 6 MB");
        }

        @Test
        @DisplayName("FA-08: quitar una foto que no es de su galería → 400")
        void fa08() throws Exception {
            error(json(HttpMethod.DELETE, fotos + "/999999", tienda, null), 400,
                    "La fotografía no pertenece a la galería del almacén");
        }

        @Test
        @DisplayName("FA-09: el nuevo orden no incluye todas las fotos actuales → 400")
        void fa09() throws Exception {
            long a = subirFoto("a.png");
            subirFoto("b.png");
            error(json(HttpMethod.PUT, fotos + "/orden", tienda, Map.of("fotoIds", List.of(a))), 400,
                    "El nuevo orden debe incluir todas las fotografías actuales del almacén");
        }
    }
}
