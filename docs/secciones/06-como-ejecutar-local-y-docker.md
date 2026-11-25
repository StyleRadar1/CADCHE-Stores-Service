[⬅ Volver al README Principal](../../README.md)

---

## 6. CÓMO EJECUTAR LOCAL Y CON DOCKER

### Local con Maven (perfil `dev`, PostgreSQL 16 en Docker)

```bash
git clone https://github.com/StyleRadar1/CADCHE-Stores-Service.git
cd CADCHE-Stores-Service
docker compose up -d tiendas-db tiendas-blob tiendas-rabbitmq
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Flyway aplica las migraciones al arrancar. En `dev`, `TRUST_GATEWAY_HEADERS=true` permite probar en Swagger con `X-User-Id` y `X-User-Roles`, o con un token de ms-auth en `dev`, que usa el mismo secreto de ejemplo.

### Stack completo con Docker Compose

```bash
docker compose up -d --build   # ms-tiendas :8140 · PostgreSQL 16 :5432 · Azurite :10000 · RabbitMQ :5672/:15672
curl http://localhost:8140/actuator/health
```

---

[⬅ Volver al README Principal](../../README.md)
