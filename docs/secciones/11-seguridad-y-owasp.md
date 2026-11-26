[⬅ Volver al README Principal](../../README.md)

---

## 11. SEGURIDAD Y ANÁLISIS OWASP TOP 10:2025

* **Documento técnico completo de seguridad:** [`../security/SR_CADCHE_Tiendas_Seguridad_y_OWASP_2025.md`](../security/SR_CADCHE_Tiendas_Seguridad_y_OWASP_2025.md)

<div align="center">
  <img src="https://img.shields.io/badge/OWASP%20Top%2010:2025-10%20%2F%2010%20MITIGADO-success?style=for-the-badge&logo=owasp&logoColor=white" height="26" />&nbsp;
  <img src="https://img.shields.io/badge/BOLA%20%2F%20IDOR-SR--RNF--01%20COMPLIANT-blueviolet?style=for-the-badge&logo=shield" height="26" />&nbsp;
  <img src="https://img.shields.io/badge/SECURITY-ASVS%20Level%202%20ALIGNED-informational?style=for-the-badge" height="26" />&nbsp;
  <img src="https://img.shields.io/badge/SONARQUBE-0%20VULNERABILITIES-brightgreen?style=for-the-badge&logo=sonarqube&logoColor=white" height="26" />
</div>

<br />

En cumplimiento de los requerimientos no funcionales de la plataforma StyleRadar y los estándares de la cátedra de arquitectura de software, se realizó una auditoría y modelado de seguridad exhaustivo de `ms-tiendas` frente a la taxonomía oficial de **OWASP Top 10:2025** para servicios web y APIs. 

A continuación se detalla la matriz de evaluación y el desglose técnico de controles implementados por categoría.

---

### 11.1 Resumen Ejecutivo y Matriz Consolidada de Cumplimiento

| Identificador OWASP | Categoría Oficial (OWASP Top 10:2025) | Severidad | ¿Aplica? | Estado | Controles Técnicos y Acciones Implementadas en `ms-tiendas` |
| :---: | :--- | :---: | :---: | :---: | :--- |
| **A01:2025** | **Broken Access Control** *(inc. BOLA / IDOR / SSRF)* | Crítica | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Validación de propiedad BOLA (`SR-RNF-01`) mediante `AutorizacionAlmacenValidator` contrastando claim `userId` de JWT vs NIT. RBAC con roles `STYLERADAR_ADMIN` y `STORE_ADMIN`. URLs de Blob fijas sin SSRF. |
| **A02:2025** | **Security Misconfiguration** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Configuración CORS en `CorsConfig`, cabeceras defensivas (`HSTS`, `CSP`, `X-Frame-Options`, `X-Content-Type-Options`) inyectadas en `SecurityHeadersFilter`, y Actuator limitado en `application.yml` (`health`, `info`, `metrics`). |
| **A03:2025** | **Software Supply Chain Failures** | Alta | **SÍ** | <span style="color:#D97706;font-weight:700">PARCIAL</span> | Análisis estático continuo en pipeline GitHub Actions (`ci-qa.yml`) con **SpotBugs (0 bugs)** y calidad en SonarQube Cloud. Imagen Docker mínima `eclipse-temurin:21-jre-alpine` que copia el JAR ya validado en CI (sin Maven dentro del build) y ejecuta como usuario no-root (`appuser`). **Dependencias (nov-2026):** auditoría contra la base OSV/GitHub; se parchearon Tomcat `10.1.59`, Spring Framework `6.1.21`, Netty `4.1.137.Final`, Jackson `2.18.11`, Logback `1.5.34`, Log4j API `2.25.5`, commons-lang3 `3.18.0`, amqp-client `5.38.0` y spring-retry `2.0.13`, driver PostgreSQL `42.7.12`. **Pendiente:** Spring MVC, Spring Security, Spring Data, Spring AMQP y Micrometer de la línea Spring Boot 3.2 no tienen parche público; se cierran solo subiendo Spring Boot a una línea con soporte. |
| **A04:2025** | **Cryptographic Failures** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Cifrado obligatorio en tránsito mediante TLS 1.3/HTTPS, PostgreSQL en Azure con `sslmode=require`, tokens JWT firmados con HMAC-SHA256 (clave de 256 bits en variable de entorno) y cifrado en reposo AES-256 en Azure Blob Storage. |
| **A05:2025** | **Injection** *(SQLi, Command, Express.)* | Crítica | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Persistencia 100% parametrizada con Spring Data JPA y `PreparedStatement` (cero SQL concatenado). Validación sintáctica Bean Validation (`@Pattern`, `@DecimalMin`, `@NotBlank`) en DTOs. Cero ejecución de comandos de SO. |
| **A06:2025** | **Insecure Design** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Límites de cuotas en dominio: máx. 6 fotos por vitrina (`Almacen.MAXIMO_FOTOGRAFIAS = 6`), límites multipart (`6 MB` por archivo / `7 MB` por petición), validación MIME (`MIMES_IMAGEN_PERMITIDOS`) e invariantes que exigen RUT PDF para aprobación y motivo para rechazo. |
| **A07:2025** | **Authentication Failures** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Validación HMAC-SHA256 stateless en `JwtAuthFilter` y `JwtSecurityValidator` sobre firma y expiración (`exp`) de JWT emitidos por `ms-auth`. Retorno estandarizado `401 Unauthorized` sin fuga de trazas internas. |
| **A08:2025** | **Software or Data Integrity Failures** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Deserializador Jackson endurecido sin tipado polimórfico arbitrario. Integridad relacional ACID con llaves foráneas en PostgreSQL 16 y migraciones DDL versionadas en Flyway (`V1` a `V4`). |
| **A09:2025** | **Security Logging and Alerting Failures** | Media | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Logging estructurado mediante SLF4J/Logback auditando decisiones de verificación y bloqueos 403 BOLA. Enmascaramiento de datos confidenciales y exportación de logs hacia App Service / Azure Monitor. |
| **A10:2025** | **Mishandling of Exceptional Conditions** | Alta | **SÍ** | <span style="color:#16A34A;font-weight:700">PASS (Mitigado)</span> | Manejador centralizado `GlobalExceptionHandler` devolviendo `ErrorResponseDTO` controlado (y `application/problem+json` en `JwtAuthFilter` 401). Supresión absoluta de stack traces en errores 500 y captura resiliente de `AmqpException` en `RabbitMQFundacionEventPublisherAdapter`. |

---


---

[⬅ Volver al README Principal](../../README.md)
