# ==============================================================================
# STYLE RADAR - DOCKERFILE MULTIETAPA (Semana 10 · DOSW · ECI - Guía Turtwig)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================

# ─── Etapa 1: Compilar con Maven ───────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar pom.xml primero para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Etapa 2: Imagen final ligera ──────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar solo el JAR de la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Variables de entorno con valores por defecto (se sobreescriben en Compose/K8s/Cloud)
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8140
ENV PORT=8140
ENV WEBSITES_PORT=8140

EXPOSE 8140

# Arrancar la aplicación
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
