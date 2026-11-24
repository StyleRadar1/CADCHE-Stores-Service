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
    private static final String INVALID_DATA = "La solicitud contiene datos inválidos";
    private static final String EXPIRADO = "El token de autenticación ha expirado";
    private static final String FIRMA = "La firma criptográfica del token es inválida o ha sido alterada";
    private static final String SIN_PERMISO_FUNDACIONES =
            "Se requiere el rol ROLE_STYLERADAR_ADMIN para gestionar fundaciones aliadas";
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
    @MockBean
    private edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort eventosAlmacen;

    private final String tienda = token(20, "STORE_ADMIN");
    private final String otraTienda = token(21, "STORE_ADMIN");
    private final String admin = token(1, "STYLERADAR_ADMIN");
    private final String comprador = token(30, "BUYER");

    @BeforeEach
    void limpiar() {
        jdbc.execute("TRUNCATE almacen_fotos, administradores_almacenes, almacen_categorias, almacenes, "
                + "fundacion_categorias_aceptadas, fundaciones RESTART IDENTITY CASCADE");
        reset(eventos, eventosAlmacen);
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
        return mockMvc.perform(multipart(ruta).file(new MockMultipartFile("file", nombre, tipo, contenido))
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
                .andExpect(jsonPath("$.message").value(INVALID_DATA))
                .andExpect(jsonPath("$.errors." + campo).value(mensaje));
    }

    /** Las dos variantes del 401 del Word: token vencido y firma alterada. */
    private void fa01(HttpMethod metodo, String ruta, Object cuerpo) throws Exception {
        error(json(metodo, ruta, firmar(20, "STYLERADAR_ADMIN", Instant.now().minusSeconds(60)), cuerpo), 401, EXPIRADO);
        String alterado = admin.substring(0, admin.length() - 3) + (admin.endsWith("AAA") ? "BBB" : "AAA");
        error(json(metodo, ruta, alterado, cuerpo), 401, FIRMA);
        error(json(metodo, ruta, null, cuerpo), 401, null);
    }

    private Map<String, Object> almacen(String nit) {
        Map<String, Object> a = new HashMap<>();
        a.put("nit", nit);
        a.put("tradeName", "Atelier Urbano");
        a.put("contactEmail", "contacto@atelier.co");
        a.put("categories", List.of("CASUAL"));
        a.put("rutPdfUrl", "https://blob/rut.pdf");
        return a;
    }

    private void registrarAlmacen() throws Exception {
        json(HttpMethod.POST, "/api/v1/stores", tienda, almacen(NIT)).andExpect(status().isCreated());
    }

    private Map<String, Object> fundacion(String nombre, String nit, long representante) {
        Map<String, Object> f = new HashMap<>();
        f.put("name", nombre);
        f.put("nit", nit);
        f.put("neighborhood", "Chapinero");
        f.put("address", "Calle 85 # 15-32");
        f.put("email", "contacto@fundacion.org");
        f.put("latitude", 4.67);
        f.put("longitude", -74.05);
        f.put("acceptedCategories", List.of("TOP"));
        f.put("representativeId", representante);
        f.put("legalEntityCertificateUrl", "https://blob/certificado.pdf");
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
                    Map.of("currentStock", stock, "available", disponible))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        }

        @Test
        @DisplayName("RN-03: umbral por defecto 3 y se guarda entre 0 y 50")
        void rn03() throws Exception {
            json(HttpMethod.GET, config, tienda, null).andExpect(jsonPath("$.lowStockThreshold").value(3));
            json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 50, "newArrivalDays", 7))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.lowStockThreshold").value(50));
        }

        @Test
        @DisplayName("RN-01 y RN-02: distintivo si 1 ≤ stock ≤ umbral; stock 0 o no disponible → NONE (agotado prevalece)")
        void rn01Rn02() throws Exception {
            assertThat(calcular(3, true).get("activeBadge").asText()).isEqualTo("LOW_STOCK");
            assertThat(calcular(1, true).get("lowStock").asBoolean()).isTrue();
            assertThat(calcular(4, true).get("activeBadge").asText()).isEqualTo("NONE");
            assertThat(calcular(0, true).get("activeBadge").asText()).isEqualTo("NONE");
            assertThat(calcular(2, false).get("activeBadge").asText()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, config,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7));
        }

        @Test
        @DisplayName("FA-02 y RN-04: almacén que no administra → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PUT, config, otraTienda, Map.of("lowStockThreshold", 3, "newArrivalDays", 7)),
                    403, "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/stores/811111111-1/catalog-settings", tienda,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7)),
                    404, "No se encontró ningún almacén registrado con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04, FA-05 y FA-06: umbral vacío, negativo o mayor que 50 → 400")
        void fa04a06() throws Exception {
            campo(json(HttpMethod.PUT, config, tienda, Map.of("newArrivalDays", 7)),
                    "lowStockThreshold", "El umbral de últimas unidades es obligatorio");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", -1, "newArrivalDays", 7)),
                    "lowStockThreshold", "El umbral de últimas unidades no puede ser negativo");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 51, "newArrivalDays", 7)),
                    "lowStockThreshold", "El umbral de últimas unidades no puede superar 50");
        }

        @Test
        @DisplayName("D10: available es obligatorio (sin valor por defecto)")
        void disponibleObligatorio() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("currentStock", 2)),
                    "available", "Debe indicar si la prenda está disponible");
        }

        @Test
        @DisplayName("FA-07: stock vacío o negativo → 400")
        void fa07() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("available", true)),
                    "currentStock", "El stock actual es obligatorio");
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("currentStock", -1, "available", true)),
                    "currentStock", "El stock actual no puede ser negativo");
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
                    Map.of("publishedAt", publicacion.toString(), "withdrawn", retirada))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                    .get("activeBadge").asText();
        }

        @Test
        @DisplayName("RN-03: periodo por defecto 7 días; entre 1 y 60")
        void rn03() throws Exception {
            json(HttpMethod.GET, config, tienda, null).andExpect(jsonPath("$.newArrivalDays").value(7));
            json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 3, "newArrivalDays", 60))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("RN-01 y RN-02: nueva durante el periodo; al día siguiente o retirada → NONE")
        void rn01Rn02() throws Exception {
            Instant ahora = Instant.now();
            assertThat(calcular(ahora.minus(6, ChronoUnit.DAYS), false)).isEqualTo("NEW_ARRIVAL");
            assertThat(calcular(ahora.minus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES), false)).isEqualTo("NEW_ARRIVAL");
            assertThat(calcular(ahora.minus(8, ChronoUnit.DAYS), false)).isEqualTo("NONE");
            assertThat(calcular(ahora.minus(1, ChronoUnit.DAYS), true)).isEqualTo("NONE");
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, config,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7));
        }

        @Test
        @DisplayName("FA-02 y RN-04: almacén que no administra → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/new-arrival-days", otraTienda,
                    Map.of("newArrivalDays", 14)), 403,
                    "El usuario autenticado (21) no tiene permisos para administrar el almacén con NIT " + NIT);
        }

        @Test
        @DisplayName("FA-03: almacén inexistente → 404")
        void fa03() throws Exception {
            error(json(HttpMethod.PUT, "/api/v1/stores/811111111-1/new-arrival-days", tienda,
                    Map.of("newArrivalDays", 14)), 404, "No se encontró ningún almacén registrado con el NIT: 811111111-1");
        }

        @Test
        @DisplayName("FA-04, FA-05 y FA-06: periodo vacío, menor que 1 o mayor que 60 → 400")
        void fa04a06() throws Exception {
            campo(json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 3)),
                    "newArrivalDays", "El periodo en días de nueva llegada es obligatorio");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 3, "newArrivalDays", 0)),
                    "newArrivalDays", "El periodo de nueva llegada debe ser de al menos 1 día");
            campo(json(HttpMethod.PUT, config, tienda, Map.of("lowStockThreshold", 3, "newArrivalDays", 61)),
                    "newArrivalDays", "El periodo de nueva llegada no puede superar 60 días");
            campo(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/new-arrival-days", tienda, Map.of()),
                    "newArrivalDays", "El periodo en días de nueva llegada es obligatorio");
        }

        @Test
        @DisplayName("D10: withdrawn es obligatorio (sin valor por defecto)")
        void retiradaObligatoria() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("publishedAt", Instant.now().minus(1, ChronoUnit.DAYS).toString())),
                    "withdrawn", "Debe indicar si la prenda fue retirada");
        }

        @Test
        @DisplayName("FA-07: fecha de publicación vacía o futura → 400")
        void fa07() throws Exception {
            campo(json(HttpMethod.PATCH, prenda, tienda, Map.of("withdrawn", false)),
                    "publishedAt", "La fecha de publicación es obligatoria");
            campo(json(HttpMethod.PATCH, prenda, tienda,
                    Map.of("publishedAt", Instant.now().plus(2, ChronoUnit.DAYS).toString(), "withdrawn", false)),
                    "publishedAt", "La fecha de publicación no puede ser futura");
        }
    }

    // ---------------------------------------------------------------- SR-RF-62-A

    @Nested
    @DisplayName("SR-RF-62-A Registrar almacén para verificación")
    class RegistrarAlmacen {

        @Test
        @DisplayName("Flujo básico, RN-01 y RN-04: el almacén nace PENDING, con RUT y asociado a la Tienda")
        void flujoBasico() throws Exception {
            json(HttpMethod.POST, "/api/v1/stores", tienda, almacen(NIT))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                    .andExpect(jsonPath("$.rutPdfUrl").value("https://blob/rut.pdf"))
                    .andExpect(jsonPath("$.representative").isNotEmpty());
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
                    .andExpect(jsonPath("$.errors.nit").exists());
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
            campo(r, "tradeName", "El nombre comercial es obligatorio");
            campo(r, "contactEmail", "El correo de contacto es obligatorio");
            campo(r, "categories", "Debe indicar al menos una categoría comercial");
            campo(r, "rutPdfUrl", "La URL o referencia del documento RUT en PDF es obligatoria para verificar la personería jurídica");
            Map<String, Object> malo = almacen("90012ABC");
            malo.put("tradeName", "A");
            malo.put("contactEmail", "sin-arroba");
            r = json(HttpMethod.POST, "/api/v1/stores", tienda, malo);
            campo(r, "nit", "El NIT debe tener un formato válido (ej. 900123456-1)");
            campo(r, "tradeName", "El nombre comercial debe tener entre 2 y 120 caracteres");
            campo(r, "contactEmail", "El correo de contacto debe tener un formato válido");
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
            json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=PENDING&page=0&size=20", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].nit").value(NIT))
                    .andExpect(jsonPath("$.totalElements").value(1));
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.decisionAdminId").value(1));
        }

        @Test
        @DisplayName("RN-02 y RN-05: rechazar con motivo de 10 a 1000 caracteres deja el motivo")
        void rn02Rn05() throws Exception {
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "REJECTED", "reason", "La cámara de comercio está vencida."))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                    .andExpect(jsonPath("$.verificationReason").value("La cámara de comercio está vencida."));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PATCH, verificacion, Map.of("decision", "APPROVED"));
        }

        @Test
        @DisplayName("FA-02: usuario sin rol Admin → 403")
        void fa02() throws Exception {
            error(json(HttpMethod.PATCH, verificacion, tienda, Map.of("decision", "APPROVED")),
                    403, "Se requiere el rol ROLE_STYLERADAR_ADMIN para verificar almacenes aliados");
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
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APPROVED")).andExpect(status().isOk());
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "REJECTED", "reason", "Motivo suficiente")),
                    409, "El almacén con NIT " + NIT + " ya fue decidido previamente. Estado actual: APPROVED");
        }

        @Test
        @DisplayName("FA-05 y RN-03: aprobar sin RUT → 422")
        void fa05() throws Exception {
            jdbc.update("UPDATE almacenes SET url_rut_pdf = NULL WHERE nit = ?", NIT);
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APPROVED")), 422,
                    "No es posible aprobar el almacén con NIT " + NIT + " porque no cuenta con el documento RUT adjunto en PDF");
        }

        @Test
        @DisplayName("FA-06: rechazo con motivo vacío o de menos de 10 caracteres → 400 en el campo motivo")
        void fa06() throws Exception {
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "REJECTED", "reason", "")),
                    400, "El rechazo exige un motivo no vacío")
                    .andExpect(jsonPath("$.errors.reason").value("El rechazo exige un motivo no vacío"));
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "REJECTED", "reason", "corto")),
                    400, "El motivo debe tener entre 10 y 1000 caracteres")
                    .andExpect(jsonPath("$.errors.reason").value("El motivo debe tener entre 10 y 1000 caracteres"));
        }

        @Test
        @DisplayName("FA-07: campos distintos a la decisión y el motivo → 400")
        void fa07() throws Exception {
            error(json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APPROVED", "estado", "APPROVED")),
                    400, "Campo no permitido en la decisión de verificación");
        }

        @Test
        @DisplayName("FA-08: retirar verificación de una tienda pendiente → 422; decidida vuelve a PENDING")
        void fa08() throws Exception {
            error(json(HttpMethod.POST, verificacion + "/review-request", admin, null),
                    422, "El almacén ya se encuentra pendiente de revisión");
            json(HttpMethod.PATCH, verificacion, admin, Map.of("decision", "APPROVED"));
            json(HttpMethod.POST, verificacion + "/review-request", admin, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.verificationStatus").value("PENDING"));
        }
    }

    // ---------------------------------------------------------------- SR-RF-63 y SR-RF-64

    /** Validaciones comunes de los datos de la fundación (FA-04 a FA-14 de RF-63 y FA-05 a FA-14 de RF-64). */
    private void validacionesFundacion(HttpMethod metodo, String ruta) throws Exception {
        ResultActions r = json(metodo, ruta, admin, Map.of());
        campo(r, "name", "El nombre es obligatorio");
        campo(r, "nit", "El NIT es obligatorio");
        campo(r, "neighborhood", "El barrio es obligatorio");
        campo(r, "address", "La dirección es obligatoria");
        campo(r, "email", "El email es obligatorio");
        campo(r, "latitude", "La latitud es obligatoria");
        campo(r, "longitude", "La longitud es obligatoria");
        campo(r, "acceptedCategories", "Debe aceptar al menos una categoría de prenda");
        campo(r, "representativeId", "El ID del usuario representante es obligatorio");
        Map<String, Object> malo = fundacion("Fun", "9002", 40);
        malo.put("neighborhood", "b".repeat(81));
        malo.put("address", "d".repeat(201));
        malo.put("email", "sin-arroba");
        malo.put("latitude", 91);
        malo.put("longitude", -181);
        r = json(metodo, ruta, admin, malo);
        campo(r, "name", "El nombre debe tener entre 4 y 120 caracteres");
        campo(r, "nit", "El NIT debe tener un formato válido (ej. 900215884-1)");
        campo(r, "neighborhood", "El barrio no puede superar 80 caracteres");
        campo(r, "address", "La dirección no puede superar 200 caracteres");
        campo(r, "email", "El email no tiene un formato válido");
        campo(r, "latitude", "La latitud debe estar entre -90 y 90");
        campo(r, "longitude", "La longitud debe estar entre -180 y 180");
        malo = fundacion("x".repeat(121), "900215884-1", 40);
        campo(json(metodo, ruta, admin, malo), "name", "El nombre debe tener entre 4 y 120 caracteres");
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
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.nit").value("900215884-1"))
                    .andExpect(jsonPath("$.neighborhood").value("Chapinero"))
                    .andExpect(jsonPath("$.registeredAt").isNotEmpty());
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
                    .andExpect(jsonPath("$.errors.name").exists());
            error(json(HttpMethod.POST, ruta, admin, fundacion("Otra Fundación", "900215885-1", 24)),
                    409, "El usuario con ID 24 ya se encuentra asignado como representante de otra fundación aliada")
                    .andExpect(jsonPath("$.errors.representativeId").exists());
            error(json(HttpMethod.POST, ruta, admin, fundacion("Otra Fundación", "900215884-1", 25)),
                    409, "Ya existe una fundación aliada registrada con el NIT: 900215884-1")
                    .andExpect(jsonPath("$.errors.nit").exists());
        }

        @Test
        @DisplayName("FA-04 y RN-04: sin certificado de personería jurídica → 400")
        void fa04() throws Exception {
            Map<String, Object> sinCertificado = fundacion("Fundación Abrigo", "900215884-1", 24);
            sinCertificado.remove("legalEntityCertificateUrl");
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
                    .andExpect(jsonPath("$.name").value("Fundación Segunda Editada"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.registeredAt").value(objectMapper.readTree(antes).get("registeredAt").asText()));
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
                    .andExpect(jsonPath("$.errors.representativeId").exists());
            error(json(HttpMethod.PUT, ruta, admin, fundacion("Fundación Segunda", "900215884-1", 26)),
                    409, "Ya existe una fundación aliada registrada con el NIT: 900215884-1")
                    .andExpect(jsonPath("$.errors.nit").exists());
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
                    .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
            ArgumentCaptor<FundacionDesactivadaEvent> desactivada = ArgumentCaptor.forClass(FundacionDesactivadaEvent.class);
            verify(eventos).publicarFundacionDesactivada(desactivada.capture());
            assertThat(desactivada.getValue().getEventoId()).isNotBlank();
            assertThat(desactivada.getValue().getRepresentanteId()).isEqualTo(24L);
            assertThat(desactivada.getValue().getMotivo()).isEqualTo("Desactivación institucional por administrador StyleRadar");
            json(HttpMethod.GET, "/api/v1/admin/foundations/" + id, admin, null).andExpect(status().isOk());

            mockMvc.perform(patch("/api/v1/admin/foundations/" + id + "/activate").param("reason", "Revisión superada")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
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
                    .andExpect(jsonPath("$[0].cover").value(true)).andExpect(jsonPath("$[0].position").value(1))
                    .andExpect(jsonPath("$[1].cover").value(false));
            List<Long> invertido = new ArrayList<>(ids);
            java.util.Collections.reverse(invertido);
            json(HttpMethod.PUT, fotos + "/order", tienda, Map.of("photoIds", invertido))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(invertido.get(0)))
                    .andExpect(jsonPath("$[0].cover").value(true));
            json(HttpMethod.DELETE, fotos + "/" + invertido.get(0), tienda, null).andExpect(status().isNoContent());
            json(HttpMethod.GET, fotos, tienda, null)
                    .andExpect(jsonPath("$.length()").value(5))
                    .andExpect(jsonPath("$[0].id").value(invertido.get(1)))
                    .andExpect(jsonPath("$[0].cover").value(true));
        }

        @Test
        @DisplayName("FA-01: sesión vencida o firma alterada → 401")
        void fa01() throws Exception {
            TrazabilidadRequerimientosTiendasTest.this.fa01(HttpMethod.PUT, fotos + "/order", Map.of("photoIds", List.of(1)));
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
            campo(json(HttpMethod.PUT, fotos + "/order", tienda, Map.of("photoIds", List.of(a))),
                    "photoIds", "El nuevo orden debe incluir todas las fotografías actuales del almacén");
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
            campos(e, "timestamp", "status", "error", "message", "path", "errors");
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
                    Map.of("lowStockThreshold", 3)), 401, null);
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
                    Map.of("lowStockThreshold", 5, "newArrivalDays", 14)).andExpect(status().isOk()));
            campos(c, "nit", "lowStockThreshold", "newArrivalDays", "updatedAt");
            assertThat(c.get("nit").asText()).isEqualTo(NIT);
            assertThat(c.get("updatedAt").asText()).matches(FECHA_UTC);
            json(HttpMethod.PUT, base + "/low-stock-threshold", tienda, Map.of("lowStockThreshold", 9))
                    .andExpect(status().isOk());
            json(HttpMethod.GET, base + "/catalog-settings", tienda, null)
                    .andExpect(jsonPath("$.lowStockThreshold").value(9))
                    .andExpect(jsonPath("$.newArrivalDays").value(14));
            campo(json(HttpMethod.PUT, base + "/low-stock-threshold", tienda, Map.of("lowStockThreshold", 51)),
                    "lowStockThreshold", "El umbral de últimas unidades no puede superar 50");
        }

        @Test
        @DisplayName("1.3: el distintivo trae nit, itemId, banderas, umbral, días, distintivoActivo y fecha; usa la configuración guardada")
        void distintivo() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/stores/" + NIT;
            json(HttpMethod.PUT, base + "/catalog-settings", tienda, Map.of("lowStockThreshold", 5, "newArrivalDays", 14));
            JsonNode u = cuerpo(json(HttpMethod.PATCH, base + "/items/7/low-stock", tienda,
                    Map.of("currentStock", 5, "available", true)).andExpect(status().isOk()));
            campos(u, "nit", "itemId", "lowStock", "lowStockThreshold", "newArrival", "newArrivalDays",
                    "activeBadge", "updatedAt");
            assertThat(u.get("itemId").asLong()).isEqualTo(7);
            assertThat(u.get("lowStockThreshold").asInt()).isEqualTo(5);
            assertThat(u.get("activeBadge").asText()).isEqualTo("LOW_STOCK");
            assertThat(u.get("updatedAt").asText()).matches(FECHA_UTC);
            JsonNode n = cuerpo(json(HttpMethod.PATCH, base + "/items/8/new-arrival", tienda,
                    Map.of("publishedAt", Instant.now().minus(13, ChronoUnit.DAYS).toString(), "withdrawn", false)));
            assertThat(n.get("newArrival").asBoolean()).isTrue();
            assertThat(n.get("newArrivalDays").asInt()).isEqualTo(14);
            assertThat(n.get("activeBadge").asText()).isEqualTo("NEW_ARRIVAL");
            error(json(HttpMethod.PATCH, base + "/items/7/low-stock", otraTienda,
                    Map.of("currentStock", 1, "available", true)), 403, null);
        }

        @Test
        @DisplayName("2.3: cada foto trae id, almacenNit, url, orden, portada y fechaRegistro; el Comprador puede ver la galería")
        void fotos() throws Exception {
            registrarAlmacen();
            JsonNode f = cuerpo(subir("/api/v1/stores/" + NIT + "/photos", tienda, "f.png", "image/png", png())
                    .andExpect(status().isCreated()));
            campos(f, "id", "storeNit", "url", "position", "cover", "registeredAt");
            assertThat(f.get("storeNit").asText()).isEqualTo(NIT);
            assertThat(f.get("position").asInt()).isEqualTo(1);
            assertThat(f.get("registeredAt").asText()).matches(FECHA_UTC);
            json(HttpMethod.GET, "/api/v1/stores/" + NIT + "/photos", comprador, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].cover").value(true));
            subir("/api/v1/stores/" + NIT + "/photos", tienda, "w.webp", "image/webp",
                    new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}).andExpect(status().isCreated());
            subir("/api/v1/stores/" + NIT + "/photos", tienda, "j.jpg", "image/jpeg",
                    new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("3A.2 y 3A.4: campos opcionales del almacén con sus límites y categorías fuera del catálogo")
        void opcionalesAlmacen() throws Exception {
            Map<String, Object> a = almacen(NIT);
            a.put("description", "d".repeat(501));
            a.put("phone", "1".repeat(31));
            a.put("latitude", 91);
            a.put("longitude", 181);
            ResultActions r = json(HttpMethod.POST, "/api/v1/stores", tienda, a);
            campo(r, "description", "La descripción no puede superar 500 caracteres");
            campo(r, "phone", "El teléfono no puede superar 30 caracteres");
            campo(r, "latitude", "La latitud debe estar entre -90 y 90");
            campo(r, "longitude", "La longitud debe estar entre -180 y 180");
            Map<String, Object> malaCategoria = almacen(NIT);
            malaCategoria.put("categories", List.of("ROPA_INTERIOR"));
            error(json(HttpMethod.POST, "/api/v1/stores", tienda, malaCategoria), 400, null);
            Map<String, Object> completo = almacen(NIT);
            completo.put("description", "Moda urbana");
            completo.put("phone", "+57 300 000 0000");
            completo.put("latitude", 4.65);
            completo.put("longitude", -74.06);
            completo.put("categories", List.of("CASUAL", "FORMAL", "SPORTY", "VINTAGE", "ACCESSORIES"));
            json(HttpMethod.POST, "/api/v1/stores", tienda, completo).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categories.length()").value(5));
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
            json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=APPROVED", admin, null)
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
            json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin, Map.of("decision", "APPROVED"));
            JsonNode a = cuerpo(json(HttpMethod.GET, "/api/v1/admin/stores/" + NIT + "/verification", admin, null)
                    .andExpect(status().isOk()));
            campos(a, "nit", "tradeName", "description", "phone", "contactEmail", "latitude", "longitude",
                    "rating", "categories", "verificationStatus", "legacyUnverified", "verificationDecidedAt",
                    "decisionAdminId", "verificationReason", "rutPdfUrl", "representative");
            assertThat(a.get("representative").asText()).contains("20");
            assertThat(a.get("verificationDecidedAt").asText()).matches(FECHA_UTC);
        }

        @Test
        @DisplayName("3.4: motivo de más de 1000 caracteres → 400 en el campo motivo")
        void motivoLargo() throws Exception {
            registrarAlmacen();
            campo(json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin,
                    Map.of("decision", "REJECTED", "reason", "m".repeat(1001))),
                    "reason", "El motivo debe tener entre 10 y 1000 caracteres");
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
            f.put("description", "Recolección de prendas");
            f.put("phone", "+57 601 742 8899");
            f.put("supportedCauses", "Niñez");
            f.put("openingHours", "L-V 8-17");
            f.put("donationPolicies", "Prendas limpias");
            f.put("acceptedCategories", List.of("TOP", "BOTTOM", "FOOTWEAR", "DRESS", "ACCESSORY"));
            json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f).andExpect(status().isCreated());
            JsonNode lista = cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations", admin, null).andExpect(status().isOk()));
            assertThat(lista.isArray()).isTrue();
            JsonNode x = lista.get(0);
            campos(x, "id", "name", "description", "address", "phone", "email", "nit", "neighborhood", "latitude", "longitude",
                    "acceptedCategories", "representativeId", "legalEntityCertificateUrl", "supportedCauses",
                    "openingHours", "donationPolicies", "active", "registeredAt");
            assertThat(x.get("acceptedCategories").size()).isEqualTo(5);
            assertThat(x.get("registeredAt").asText()).matches(FECHA_UTC);
            error(json(HttpMethod.GET, "/api/v1/admin/foundations", tienda, null), 403, SIN_PERMISO_FUNDACIONES);
        }

        @Test
        @DisplayName("4.2: límites de los campos opcionales de la fundación, URL de 300 y categoría fuera del catálogo")
        void opcionalesFundacion() throws Exception {
            Map<String, Object> f = fundacion("Fundación Abrigo", "900215884-1", 24);
            f.put("description", "d".repeat(501));
            f.put("phone", "1".repeat(31));
            f.put("legalEntityCertificateUrl", "https://" + "u".repeat(300));
            f.put("supportedCauses", "c".repeat(251));
            f.put("openingHours", "h".repeat(151));
            f.put("donationPolicies", "p".repeat(501));
            ResultActions r = json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f);
            campo(r, "description", "La descripción no puede superar 500 caracteres");
            campo(r, "phone", "El teléfono no puede superar 30 caracteres");
            campo(r, "legalEntityCertificateUrl", "La URL del certificado no puede superar 300 caracteres");
            campo(r, "supportedCauses", "Las causas apoyadas no pueden superar 250 caracteres");
            campo(r, "openingHours", "El horario de atención no puede superar 150 caracteres");
            campo(r, "donationPolicies", "Las políticas de donación no pueden superar 500 caracteres");
            Map<String, Object> mala = fundacion("Fundación Abrigo", "900215884-1", 24);
            mala.put("acceptedCategories", List.of("SOMBRERO"));
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

    // ---------------------------------------------------------------- Análisis de requerimientos v2 (17/11/2026)

    @Nested
    @DisplayName("Análisis v2: rutas, campos, valores y códigos en inglés; mensajes en español")
    class AnalisisV2 {

        private JsonNode cuerpo(ResultActions r) throws Exception {
            return objectMapper.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Almacén: campos en inglés y verificationStatus PENDING; categorías SPORTY y ACCESSORIES")
        void almacenV2() throws Exception {
            Map<String, Object> a = almacen(NIT);
            a.put("categories", List.of("SPORTY", "ACCESSORIES"));
            JsonNode b = cuerpo(json(HttpMethod.POST, "/api/v1/stores", tienda, a).andExpect(status().isCreated()));
            assertThat(b.fieldNames()).toIterable().contains("nit", "tradeName", "description", "phone", "contactEmail",
                    "latitude", "longitude", "rating", "categories", "verificationStatus", "verificationDecidedAt",
                    "decisionAdminId", "verificationReason", "rutPdfUrl", "legacyUnverified")
                    .doesNotContain("nombreComercial", "estadoVerificacion", "urlRutPdf", "categorias");
            assertThat(b.get("verificationStatus").asText()).isEqualTo("PENDING");
            assertThat(b.get("categories").toString()).contains("SPORTY", "ACCESSORIES");
        }

        @Test
        @DisplayName("Verificación: decision APPROVED/REJECTED con reason; listado con ?status=")
        void verificacion() throws Exception {
            registrarAlmacen();
            json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=PENDING", admin, null)
                    .andExpect(jsonPath("$.totalElements").value(1));
            JsonNode b = cuerpo(json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin,
                    Map.of("decision", "REJECTED", "reason", "La cámara de comercio está vencida.")).andExpect(status().isOk()));
            assertThat(b.get("verificationStatus").asText()).isEqualTo("REJECTED");
            assertThat(b.get("verificationReason").asText()).isEqualTo("La cámara de comercio está vencida.");
        }

        @Test
        @DisplayName("Distintivos: lowStockThreshold, newArrivalDays, activeBadge LOW_STOCK/NEW_ARRIVAL/NONE")
        void distintivos() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/stores/" + NIT;
            JsonNode c = cuerpo(json(HttpMethod.GET, base + "/catalog-settings", tienda, null));
            assertThat(c.fieldNames()).toIterable().containsExactlyInAnyOrder("nit", "lowStockThreshold", "newArrivalDays", "updatedAt");
            JsonNode u = cuerpo(json(HttpMethod.PATCH, base + "/items/7/low-stock", tienda, Map.of("currentStock", 2, "available", true)));
            assertThat(u.fieldNames()).toIterable().containsExactlyInAnyOrder("nit", "itemId", "lowStock", "lowStockThreshold",
                    "newArrival", "newArrivalDays", "activeBadge", "updatedAt");
            assertThat(u.get("activeBadge").asText()).isEqualTo("LOW_STOCK");
            assertThat(cuerpo(json(HttpMethod.PATCH, base + "/items/7/low-stock", tienda, Map.of("currentStock", 0, "available", true)))
                    .get("activeBadge").asText()).isEqualTo("NONE");
            assertThat(cuerpo(json(HttpMethod.PATCH, base + "/items/8/new-arrival", tienda,
                    Map.of("publishedAt", Instant.now().minus(1, ChronoUnit.DAYS).toString(), "withdrawn", false)))
                    .get("activeBadge").asText()).isEqualTo("NEW_ARRIVAL");
        }

        @Test
        @DisplayName("Fundación: campos en inglés, acceptedCategories TOP/BOTTOM/FOOTWEAR/DRESS/ACCESSORY")
        void fundacionV2() throws Exception {
            Map<String, Object> f = fundacion("Fundación Abrigo", "900215884-1", 24);
            f.put("acceptedCategories", List.of("TOP", "BOTTOM", "FOOTWEAR", "DRESS", "ACCESSORY"));
            JsonNode b = cuerpo(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f).andExpect(status().isCreated()));
            assertThat(b.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "name", "nit", "neighborhood", "description",
                    "address", "phone", "email", "latitude", "longitude", "acceptedCategories", "representativeId",
                    "legalEntityCertificateUrl", "supportedCauses", "openingHours", "donationPolicies", "active", "registeredAt");
            assertThat(b.get("acceptedCategories")).hasSize(5);
            Map<String, Object> mala = fundacion("Fundación Dos", "900215885-1", 25);
            mala.put("acceptedCategories", List.of("SUPERIOR"));
            error(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, mala), 400, null);
        }

        @Test
        @DisplayName("Fotos: campo file en multipart y salida id, storeNit, url, position, cover, registeredAt")
        void fotosV2() throws Exception {
            registrarAlmacen();
            JsonNode b = cuerpo(mockMvc.perform(multipart("/api/v1/stores/" + NIT + "/photos")
                    .file(new MockMultipartFile("file", "f.png", "image/png", png()))
                    .header("Authorization", "Bearer " + tienda)).andExpect(status().isCreated()));
            assertThat(b.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "storeNit", "url", "position", "cover", "registeredAt");
            assertThat(b.get("cover").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("ProblemDetail: type /errors/{code}, code y errors en inglés; detail y mensajes por campo en español")
        void problemDetail() throws Exception {
            JsonNode e = cuerpo(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, Map.of()));
            assertThat(e.get("type").asText()).isEqualTo("https://api.styleradar.com/errors/invalid-data");
            assertThat(e.get("code").asText()).isEqualTo("INVALID_DATA");
            assertThat(e.get("detail").asText()).isEqualTo(INVALID_DATA);
            assertThat(e.get("errors").get("name").asText()).isEqualTo("El nombre es obligatorio");
            assertThat(e.get("errors").get("neighborhood").asText()).isEqualTo("El barrio es obligatorio");
            assertThat(e.get("errors").get("representativeId").asText()).isEqualTo("El ID del usuario representante es obligatorio");
            assertThat(e.fieldNames()).toIterable().doesNotContain("codigo", "validationErrors");
            assertThat(cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations/999999", admin, null)).get("code").asText())
                    .isEqualTo("RESOURCE_NOT_FOUND");
            assertThat(cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations", tienda, null)).get("code").asText())
                    .isEqualTo("ACCESS_DENIED");
            assertThat(cuerpo(json(HttpMethod.GET, "/api/v1/admin/foundations", null, null)).get("code").asText())
                    .isEqualTo("UNAUTHENTICATED");
        }

        @Test
        @DisplayName("Anexo: GET /api/v1/foundations es público, sin parámetros obligatorios y solo con fundaciones activas")
        void fundacionesPublicas() throws Exception {
            long lejana = registrarFundacion("Fundación Zeta", "900215884-1", 24);
            registrarFundacion("Fundación Alfa", "900215885-1", 25);
            long inactiva = registrarFundacion("Fundación Beta", "900215886-1", 26);
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + inactiva + "/deactivate", admin, null).andExpect(status().isOk());
            jdbc.update("UPDATE fundaciones SET latitud = 10.0, longitud = -70.0 WHERE id = ?", lejana);
            json(HttpMethod.GET, "/api/v1/foundations", null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Fundación Alfa"))
                    .andExpect(jsonPath("$[1].name").value("Fundación Zeta"));
            json(HttpMethod.GET, "/api/v1/foundations?latitude=10.0&longitude=-70.0", null, null)
                    .andExpect(jsonPath("$[0].name").value("Fundación Zeta"));
            json(HttpMethod.GET, "/api/v1/foundations?type=DRESS", null, null).andExpect(jsonPath("$.length()").value(0));
            error(json(HttpMethod.GET, "/api/v1/foundations?latitude=4.6", null, null), 400,
                    "Debe enviar la latitud y la longitud juntas");
        }

        @Test
        @DisplayName("Roles en inglés en el JWT (STORE_ADMIN, STYLERADAR_ADMIN) y compatibilidad con tokens anteriores")
        void roles() throws Exception {
            json(HttpMethod.GET, "/api/v1/admin/foundations", token(1, "STYLERADAR_ADMIN"), null).andExpect(status().isOk());
            json(HttpMethod.GET, "/api/v1/admin/foundations", token(1, "ADMIN_STYLERADAR"), null).andExpect(status().isOk());
            json(HttpMethod.POST, "/api/v1/stores", token(40, "ADMIN_ALMACEN"), almacen("800111222-3")).andExpect(status().isCreated());
            error(json(HttpMethod.GET, "/api/v1/admin/foundations", token(30, "BUYER"), null), 403,
                    "Se requiere el rol ROLE_STYLERADAR_ADMIN para gestionar fundaciones aliadas");
        }
    }

    @Nested
    @DisplayName("Análisis v2: código de error de cada flujo alterno")
    class CodigosV2 {

        private void codigo(ResultActions r, int http, String code, String detail) throws Exception {
            r.andExpect(status().is(http)).andExpect(jsonPath("$.code").value(code));
            if (detail != null) {
                r.andExpect(jsonPath("$.detail").value(detail));
            }
        }

        @Test
        @DisplayName("401 UNAUTHENTICATED con los tres mensajes del documento")
        void noAutenticado() throws Exception {
            codigo(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/catalog-settings", null,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7)), 401, "UNAUTHENTICATED",
                    "Se requiere un token de acceso válido para esta operación");
            codigo(json(HttpMethod.GET, "/api/v1/admin/foundations", firmar(1, "STYLERADAR_ADMIN", Instant.now().minusSeconds(60)), null),
                    401, "UNAUTHENTICATED", EXPIRADO);
        }

        @Test
        @DisplayName("SR-RF-59/60: ACCESS_DENIED, RESOURCE_NOT_FOUND e INVALID_DATA")
        void distintivos() throws Exception {
            registrarAlmacen();
            codigo(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/catalog-settings", otraTienda,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7)), 403, "ACCESS_DENIED", null);
            codigo(json(HttpMethod.PUT, "/api/v1/stores/811111111-1/catalog-settings", tienda,
                    Map.of("lowStockThreshold", 3, "newArrivalDays", 7)), 404, "RESOURCE_NOT_FOUND", null);
            codigo(json(HttpMethod.PUT, "/api/v1/stores/" + NIT + "/catalog-settings", tienda,
                    Map.of("lowStockThreshold", 51, "newArrivalDays", 7)), 400, "INVALID_DATA", INVALID_DATA);
        }

        @Test
        @DisplayName("SR-RF-62: CONFLICT, UNPROCESSABLE_OPERATION y BUSINESS_RULE_VIOLATION (motivo y campo no permitido)")
        void verificacion() throws Exception {
            registrarAlmacen();
            String v = "/api/v1/admin/stores/" + NIT + "/verification";
            codigo(json(HttpMethod.PATCH, v, admin, Map.of("decision", "REJECTED", "reason", "")), 400,
                    "BUSINESS_RULE_VIOLATION", "El rechazo exige un motivo no vacío");
            codigo(json(HttpMethod.PATCH, v, admin, Map.of("decision", "REJECTED", "reason", "corto")), 400,
                    "BUSINESS_RULE_VIOLATION", "El motivo debe tener entre 10 y 1000 caracteres");
            codigo(json(HttpMethod.PATCH, v, admin, Map.of("decision", "APPROVED", "status", "APPROVED")), 400,
                    "BUSINESS_RULE_VIOLATION", "Campo no permitido en la decisión de verificación");
            codigo(json(HttpMethod.POST, v + "/review-request", admin, null), 422, "UNPROCESSABLE_OPERATION",
                    "El almacén ya se encuentra pendiente de revisión");
            json(HttpMethod.PATCH, v, admin, Map.of("decision", "APPROVED"));
            codigo(json(HttpMethod.PATCH, v, admin, Map.of("decision", "APPROVED")), 409, "CONFLICT", null);
        }

        @Test
        @DisplayName("SR-RF-63: CONFLICT y BUSINESS_RULE_VIOLATION sin certificado")
        void fundaciones() throws Exception {
            Map<String, Object> f = fundacion("Fundación Abrigo", "900215884-1", 24);
            f.remove("legalEntityCertificateUrl");
            codigo(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, f), 400, "BUSINESS_RULE_VIOLATION",
                    "El certificado de personería jurídica en PDF es obligatorio para registrar la fundación aliada");
            registrarFundacion("Fundación Abrigo", "900215884-1", 24);
            codigo(json(HttpMethod.POST, "/api/v1/admin/foundations", admin, fundacion("Fundación Abrigo", "900215885-1", 25)),
                    409, "CONFLICT", null);
        }

        @Test
        @DisplayName("SR-RF-74: BUSINESS_RULE_VIOLATION, FILE_TOO_LARGE, UNPROCESSABLE_OPERATION e INVALID_DATA al reordenar")
        void fotos() throws Exception {
            registrarAlmacen();
            String fotos = "/api/v1/stores/" + NIT + "/photos";
            codigo(subir(fotos, tienda, "v.png", "image/png", new byte[0]), 400, "BUSINESS_RULE_VIOLATION",
                    "Se requiere adjuntar un archivo de imagen válido");
            codigo(subir(fotos, tienda, "d.pdf", "application/pdf", "%PDF".getBytes()), 400, "BUSINESS_RULE_VIOLATION", null);
            byte[] grande = new byte[6 * 1024 * 1024 + 1];
            grande[0] = (byte) 0x89;
            codigo(subir(fotos, tienda, "g.png", "image/png", grande), 413, "FILE_TOO_LARGE", "La imagen supera el tamaño máximo de 6 MB");
            codigo(json(HttpMethod.DELETE, fotos + "/999999", tienda, null), 400, "BUSINESS_RULE_VIOLATION",
                    "La fotografía no pertenece a la galería del almacén");
            long a = objectMapper.readTree(subir(fotos, tienda, "a.png", "image/png", png()).andReturn().getResponse()
                    .getContentAsString()).get("id").asLong();
            subir(fotos, tienda, "b.png", "image/png", png());
            ResultActions r = json(HttpMethod.PUT, fotos + "/order", tienda, Map.of("photoIds", List.of(a)));
            codigo(r, 400, "INVALID_DATA", INVALID_DATA);
            r.andExpect(jsonPath("$.errors.photoIds").value("El nuevo orden debe incluir todas las fotografías actuales del almacén"));
            for (int i = 0; i < 4; i++) {
                subir(fotos, tienda, "c" + i + ".png", "image/png", png());
            }
            codigo(subir(fotos, tienda, "x.png", "image/png", png()), 422, "UNPROCESSABLE_OPERATION", null);
        }
    }

    // ---------------------------------------------------------------- Análisis v6 (24/11/2026)

    @Nested
    @DisplayName("Análisis v6: SR-RF-116 a SR-RF-120 y salidas precisadas de SR-RF-59, 60, 62, 65 y 118")
    class AnalisisV6 {

        private static final String PARA_OPERACION = "Se requiere el rol ROLE_STYLERADAR_ADMIN para esta operación";

        private JsonNode cuerpo(ResultActions r) throws Exception {
            return objectMapper.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }

        private void aprobar(String nit) throws Exception {
            json(HttpMethod.PATCH, "/api/v1/admin/stores/" + nit + "/verification", admin, Map.of("decision", "APPROVED"))
                    .andExpect(status().isOk());
        }

        private ResultActions problema(ResultActions r, int http, String code, String mensaje) throws Exception {
            return error(r, http, mensaje).andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.type").value("https://api.styleradar.com/errors/"
                            + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-')));
        }

        // ---- SR-RF-119 Eliminar almacén aliado

        @Test
        @DisplayName("SR-RF-119 flujo básico, RN-03 y RN-05: 204; se borran fotos, categorías y administradores y se publica store.deleted")
        void rf119FlujoBasico() throws Exception {
            registrarAlmacen();
            subir("/api/v1/stores/" + NIT + "/photos", tienda, "f.png", "image/png", png()).andExpect(status().isCreated());
            json(HttpMethod.DELETE, "/api/v1/admin/stores/" + NIT, admin, null).andExpect(status().isNoContent())
                    .andExpect(r -> assertThat(r.getResponse().getContentAsString()).isEmpty());
            problema(json(HttpMethod.GET, "/api/v1/stores/" + NIT, null, null), 404, "RESOURCE_NOT_FOUND",
                    "No se encontró ningún almacén registrado con el NIT: " + NIT);
            for (String tabla : List.of("almacen_fotos", "administradores_almacenes", "almacen_categorias")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla + " WHERE almacen_nit = ?", Integer.class, NIT))
                        .as(tabla).isZero();
            }
            ArgumentCaptor<edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent> ev =
                    ArgumentCaptor.forClass(edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent.class);
            verify(eventosAlmacen).publicarAlmacenEliminado(ev.capture());
            assertThat(ev.getValue().getAlmacenNit()).isEqualTo(NIT);
            assertThat(java.util.UUID.fromString(ev.getValue().getEventoId()).version()).isEqualTo(4);
            assertThat(ev.getValue().getFechaEliminacion()).isNotNull();
            assertThat(edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.RabbitMQAlmacenEventPublisherAdapter
                    .class).isNotNull();
        }

        @Test
        @DisplayName("SR-RF-119 FA-01, FA-02 y FA-03: 401 sin token, 403 sin rol de administrador y 404 si no existe")
        void rf119Errores() throws Exception {
            registrarAlmacen();
            String ruta = "/api/v1/admin/stores/" + NIT;
            problema(json(HttpMethod.DELETE, ruta, null, null), 401, "UNAUTHENTICATED",
                    "Se requiere un token de acceso válido para esta operación");
            problema(json(HttpMethod.DELETE, ruta, tienda, null), 403, "ACCESS_DENIED", PARA_OPERACION);
            problema(json(HttpMethod.DELETE, "/api/v1/admin/stores/900999999-9", admin, null), 404, "RESOURCE_NOT_FOUND",
                    "No se encontró ningún almacén registrado con el NIT: 900999999-9");
            verify(eventosAlmacen, never()).publicarAlmacenEliminado(org.mockito.ArgumentMatchers.any());
        }

        // ---- SR-RF-120 Eliminar fundación aliada

        @Test
        @DisplayName("SR-RF-120 flujo básico y RN-05: 204; la fundación deja de existir y se publica foundation.deleted")
        void rf120FlujoBasico() throws Exception {
            long id = registrarFundacion("Fundación Eliminar", "900111222-3", 40);
            json(HttpMethod.DELETE, "/api/v1/admin/foundations/" + id, admin, null).andExpect(status().isNoContent());
            problema(json(HttpMethod.GET, "/api/v1/admin/foundations/" + id, admin, null), 404, "RESOURCE_NOT_FOUND",
                    "No existe una fundación aliada con ID: " + id);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fundacion_categorias_aceptadas WHERE fundacion_id = ?",
                    Integer.class, id)).isZero();
            ArgumentCaptor<edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent> ev =
                    ArgumentCaptor.forClass(edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent.class);
            verify(eventos).publicarFundacionEliminada(ev.capture());
            assertThat(ev.getValue().getFundacionId()).isEqualTo(id);
            assertThat(ev.getValue().getEventoId()).isNotBlank();
        }

        @Test
        @DisplayName("SR-RF-120 FA-01, FA-02 y FA-03: 401, 403 «para esta operación» y 404 «No existe una fundación aliada con ID»")
        void rf120Errores() throws Exception {
            long id = registrarFundacion("Fundación Errores", "900111333-4", 41);
            String ruta = "/api/v1/admin/foundations/" + id;
            problema(json(HttpMethod.DELETE, ruta, null, null), 401, "UNAUTHENTICATED",
                    "Se requiere un token de acceso válido para esta operación");
            problema(json(HttpMethod.DELETE, ruta, comprador, null), 403, "ACCESS_DENIED", PARA_OPERACION);
            problema(json(HttpMethod.DELETE, "/api/v1/admin/foundations/999999", admin, null), 404, "RESOURCE_NOT_FOUND",
                    "No existe una fundación aliada con ID: 999999");
        }

        // ---- SR-RF-118 Asociar administrador de almacén

        @Test
        @DisplayName("SR-RF-118: asociar devuelve id, userId, storeNit, assignedById y assignedAt; FA-02 a FA-05")
        void rf118() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/admin/stores/" + NIT + "/administrators";
            JsonNode a = cuerpo(json(HttpMethod.PUT, base + "/55", admin, null).andExpect(status().isOk()));
            assertThat(a.get("id").asLong()).isPositive();
            assertThat(a.get("userId").asLong()).isEqualTo(55);
            assertThat(a.get("storeNit").asText()).isEqualTo(NIT);
            assertThat(a.get("assignedById").asLong()).isEqualTo(1);
            assertThat(a.get("assignedAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
            problema(json(HttpMethod.PUT, base + "/56", tienda, null), 403, "ACCESS_DENIED", PARA_OPERACION);
            problema(json(HttpMethod.PUT, base + "/0", admin, null), 400, "BUSINESS_RULE_VIOLATION",
                    "El identificador del usuario administrador debe ser mayor a cero");
            problema(json(HttpMethod.PUT, "/api/v1/admin/stores/900999999-9/administrators/57", admin, null), 404,
                    "RESOURCE_NOT_FOUND", "No se encontró ningún almacén registrado con el NIT: 900999999-9");
            json(HttpMethod.GET, base, tienda, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
            problema(json(HttpMethod.GET, base, otraTienda, null), 403, "ACCESS_DENIED", null);
        }

        // ---- SR-RF-117 Consultar fundaciones aliadas activas

        @Test
        @DisplayName("SR-RF-117: pública, solo activas, sin representante, certificado ni fecha de registro; orden por nombre")
        void rf117Salida() throws Exception {
            registrarFundacion("Fundación Zeta", "900111444-5", 42);
            long inactiva = registrarFundacion("Fundación Alfa Inactiva", "900111555-6", 43);
            registrarFundacion("Fundación Beta", "900111666-7", 44);
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + inactiva + "/deactivate", admin, null).andExpect(status().isOk());
            JsonNode lista = cuerpo(json(HttpMethod.GET, "/api/v1/foundations", null, null).andExpect(status().isOk()));
            assertThat(lista).hasSize(2);
            assertThat(lista.get(0).get("name").asText()).isEqualTo("Fundación Beta");
            List<String> campos = new java.util.ArrayList<>();
            lista.get(0).fieldNames().forEachRemaining(campos::add);
            assertThat(campos).contains("id", "name", "nit", "neighborhood", "address", "email", "latitude", "longitude",
                    "acceptedCategories", "active").doesNotContain("representativeId", "legalEntityCertificateUrl", "registeredAt");
            assertThat(lista.get(0).get("active").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("SR-RF-117 FA-01 a FA-03: latitud, longitud o tipo inválidos → 400 INVALID_DATA")
        void rf117Errores() throws Exception {
            problema(json(HttpMethod.GET, "/api/v1/foundations?latitude=95&longitude=1", null, null), 400, "INVALID_DATA",
                    INVALID_DATA).andExpect(jsonPath("$.errors.latitude").value("La latitud debe estar entre -90 y 90"));
            problema(json(HttpMethod.GET, "/api/v1/foundations?latitude=1&longitude=200", null, null), 400, "INVALID_DATA",
                    INVALID_DATA).andExpect(jsonPath("$.errors.longitude").value("La longitud debe estar entre -180 y 180"));
            problema(json(HttpMethod.GET, "/api/v1/foundations?type=ZAPATO", null, null), 400, "INVALID_DATA", INVALID_DATA);
        }

        // ---- SR-RF-116 Consultar almacenes aliados

        @Test
        @DisplayName("SR-RF-116: lista paginada solo de aprobados con content, page, size, totalElements, totalPages, first y last")
        void rf116Salida() throws Exception {
            registrarAlmacen();
            json(HttpMethod.POST, "/api/v1/stores", otraTienda, almacen("900654321-2")).andExpect(status().isCreated());
            aprobar(NIT);
            JsonNode p = cuerpo(json(HttpMethod.GET, "/api/v1/stores?page=0&size=10", null, null).andExpect(status().isOk()));
            for (String c : List.of("content", "page", "size", "totalElements", "totalPages", "first", "last")) {
                assertThat(p.has(c)).as(c).isTrue();
            }
            assertThat(p.get("totalElements").asInt()).isEqualTo(1);
            assertThat(p.get("content").get(0).get("nit").asText()).isEqualTo(NIT);
            assertThat(p.get("first").asBoolean()).isTrue();
            List<String> campos = new java.util.ArrayList<>();
            p.get("content").get(0).fieldNames().forEachRemaining(campos::add);
            assertThat(campos).contains("nit", "tradeName", "categories")
                    .doesNotContain("rutPdfUrl", "representative", "administrators");
            json(HttpMethod.GET, "/api/v1/stores/" + NIT, null, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeName").value("Atelier Urbano"));
        }

        @Test
        @DisplayName("SR-RF-116 FA-01 y FA-02: categoría, página o tamaño inválidos → 400; NIT inexistente → 404")
        void rf116Errores() throws Exception {
            for (String q : List.of("category=ZAPATOS", "page=-1", "size=0", "size=101")) {
                problema(json(HttpMethod.GET, "/api/v1/stores?" + q, null, null), 400, "INVALID_DATA", INVALID_DATA);
            }
            problema(json(HttpMethod.GET, "/api/v1/stores/900888777-1", null, null), 404, "RESOURCE_NOT_FOUND",
                    "No se encontró ningún almacén registrado con el NIT: 900888777-1");
        }

        // ---- SR-RF-62, 59, 60 y 65 (detalles precisados en la v6)

        @Test
        @DisplayName("SR-RF-62: listado paginado; motivo de menos de 10 caracteres → 400; FA-08 retirar verificación de una pendiente → 422")
        void rf62() throws Exception {
            registrarAlmacen();
            JsonNode p = cuerpo(json(HttpMethod.GET, "/api/v1/admin/stores/verifications?status=PENDING&page=0&size=5", admin, null)
                    .andExpect(status().isOk()));
            assertThat(p.get("content").get(0).get("verificationStatus").asText()).isEqualTo("PENDING");
            assertThat(p.get("size").asInt()).isEqualTo(5);
            problema(json(HttpMethod.PATCH, "/api/v1/admin/stores/" + NIT + "/verification", admin,
                    Map.of("decision", "REJECTED", "reason", "Corto")), 400, "BUSINESS_RULE_VIOLATION",
                    "El motivo debe tener entre 10 y 1000 caracteres");
            problema(json(HttpMethod.POST, "/api/v1/admin/stores/" + NIT + "/verification/review-request", admin, null), 422,
                    "UNPROCESSABLE_OPERATION", "El almacén ya se encuentra pendiente de revisión");
        }

        @Test
        @DisplayName("SR-RF-59 y SR-RF-60: alternativas ?currentStock= y ?arrivalDate= sin cuerpo JSON")
        void rf59Rf60Alternativas() throws Exception {
            registrarAlmacen();
            String base = "/api/v1/stores/" + NIT + "/items/7";
            json(HttpMethod.PATCH, base + "/low-stock?currentStock=2", tienda, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.lowStock").value(true)).andExpect(jsonPath("$.activeBadge").value("LOW_STOCK"))
                    .andExpect(jsonPath("$.itemId").value(7));
            json(HttpMethod.PATCH, base + "/new-arrival?arrivalDate=" + Instant.now().minus(1, ChronoUnit.DAYS), tienda, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.newArrival").value(true))
                    .andExpect(jsonPath("$.activeBadge").value("NEW_ARRIVAL"));
        }

        @Test
        @DisplayName("SR-RF-65: motivo de más de 500 caracteres → 400; sin motivo se usa el texto por defecto")
        void rf65Motivo() throws Exception {
            long id = registrarFundacion("Fundación Motivo", "900111777-8", 45);
            problema(json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate?reason=" + "x".repeat(501),
                    admin, null), 400, "BUSINESS_RULE_VIOLATION", "El motivo no puede superar 500 caracteres");
            json(HttpMethod.PATCH, "/api/v1/admin/foundations/" + id + "/deactivate", admin, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
            ArgumentCaptor<edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent> ev =
                    ArgumentCaptor.forClass(edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent.class);
            verify(eventos).publicarFundacionDesactivada(ev.capture());
            assertThat(ev.getValue().getMotivo()).isEqualTo("Desactivación institucional por administrador StyleRadar");
        }
    }
}
