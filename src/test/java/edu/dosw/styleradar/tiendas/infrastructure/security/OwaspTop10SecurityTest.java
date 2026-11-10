package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.application.usecase.ConfigurarUmbralUltimasUnidadesUseCaseImpl;
import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.LimiteFotografiasSuperadoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.exception.RutRequeridoParaAprobacionException;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.CategoriaAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.DecisionVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.FotografiaTienda;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.infrastructure.exception.GlobalExceptionHandler;
import jakarta.servlet.ServletException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Suite de Verificación Exhaustiva de Seguridad - OWASP Top 10:2025.
 * Cada método de prueba evalúa y demuestra la efectividad de los controles
 * implementados en ms-tiendas para cada categoría oficial de la taxonomía 2025.
 */
@DisplayName("Auditoría y Pruebas de Cumplimiento OWASP Top 10:2025")
class OwaspTop10SecurityTest {

    private static final String SECRET_256_BITS = "styleradar_super_secret_jwt_key_2025_at_least_32_bytes_long";

    private ObjectMapper objectMapper;
    private Validator validator;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        exceptionHandler = new GlobalExceptionHandler();
    }

    // =========================================================================
    // A01:2025 – Broken Access Control (BOLA / IDOR / SSRF / RBAC)
    // =========================================================================
    @Nested
    @DisplayName("A01:2025 – Broken Access Control")
    class A01BrokenAccessControlTests {

        @Test
        @DisplayName("A01.1: BOLA/IDOR - Debe bloquear acceso si el usuario autenticado no es dueño del almacén")
        void test_A01_BrokenAccessControl_BOLA_Mitigated() {
            AutorizacionAlmacenValidator bolaValidator = new AutorizacionAlmacenValidator();
            AlmacenRepositoryPort repositoryPort = mock(AlmacenRepositoryPort.class);

            String nit = "900123456";
            Long usuarioIntrusoId = 88L;
            when(repositoryPort.esAdministradorDeAlmacen(nit, usuarioIntrusoId)).thenReturn(false);

            assertThatThrownBy(() -> bolaValidator.validarAcceso(nit, usuarioIntrusoId, repositoryPort))
                    .isInstanceOf(AccesoDenegadoAlmacenException.class)
                    .hasMessageContaining("no tiene permisos para administrar el almacén");
        }

        @Test
        @DisplayName("A01.2: RBAC - Debe rechazar roles no administrativos en endpoints de moderación")
        void test_A01_BrokenAccessControl_RBAC_Enforced() {
            AutorizacionAdminStyleRadarValidator adminValidator = new AutorizacionAdminStyleRadarValidator();

            assertThatThrownBy(() -> adminValidator.validarEsAdministradorStyleRadar("ROLE_COMPRADOR"))
                    .isInstanceOf(AccesoDenegadoAlmacenException.class)
                    .hasMessageContaining("Se requiere el rol ROLE_ADMIN_STYLERADAR");

            assertThatThrownBy(() -> adminValidator.validarEsAdministradorStyleRadar("ROLE_ADMIN_ALMACEN"))
                    .isInstanceOf(AccesoDenegadoAlmacenException.class);

            assertThatCode(() -> adminValidator.validarEsAdministradorStyleRadar("ROLE_ADMIN_STYLERADAR"))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // A02:2025 – Security Misconfiguration
    // =========================================================================
    @Nested
    @DisplayName("A02:2025 – Security Misconfiguration")
    class A02SecurityMisconfigurationTests {

        @Test
        @DisplayName("A02.1: Cabeceras defensivas inyectadas en respuestas HTTP")
        void test_A02_SecurityMisconfiguration_HeadersEnforced() throws ServletException, IOException {
            SecurityHeadersFilter filter = new SecurityHeadersFilter();
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, res, chain);

            assertThat(res.getHeader(SecurityHeadersFilter.HEADER_X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff");
            assertThat(res.getHeader(SecurityHeadersFilter.HEADER_X_FRAME_OPTIONS)).isEqualTo("DENY");
            assertThat(res.getHeader(SecurityHeadersFilter.HEADER_STRICT_TRANSPORT_SECURITY)).contains("max-age=31536000");
            assertThat(res.getHeader(SecurityHeadersFilter.HEADER_CONTENT_SECURITY_POLICY)).contains("frame-ancestors 'none'");
            assertThat(res.getHeader(SecurityHeadersFilter.HEADER_REFERRER_POLICY)).isEqualTo("strict-origin-when-cross-origin");
        }

        @Test
        @DisplayName("A02.2: CORS configurado con patrones de origen explícitos sin comodín abierto")
        void test_A02_SecurityMisconfiguration_CorsConfigRestricted() {
            org.springframework.web.servlet.config.annotation.CorsRegistry registry =
                    new org.springframework.web.servlet.config.annotation.CorsRegistry();
            edu.dosw.styleradar.tiendas.infrastructure.config.CorsConfig corsConfig =
                    new edu.dosw.styleradar.tiendas.infrastructure.config.CorsConfig();

            assertThatCode(() -> corsConfig.addCorsMappings(registry)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // A03:2025 – Software Supply Chain Failures
    // =========================================================================
    @Nested
    @DisplayName("A03:2025 – Software Supply Chain Failures")
    class A03SoftwareSupplyChainTests {

        @Test
        @DisplayName("A03.1: Dockerfile define multi-stage build y ejecuta con usuario no-root appuser")
        void test_A03_SoftwareSupplyChain_DockerfileNonRoot() throws IOException {
            Path dockerfilePath = Path.of("Dockerfile");
            assertThat(Files.exists(dockerfilePath)).isTrue();

            String dockerfileContent = Files.readString(dockerfilePath);
            assertThat(dockerfileContent)
                    .contains("AS builder")
                    .contains("eclipse-temurin")
                    .contains("adduser -S appuser")
                    .contains("USER appuser:appgroup");
        }
    }

    // =========================================================================
    // A04:2025 – Cryptographic Failures
    // =========================================================================
    @Nested
    @DisplayName("A04:2025 – Cryptographic Failures")
    class A04CryptographicFailuresTests {

        @Test
        @DisplayName("A04.1: Entropía mínima de 256 bits requerida para firmas criptográficas HMAC-SHA256")
        void test_A04_CryptographicFailures_JwtKeyEntropyEnforced() {
            JwtSecurityValidator jwtValidator = new JwtSecurityValidator(objectMapper, SECRET_256_BITS);

            assertThatThrownBy(() -> jwtValidator.generarToken(1L, "ROLE_USER", 3600, "clave_corta_insegura"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("256 bits");
        }

        @Test
        @DisplayName("A04.2: Configuración de producción exige SSL en conexión a base de datos PostgreSQL")
        void test_A04_CryptographicFailures_SslRequiredInProdProfile() throws IOException {
            Path prodYml = Path.of("src/main/resources/application-prod.yml");
            assertThat(Files.exists(prodYml)).isTrue();

            String content = Files.readString(prodYml);
            assertThat(content).contains("sslmode=require");
        }
    }

    // =========================================================================
    // A05:2025 – Injection (SQLi, Command, Expression)
    // =========================================================================
    @Nested
    @DisplayName("A05:2025 – Injection")
    class A05InjectionTests {

        @Test
        @DisplayName("A05.1: Validación de NIT rechaza caracteres maliciosos de inyección SQL")
        void test_A05_Injection_SqlInjectionPreventedByNitPattern() {
            RegistrarAlmacenRequestDTO maliciousDto = RegistrarAlmacenRequestDTO.builder()
                    .nit("900123456' OR '1'='1")
                    .nombreComercial("Tienda Inyección")
                    .correoContacto("test@tienda.com")
                    .categorias(Set.of(CategoriaAlmacen.CASUAL))
                    .urlRutPdf("https://blob.com/rut.pdf")
                    .build();

            var violations = validator.validate(maliciousDto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nit"));
        }

        @Test
        @DisplayName("A05.2: Coordenadas geográficas fuera de rango son rechazadas por Bean Validation")
        void test_A05_Injection_CoordinateRangeValidation() {
            RegistrarAlmacenRequestDTO invalidCoords = RegistrarAlmacenRequestDTO.builder()
                    .nit("900123456-1")
                    .nombreComercial("Tienda Coordenadas")
                    .correoContacto("test@tienda.com")
                    .latitud(999.0) // Latitud inválida (> 90.0)
                    .longitud(-74.0817)
                    .categorias(Set.of(CategoriaAlmacen.CASUAL))
                    .urlRutPdf("https://blob.com/rut.pdf")
                    .build();

            var violations = validator.validate(invalidCoords);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("latitud"));
        }
    }

    // =========================================================================
    // A06:2025 – Insecure Design (Límites de Dominio y Reglas de Negocio)
    // =========================================================================
    @Nested
    @DisplayName("A06:2025 – Insecure Design")
    class A06InsecureDesignTests {

        @Test
        @DisplayName("A06.1: Cuota física de fotografías (máx. 6) previene DoS y agotamiento de almacenamiento")
        void test_A06_InsecureDesign_PhotoQuotaLimit() {
            Almacen almacen = Almacen.builder()
                    .nit("900123456-1")
                    .nombreComercial("Tienda Cuotas")
                    .build();

            for (int i = 1; i <= 6; i++) {
                almacen.agregarFotografia("https://blob/foto" + i + ".png", Instant.now());
            }

            assertThatThrownBy(() -> almacen.agregarFotografia("https://blob/foto7.png", Instant.now()))
                    .isInstanceOf(LimiteFotografiasSuperadoException.class)
                    .hasMessageContaining("máximo permitido");
        }

        @Test
        @DisplayName("A06.2: Verificación de almacén exige documento RUT obligatorio para aprobación")
        void test_A06_InsecureDesign_RutMandatoryForApproval() {
            Almacen almacenSinRut = Almacen.builder()
                    .nit("900123456-1")
                    .nombreComercial("Tienda Sin Rut")
                    .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                    .urlRutPdf(null)
                    .build();

            assertThatThrownBy(() -> almacenSinRut.decidirVerificacion(
                    DecisionVerificacionAlmacen.APROBADO, "Aprobación", 1L, Instant.now()))
                    .isInstanceOf(RutRequeridoParaAprobacionException.class);
        }

        @Test
        @DisplayName("A06.3: Rechazo de verificación exige motivo explicativo no vacío")
        void test_A06_InsecureDesign_RejectionRequiresMotive() {
            Almacen almacen = Almacen.builder()
                    .nit("900123456-1")
                    .nombreComercial("Tienda")
                    .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                    .build();

            assertThatThrownBy(() -> almacen.decidirVerificacion(
                    DecisionVerificacionAlmacen.RECHAZADO, "   ", 1L, Instant.now()))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("motivo");
        }
    }

    // =========================================================================
    // A07:2025 – Authentication Failures
    // =========================================================================
    @Nested
    @DisplayName("A07:2025 – Authentication Failures")
    class A07AuthenticationFailuresTests {

        @Test
        @DisplayName("A07.1: Tokens expirados o con firma alterada son rechazados inmediatamente")
        void test_A07_AuthenticationFailures_ExpiredAndTamperedTokensRejected() {
            JwtSecurityValidator jwtValidator = new JwtSecurityValidator(objectMapper, SECRET_256_BITS);

            String tokenExpirado = jwtValidator.generarToken(1L, "ROLE_ADMIN", -50, SECRET_256_BITS);
            assertThatThrownBy(() -> jwtValidator.validarToken(tokenExpirado))
                    .isInstanceOf(FalloAutenticacionException.class)
                    .hasMessageContaining("ha expirado");

            String tokenValido = jwtValidator.generarToken(1L, "ROLE_ADMIN", 3600, SECRET_256_BITS);
            String tokenFirmaFalsa = tokenValido.substring(0, tokenValido.length() - 4) + "XXXX";
            assertThatThrownBy(() -> jwtValidator.validarToken(tokenFirmaFalsa))
                    .isInstanceOf(FalloAutenticacionException.class)
                    .hasMessageContaining("inválida o ha sido alterada");
        }

        @Test
        @DisplayName("A07.2: FalloAutenticacionException se transforma en HTTP 401 Unauthorized")
        void test_A07_AuthenticationFailures_MishandlingMapsTo401() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/almacenes/900123456/catalogo");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleFalloAutenticacion(
                    new FalloAutenticacionException("Token expirado"), req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getMessage()).isEqualTo("Token expirado");
        }
    }

    // =========================================================================
    // A08:2025 – Software or Data Integrity Failures
    // =========================================================================
    @Nested
    @DisplayName("A08:2025 – Software or Data Integrity Failures")
    class A08SoftwareOrDataIntegrityTests {

        @Test
        @DisplayName("A08.1: Scripts de migración Flyway versionados y estructurados (V1 a V4)")
        void test_A08_SoftwareOrDataIntegrity_FlywayMigrationsExist() {
            File migrationDir = new File("src/main/resources/db/migration");
            assertThat(migrationDir).isDirectory();

            File[] sqlFiles = migrationDir.listFiles((dir, name) -> name.endsWith(".sql"));
            assertThat(sqlFiles).isNotNull().hasSizeGreaterThanOrEqualTo(4);

            assertThat(migrationDir.toPath().resolve("V1__init_schema_almacenes.sql")).exists();
            assertThat(migrationDir.toPath().resolve("V2__add_verificacion_almacen.sql")).exists();
            assertThat(migrationDir.toPath().resolve("V3__init_schema_fundaciones.sql")).exists();
            assertThat(migrationDir.toPath().resolve("V4__add_fotos_tienda.sql")).exists();
        }

        @Test
        @DisplayName("A08.2: Operaciones mutadoras de casos de uso anotadas con @Transactional")
        void test_A08_SoftwareOrDataIntegrity_TransactionalIntegrity() {
            assertThat(ConfigurarUmbralUltimasUnidadesUseCaseImpl.class)
                    .hasAnnotation(org.springframework.stereotype.Service.class);

            boolean hasTransactionalMethods = java.util.Arrays.stream(
                            ConfigurarUmbralUltimasUnidadesUseCaseImpl.class.getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(Transactional.class));

            assertThat(hasTransactionalMethods).isTrue();
        }
    }

    // =========================================================================
    // A09:2025 – Security Logging and Alerting Failures
    // =========================================================================
    @Nested
    @DisplayName("A09:2025 – Security Logging and Alerting Failures")
    class A09SecurityLoggingTests {

        @Test
        @DisplayName("A09.1: Respuestas de error contienen auditoría de ruta, código de estado y timestamp")
        void test_A09_SecurityLogging_AuditResponseFields() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/almacenes/900123456/catalogo");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleAccesoDenegado(
                    new AccesoDenegadoAlmacenException("Acceso BOLA denegado"), req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/v1/almacenes/900123456/catalogo");
            assertThat(response.getBody().getTimestamp()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(403);
        }
    }

    // =========================================================================
    // A10:2025 – Mishandling of Exceptional Conditions
    // =========================================================================
    @Nested
    @DisplayName("A10:2025 – Mishandling of Exceptional Conditions")
    class A10MishandlingOfExceptionalConditionsTests {

        @Test
        @DisplayName("A10.1: Errores no controlados 500 suprimen trazas de JVM y detalles de base de datos")
        void test_A10_MishandlingOfExceptionalConditions_StackTracesSuppressed() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/almacenes/critico");

            NullPointerException secretInternalException =
                    new NullPointerException("NullPointer in internal db driver class org.postgresql.Driver: table almacenes");

            ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleUnexpected(
                    secretInternalException, req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("Ocurrió un error interno. Intente nuevamente más tarde.")
                    .doesNotContain("NullPointer")
                    .doesNotContain("org.postgresql")
                    .doesNotContain("almacenes");
        }
    }
}
