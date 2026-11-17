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
 * Trazabilidad del análisis de requerimientos y del contrato Front ↔ ms-tiendas (versión final) de ms-tiendas (Requerimientos_ms-tiendas_completo, 13/11/2026).
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
        json(HttpMethod.POST, "/api/v1/stores", tienda, almacen(NIT)).andExpect(status().isCreated());
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
        String r = json(HttpMethod.POST, "/api/v1/admin/foundations", admin, fundacion(nombre, nit, representante))
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

        private final String config = "/api/v1/stores/" + NIT + "/catalog-settings";
        private final String prenda = "/api/v1/stores/" + NIT + "/items/7/low-stock";

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
            error(json(HttpMethod.PUT, "/api/v1/stores/811111111-1/catalog-settings", tienda,
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

        private final String config = "/api/v1/stores/" + NIT + "/catalog-settings";
        private final String prenda = "/api/v1/stores/" + NIT + "/items/8/new-arrival";

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
            error(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/new-arrival-days", otraTienda,
                    Map.of("diasNuevaLlegada", 14)), 403,
                    "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/stores/811111111-1/new-arrival-days", tienda,
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
            campo(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/new-arrival-days", tienda, Map.of()),
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
            json(HttpMethod.POST, "/api/v1/stores", tienda, almacen(NIT))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"))
                    .andExpect(jsonPath("$.urlRutPdf").value("https://blob/rut.pdf"))
                    .andExpect(jsonPath("$.representante").isNotEmpty());
            json(HttpMethod.GET, "/api/v1/users/20/store", tienda, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nit").value(NIT));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.POST, "/api/v1/stores", almacen(NIT));
        }

        @Test
        @DisplayName("FA-02: cuenta que no es de Tienda → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.POST, "/api/v1/stores", comprador, almacen(NIT)),
                    403, "Solo una cuenta con rol de tienda puede registrar un almacén");
        }

        @Test
        @DisplayName("FA-03 y RN-02: NIT repetido → 409 con el campo nit")
        void fa03() throws Exception {
            registrarAlmacen();
            error(json(HttpMethod.POST, "/api/v1/stores", otraTienda, almacen(NIT)),
                    409, "Ya existe un almacén registrado con el NIT: " + NIT)
                    .andExpect(jsonPath("$.validationErrors.nit").exists());
        }

        @Test
        @DisplayName("FA-04 y RN-03: la Tienda ya administra un almacén → 409")
        void fa04() throws Exception {
            registrarAlmacen();
            error(json(HttpMethod.POST, "/api/v1/stores", tienda, almacen("800111222-3")),
                    409, "El usuario ya administra un almacén");
        }

        @Test
        @DisplayName("FA-05 a FA-08 y FA-10: NIT, razón social, correo, categorías y RUT inválidos → 400 por campo")
        void fa05a08yFa10() throws Exception {
            Map<String, Object> vacio = new HashMap<>();
            ResultActions r = json(HttpMethod.POST, "/api/v1/stores", tienda, vacio);
            campo(r, "nit", "El NIT del almacén es obligatorio");
            campo(r, "nombreComercial", "El nombre comercial es obligatorio");
            campo(r, "correoContacto", "El correo de contacto es obligatorio");
            campo(r, "categorias", "Debe indicar al menos una categoría comercial");
            campo(r, "urlRutPdf", "La URL o referencia del documento RUT en PDF es obligatoria para verificar la personería jurídica");
            Map<String, Object> malo = almacen("90012ABC");
            malo.put("nombreComercial", "A");
            malo.put("correoContacto", "sin-arroba");
            r = json(HttpMethod.POST, "/api/v1/stores", tienda, malo);
            campo(r, "nit", "El NIT debe tener un formato válido (ej. 900123456-1)");
            campo(r, "nombreComercial", "El nombre comercial debe tener entre 2 y 120 caracteres");
            campo(r, "correoContacto", "El correo de contacto debe tener un formato válido");
        }

        @Test
        @DisplayName("FA-09: RUT que no es PDF o pesa más de 5 MB → 400")
        void fa09() throws Exception {
            error(subir("/api/v1/stores/documents/rut", tienda, "rut.png", "image/png", png()),
                    400, "El RUT debe ser un archivo PDF de máximo 5 MB");
            error(subir("/api/v1/stores/documents/rut", tienda, "rut.pdf", "application/pdf",
                    new byte[5 * 1024 * 1024 + 1]), 400, "El RUT debe ser un archivo PDF de máximo 5 MB");
            subir("/api/v1/stores/documents/rut", tienda, "rut.pdf", "application/pdf", "%PDF-1.7".getBytes())
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.url").isNotEmpty());
        }
    }

    // ---------------------------------------------------------------- SR-RF-62

    @Nested
    @DisplayName("SR-RF-62 Verificar almacén aliado")
    class VerificarAlmacen {

        private final String verificacion = "/api/v1/admin/stores/" + NIT + "/verification";

        @BeforeEach
        void almacen() throws Exception {
            registrarAlmacen();
        }

        @Test
        @DisplayName("Flujo básico y RN-01: el Admin aprueba y queda quién decidió; listado paginado por estado")
        void flujoBasico() throws Exception {
            json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=PENDIENTE&page=0&size=20", admin, null)
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
            error(json(HttpMethod.GET, "/api/v1/admin/stores/811111111-1/verification", admin, null),
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
            error(json(HttpMethod.POST, verificacion + "/review-request", admin, null),
                    422, "El almacén ya se encuentra pendiente de revisión");
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APROBADO"));
            json(HttpMethod.POST, verificacion + "/review-request", admin, null)
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

        private final String ruta = "/api/v1/admin/foundations";

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
            ruta = "/api/v1/admin/foundations/" + id;
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
            error(json(HttpMethod.GET, "/api/v1/admin/foundations/999999", admin, null),
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
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
            ArgumentCaptor<FundacionDesactivadaEvent> desactivada = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
            verify(eventos).publicarFundacionDesactivada(desactivada.capture());
            assertThat(desactivada.getValue().getEventoId()).isNotBlank();
            assertThat(desactivada.getValue().getRepresentanteId()).isEqualTo(24L);
            assertThat(desactivada.getValue().getMotivo()).isEqualTo("Desactivación institucional por administrador StyleRadar");
            json(HttpMethod.GET, "/api/v1/admin/foundations/" + id, admin, null).andExpect(status().isOk());

            mockMvc.perform(patch("/api/v1/admin/foundations/" + id + "/activate").param("reason", "Revisión superada")
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
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate?reason=" + "x".repeat(501), admin, null),
                    400, "El motivo no puede superar 500 caracteres");
            verify(eventos, never()).publicarFundacionDesactivada(any());
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", null);
        }

        @Test
        @DisplayName("FA-02 y RN-04: usuario sin rol Admin → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/activate", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("FA-03: fundación inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/999999/deactivate", admin, null),
                    404, "No existe una fundación aliada con ID: 999999");
        }

        @Test
        @DisplayName("FA-04 y FA-05: ya desactivada o ya activa → 409")
        void fa04Fa05() throws Exception {
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/activate", admin, null),
                    409, "La fundación aliada ya se encuentra activa");
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", admin, null).andExpect(status().isOk());
            error(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", admin, null),
                    409, "La fundación aliada ya se encuentra desactivada");
        }
    }

    // ---------------------------------------------------------------- SR-RF-74

    @Nested
    @DisplayName("SR-RF-74 Registrar fotografías de la tienda física")
    class Fotografias {

        private final String fotos = "/api/v1/stores/" + NIT + "/photos";

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
            json(HttpMethod.PUT, fotos + "/order", tienda, Map.of("fotoIds", invertido))
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
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, fotos + "/order", Map.of("fotoIds", List.of(1)));
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
            error(subir("/api/v1/stores/811111111-1/photos", tienda, "f.png", "image/png", png()),
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
            error(json(HttpMethod.PUT, fotos + "/order", tienda, Map.of("fotoIds", List.of(a))), 400,
                    "El nuevo orden debe incluir todas las fotografías actuales del almacén");
        }
    }

    // ---------------------------------------------------------------- Contrato Front ↔ ms-tiendas

    @Nested
    @DisplayName("Contrato Front ↔ ms-tiendas (versión final 13/11/2026)")
    class ContratoFront {

        private static final String FECHA_UTC = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

        private JsonNode cuerpo(ResultActions r) throws Exception {
            return objectMapper.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }

        private void campos(JsonNode nodo, String... nombres) {
            assertThat(nodo.fieldNames()).toIterable().contains(nombres);
        }

        @Test
        @DisplayName("0.2: todo error trae timestamp, status, error, message, path y validationErrors")
        void formatoDeError() throws Exception {
            JsonNode e = cuerpo(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, Map.of()));
            campos(e, "timestamp", "status", "error", "message", "path", "validationErrors");
            assertThat(e.get("error").asText()).isEqualTo("Bad Request");
            assertThat(e.get("path").asText()).isEqualTo("/api/v1/admin/foundations");
            JsonNode n = cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations/999999", admin, null));
            assertThat(n.get("error").asText()).isEqualTo("Not Found");
        }

        @Test
        @DisplayName("0.1 y B-0.1: sin token, toda función restringida responde 401 (también las consultas de Admin)")
        void sinToken() throws Exception {
            error(json(HttpMethod.GET, "/api/v1/admin/foundations", null, null), 401, null);
            error(json(HttpMethod.GET, "/api/v1/admin/stores/verifications", null, null), 401, null);
            error(json(HttpMethod.GET, "/api/v1/users/20/store", null, null), 401, null);
            error(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/low-stock-threshold", null,
                    Map.of("umbralUltimasUnidades", 3)), 401, null);
        }

        @Test
        @DisplayName("1.1 y 3A.1: /users/{userId}/store responde 404 si la Tienda aún no registra uno")
        void miAlmacenSinRegistrar() throws Exception {
            json(HttpMethod.GET, "/api/v1/users/20/store", tienda, null).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sin /me: /users/{userId}/store solo para el mismo usuario o un Admin; otro usuario → 403")
        void almacenDelUsuario() throws Exception {
            registrarAlmacen();
            json(HttpMethod.GET, "/api/v1/users/20/store", tienda, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nit").value(NIT));
            json(HttpMethod.GET, "/api/v1/users/20/store", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nit").value(NIT));
            error(json(HttpMethod.GET, "/api/v1/users/20/store", otraTienda, null), 403,
                    "El usuario autenticado (21) no tiene permisos para consultar los recursos del usuario 20");
            error(json(HttpMethod.GET, "/api/v1/users/20/store", comprador, null), 403, null);
        }

        @Test
        @DisplayName("1.1 y 1.3: configuración con nit, umbral, días y fechaActualizacion en UTC; PUT solo del umbral")
        void configuracion() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/stores/" + NIT;
            JsonNode c = cuerpo(json(HttpMethod.PUT, base + "/catalog-settings", tienda,
                    Map.of("umbralUltimasUnidades", 5, "diasNuevaLlegada", 14)).andExpect(status().isOk()));
            campos(c, "nit", "umbralUltimasUnidades", "diasNuevaLlegada", "fechaActualizacion");
            assertThat(c.get("nit").asText()).isEqualTo(NIT);
            assertThat(c.get("fechaActualizacion").asText()).matches(FECHA_UTC);
            json(HttpMethod.PUT, base + "/low-stock-threshold", tienda, Map.of("umbralUltimasUnidades", 9))
                    .andExpect(status().isOk());
            json(HttpMethod.GET, base + "/catalog-settings", tienda, null)
                    .andExpect(jsonPath("$.umbralUltimasUnidades").value(9))
                    .andExpect(jsonPath("$.diasNuevaLlegada").value(14));
            campo(json(HttpMethod.PUT, base + "/low-stock-threshold", tienda, Map.of("umbralUltimasUnidades", 51)),
                    "umbralUltimasUnidades", "El umbral de últimas unidades no puede superar 50");
        }

        @Test
        @DisplayName("1.3: el distintivo trae nit, itemId, banderas, umbral, días, distintivoActivo y fecha; usa la configuración guardada")
        void distintivo() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/stores/" + NIT;
            json(HttpMethod.PUT, base + "/catalog-settings", tienda, Map.of("umbralUltimasUnidades", 5, "diasNuevaLlegada", 14));
            JsonNode u = cuerpo(json(HttpMethod.PATCH, base + "/items/7/low-stock", tienda,
                    Map.of("stockActual", 5, "disponible", true)).andExpect(status().isOk()));
            campos(u, "nit", "itemId", "ultimasUnidades", "umbralUltimasUnidades", "nuevaLlegada", "diasNuevaLlegada",
                    "distintivoActivo", "fechaActualizacion");
            assertThat(u.get("itemId").asLong()).isEqualTo(7);
            assertThat(u.get("umbralUltimasUnidades").asInt()).isEqualTo(5);
            assertThat(u.get("distintivoActivo").asText()).isEqualTo("ULTIMAS_UNIDADES");
            assertThat(u.get("fechaActualizacion").asText()).matches(FECHA_UTC);
            JsonNode n = cuerpo(json(HttpMethod.PATCH, base + "/items/8/new-arrival", tienda,
                    Map.of("fechaPublicacion", Instant.now().minus(13, ChronoUnit.DAYS).toString(), "retirada", false)));
            assertThat(n.get("nuevaLlegada").asBoolean()).isTrue();
            assertThat(n.get("diasNuevaLlegada").asInt()).isEqualTo(14);
            assertThat(n.get("distintivoActivo").asText()).isEqualTo("NUEVA_PRENDA");
            error(json(HttpMethod.PATCH, base + "/items/7/low-stock", otraTienda,
                    Map.of("stockActual", 1, "disponible", true)), 403, null);
        }

        @Test
        @DisplayName("2.3: cada foto trae id, almacenNit, url, orden, portada y fechaRegistro; el Comprador puede ver la galería")
        void fotos() throws Exception {
            registrarAlmacen();
            JsonNode f = cuerpo(subir("/api/v1/stores/" + NIT + "/photos", tienda, "f.png", "image/png", png())
                    .andExpect(status().isCreated()));
            campos(f, "id", "almacenNit", "url", "orden", "portada", "fechaRegistro");
            assertThat(f.get("almacenNit").asText()).isEqualTo(NIT);
            assertThat(f.get("orden").asInt()).isEqualTo(1);
            assertThat(f.get("fechaRegistro").asText()).matches(FECHA_UTC);
            json(HttpMethod.GET, "/api/v1/stores/" + NIT + "/photos", comprador, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].portada").value(true));
            subir("/api/v1/stores/" + NIT + "/photos", tienda, "w.webp", "image/webp",
                    new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}).andExpect(status().isCreated());
            subir("/api/v1/stores/" + NIT + "/photos", tienda, "j.jpg", "image/jpeg",
                    new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("3A.2 y 3A.4: campos opcionales del almacén con sus límites y categorías fuera del catálogo")
        void opcionalesAlmacen() throws Exception {
            Map<String, Object> a = almacen(NIT);
            a.put("descripcion", "d".repeat(501));
            a.put("telefono", "1".repeat(31));
            a.put("latitud", 91);
            a.put("longitud", 181);
            ResultActions r = json(HttpMethod.POST, "/api/v1/stores", tienda, a);
            campo(r, "descripcion", "La descripción no puede superar 500 caracteres");
            campo(r, "telefono", "El teléfono no puede superar 30 caracteres");
            campo(r, "latitud", "La latitud debe estar entre -90 y 90");
            campo(r, "longitud", "La longitud debe estar entre -180 y 180");
            Map<String, Object> malaCategoria = almacen(NIT);
            malaCategoria.put("categorias", List.of("ROPA_INTERIOR"));
            error(json(HttpMethod.POST, "/api/v1/stores", tienda, malaCategoria), 400, null);
            Map<String, Object> completo = almacen(NIT);
            completo.put("descripcion", "Moda urbana");
            completo.put("telefono", "+57 300 000 0000");
            completo.put("latitud", 4.65);
            completo.put("longitud", -74.06);
            completo.put("categorias", List.of("CASUAL", "FORMAL", "DEPORTIVA", "VINTAGE", "ACCESORIOS"));
            json(HttpMethod.POST, "/api/v1/stores", tienda, completo).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categorias.length()").value(5));
        }

        @Test
        @DisplayName("3.2 y 3.3: página con content, page, size, totalPages, totalElements, first y last; size de 1 a 100")
        void paginaVerificacion() throws Exception {
            registrarAlmacen();
            JsonNode p = cuerpo(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?page=0&size=20", admin, null)
                    .andExpect(status().isOk()));
            campos(p, "content", "page", "size", "totalPages", "totalElements", "first", "last");
            assertThat(p.get("first").asBoolean()).isTrue();
            assertThat(p.get("last").asBoolean()).isTrue();
            assertThat(p.get("size").asInt()).isEqualTo(20);
            json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=APROBADO", admin, null)
                    .andExpect(jsonPath("$.totalElements").value(0));
            error(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?size=101", admin, null), 400, null);
            error(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?size=0", admin, null), 400, null);
            error(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?page=-1", admin, null), 400, null);
            error(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=OTRO", admin, null), 400, null);
        }

        @Test
        @DisplayName("3.3 y B-3.3: el almacén trae todos los campos del contrato, incluido el representante")
        void camposAlmacen() throws Exception {
            registrarAlmacen();
            json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin, Map.of("decision", "APROBADO"));
            JsonNode a = cuerpo(json(HttpMethod.GET, "/api/v1/admin/stores/" + NIT + "/verification", admin, null)
                    .andExpect(status().isOk()));
            campos(a, "nit", "nombreComercial", "descripcion", "telefono", "correoContacto", "latitud", "longitud",
                    "reputacion", "categorias", "estadoVerificacion", "legadoSinVerificacion", "fechaDecisionVerificacion",
                    "administradorDecisionId", "motivoVerificacion", "urlRutPdf", "representante");
            assertThat(a.get("representante").asText()).contains("20");
            assertThat(a.get("fechaDecisionVerificacion").asText()).matches(FECHA_UTC);
        }

        @Test
        @DisplayName("3.4: motivo de más de 1000 caracteres → 400 en el campo motivo")
        void motivoLargo() throws Exception {
            registrarAlmacen();
            campo(json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin,
                    Map.of("decision", "RECHAZADO", "motivo", "m".repeat(1001))),
                    "motivo", "El motivo debe tener entre 10 y 1000 caracteres");
        }

        @Test
        @DisplayName("4.1 y B-4.2: subir el certificado PDF devuelve la URL; solo Admin y solo PDF")
        void certificado() throws Exception {
            subir("/api/v1/admin/foundations/certificates", admin, "c.pdf", "application/pdf", "%PDF-1.7".getBytes())
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.url").isNotEmpty());
            error(subir("/api/v1/admin/foundations/certificates", tienda, "c.pdf", "application/pdf", "%PDF-1.7".getBytes()),
                    403, SIN_PERMISO_FUNDACIONES);
            error(subir("/api/v1/admin/foundations/certificates", admin, "c.png", "image/png", png()), 400, null);
        }

        @Test
        @DisplayName("4.1 y 4.3: listar fundaciones devuelve una lista con todos los campos y fechaRegistro en UTC")
        void listarFundaciones() throws Exception {
            Map<String, Object> f = fundacion("Fundación Abrigo Solidario", "900215884-1", 24);
            f.put("descripcion", "Recolección de prendas");
            f.put("telefono", "+57 601 742 8899");
            f.put("causasApoyadas", "Niñez");
            f.put("horarioAtencion", "L-V 8-17");
            f.put("politicasDonacion", "Prendas limpias");
            f.put("categoriasAceptadas", List.of("SUPERIOR", "INFERIOR", "CALZADO", "VESTIDO", "ACCESORIO"));
            json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f).andExpect(status().isCreated());
            JsonNode lista = cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations", admin, null).andExpect(status().isOk()));
            assertThat(lista.isArray()).isTrue();
            JsonNode x = lista.get(0);
            campos(x, "id", "nombre", "descripcion", "direccion", "telefono", "email", "nit", "barrio", "latitud", "longitud",
                    "categoriasAceptadas", "representanteId", "urlCertificadoPersoneriaJuridica", "causasApoyadas",
                    "horarioAtencion", "politicasDonacion", "activa", "fechaRegistro");
            assertThat(x.get("categoriasAceptadas").size()).isEqualTo(5);
            assertThat(x.get("fechaRegistro").asText()).matches(FECHA_UTC);
            error(json(HttpMethod.GET, "/api/v1/admin/foundations", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("4.2: límites de los campos opcionales de la fundación, URL de 300 y categoría fuera del catálogo")
        void opcionalesFundacion() throws Exception {
            Map<String, Object> f = fundacion("Fundación Abrigo", "900215884-1", 24);
            f.put("descripcion", "d".repeat(501));
            f.put("telefono", "1".repeat(31));
            f.put("urlCertificadoPersoneriaJuridica", "https://" + "u".repeat(300));
            f.put("causasApoyadas", "c".repeat(251));
            f.put("horarioAtencion", "h".repeat(151));
            f.put("politicasDonacion", "p".repeat(501));
            ResultActions r = json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f);
            campo(r, "descripcion", "La descripción no puede superar 500 caracteres");
            campo(r, "telefono", "El teléfono no puede superar 30 caracteres");
            campo(r, "urlCertificadoPersoneriaJuridica", "La URL del certificado no puede superar 300 caracteres");
            campo(r, "causasApoyadas", "Las causas apoyadas no pueden superar 250 caracteres");
            campo(r, "horarioAtencion", "El horario de atención no puede superar 150 caracteres");
            campo(r, "politicasDonacion", "Las políticas de donación no pueden superar 500 caracteres");
            Map<String, Object> mala = fundacion("Fundación Abrigo", "900215884-1", 24);
            mala.put("categoriasAceptadas", List.of("SOMBRERO"));
            error(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, mala), 400, null);
            Map<String, Object> sinTelefono = fundacion("Fundación Abrigo", "900215884-1", 24);
            json(HttpMethod.POST, "/api/v1/admin/foundations", admin, sinTelefono).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("6.3: los eventos traen nombreFundacion y la fecha del cambio; el motivo por defecto al desactivar")
        void eventos() throws Exception {
            long id = registrarFundacion("Fundación Abrigo", "900215884-1", 24);
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", admin, null).andExpect(status().isOk());
            ArgumentCaptor<FundacionDesactivadaEvent> d = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
            verify(eventos).publicarFundacionDesactivada(d.capture());
            assertThat(d.getValue().getNombreFundacion()).isEqualTo("Fundación Abrigo");
            assertThat(d.getValue().getFundacionId()).isEqualTo(id);
            assertThat(d.getValue().getFechaDesactivacion()).isNotNull();
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/activate", admin, null).andExpect(status().isOk());
            ArgumentCaptor<FundacionReactivadaEvent> r = ArgumentCaptor.forClass(FundacionReactivadaEvent.class);
            verify(eventos).publicarFundacionReactivada(r.capture());
            assertThat(r.getValue().getEventoId()).isNotBlank().isNotEqualTo(d.getValue().getEventoId());
            assertThat(r.getValue().getNombreFundacion()).isEqualTo("Fundación Abrigo");
            assertThat(r.getValue().getRepresentanteId()).isEqualTo(24L);
            assertThat(r.getValue().getFechaReactivacion()).isNotNull();
            assertThat(r.getValue().getMotivo()).isNotBlank();
        }
    }
}
