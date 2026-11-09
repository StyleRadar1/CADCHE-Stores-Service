# ==============================================================================
# STYLE RADAR - PRODUCTION-GRADE MULTI-STAGE DOCKERFILE
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# Estándares: OCI, CIS Benchmark, Non-Root User, JVM Container-Aware
# ==============================================================================

# ─── Etapa 1: Compilación con Maven (Build Environment) ────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1. Cachear dependencias copiando pom.xml primero
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

# 2. Copiar código fuente y compilar binario optimizado
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Etapa 2: Imagen Final Ultraligera y Segura (Runtime Environment) ─────────
FROM eclipse-temurin:17-jre-alpine

# Metadatos estándar OCI (Open Container Initiative)
LABEL org.opencontainers.image.title="StyleRadar CADCHE Stores Service" \
      org.opencontainers.image.description="Microservicio de Gestión de Tiendas y Fundaciones (Arquitectura Hexagonal)" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="StyleRadar Team CADche" \
      org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /app

# Seguridad: Crear usuario no privilegiado (No-Root User) según CIS Benchmarks
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el ejecutable JAR desde la etapa de compilación
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Variables de entorno por defecto (adaptadas a los estándares de microservicios)
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8140 \
    PORT=8140 \
    WEBSITES_PORT=8140 \
    TZ=America/Bogota

# Puerto oficial asignado al microservicio CADCHE Tiendas
EXPOSE 8140

# Healthcheck nativo para orquestadores (Docker Swarm, Kubernetes, App Service)
HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8140/actuator/health/liveness || exit 1

# Cambiar a usuario no-root
USER appuser:appgroup

# Punto de entrada con flags de JVM adaptados a contenedores y contención de memoria
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
