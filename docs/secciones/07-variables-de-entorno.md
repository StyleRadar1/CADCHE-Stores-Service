[⬅ Volver al README Principal](../../README.md)

---

## 7. VARIABLES DE ENTORNO

Plantilla en [`.env.example`](../../.env.example). En Azure, los pipelines configuran estas variables como *App Settings* desde los secretos de GitHub.

| Variable | Descripción | Por defecto |
| :--- | :--- | :--- |
| `SERVER_PORT` / `PORT` | Puerto HTTP | `8140` |
| `SPRING_PROFILES_ACTIVE` | `dev`, `docker`, `qa` o `prod` | `dev` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL 16 (TLS `sslmode=require` en Azure) | `jdbc:postgresql://localhost:5432/styleradar_tiendas` |
| `JWT_SECRET_BASE64` | **Mismo secreto Base64 de ms-auth** (≥ 256 bits). Obligatorio en QA y PROD; sin él, o con un secreto de ejemplo, el servicio no arranca. | Solo dev/docker tienen un valor de ejemplo |
| `TRUST_GATEWAY_HEADERS` | Acepta `X-User-*` sin token. Solo `true` en local o con red restringida al Gateway | `false` (`true` en dev/docker) |
| `DONACIONES_INTERNAL_USERNAME` / `DONACIONES_INTERNAL_PASSWORD` | Credencial Basic de ms-segunda-vida-donaciones para `/api/v1/internal/**`. Sin contraseña esas rutas responden `503` y nunca quedan abiertas | `ms-donaciones` / — |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` / `RABBITMQ_SSL_ENABLED` | Broker | `localhost` / `5672` |
| `AZURE_STORAGE_CONNECTION_STRING` | Cadena de conexión de Azure Blob Storage (secreto `AZURE_STORAGE_CONNECTION_STRING_QA/PROD`). Sin ella, en QA/PROD las subidas responden `503` | Azurite en `docker`; memoria en `dev`/`test` |
| `AZURE_STORAGE_CONTENEDORES_PUBLICOS` | Contenedores con lectura anónima por blob (la URL devuelta abre; no se pueden listar) | `tiendas,documentos-almacenes,documentos-fundaciones` |
| `RABBITMQ_HOST` (PROD) | Sin valor por defecto: PROD nunca apunta al broker de QA (CADCHE-131) | Secreto `RABBITMQ_HOST_PROD` |
| `SWAGGER_SERVER_URL` | URL pública que muestra Swagger | Por ambiente |
| `JPA_DDL_AUTO` / `FLYWAY_ENABLED` | Validación del esquema y migraciones | `validate` / según perfil |

---

[⬅ Volver al README Principal](../../README.md)
