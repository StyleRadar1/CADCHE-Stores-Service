#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE DE APLICACIÓN Y SWAGGER EN AZURE APP SERVICE
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================

set -euo pipefail

TARGET="${1:-all}" # qa | prod | all

echo "=========================================================="
echo "  STYLE RADAR - COMPILACIÓN Y DESPLIEGUE DE MS-TIENDAS"
echo "=========================================================="

echo "[1/4] Compilando artefacto ejecutable con Maven..."
mvn clean package -DskipTests

JAR_PATH="target/ms-tiendas-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
  echo "Error: No se encontró el artefacto $JAR_PATH"
  exit 1
fi

deploy_to() {
  local ENV_NAME="$1"
  local RG="rg-styleradar-${ENV_NAME}"
  local APP="styleradar-tiendas-${ENV_NAME}"

  echo ""
  echo "=========================================================="
  echo "  Desplegando en Ambiente: ${ENV_NAME^^} ($APP)"
  echo "=========================================================="

  echo "[2/4] Configurando runtime nativo Java 17 y puerto oficial :8140..."
  az webapp config set \
    --resource-group "$RG" \
    --name "$APP" \
    --linux-fx-version "JAVA|17-java17" \
    --output none

  az webapp config appsettings set \
    --resource-group "$RG" \
    --name "$APP" \
    --settings \
      WEBSITES_PORT=8140 \
      PORT=8140 \
      JAVA_OPTS="-Dserver.port=8140 -Djava.security.egd=file:/dev/./urandom" \
      SPRING_PROFILES_ACTIVE="$ENV_NAME" \
    --output none

  echo "[3/4] Subiendo y desplegando artefacto $JAR_PATH en Azure..."
  az webapp deploy \
    --resource-group "$RG" \
    --name "$APP" \
    --src-path "$JAR_PATH" \
    --type jar \
    --output none

  echo "[4/4] Iniciando servicio..."
  az webapp start --resource-group "$RG" --name "$APP" --output none

  echo ""
  echo ">> Estado del servicio:"
  az webapp show --resource-group "$RG" --name "$APP" --query "{Nombre:name, Estado:state, Host:defaultHostName}" -o table

  echo ""
  echo ">> Esperando inicialización de Spring Boot y migraciones Flyway..."
  URL_DOCS="https://${APP}.azurewebsites.net/v3/api-docs"
  for i in $(seq 1 15); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$URL_DOCS" || true)
    if [ "$STATUS" = "200" ]; then
      echo ">> ¡Swagger OpenAPI respondiendo HTTP 200 OK en intento $i!"
      break
    fi
    echo "Intento $i: HTTP $STATUS (iniciando contexto de Spring Boot...)"
    sleep 8
  done

  echo ""
  echo ">> Swagger UI en Vivo: https://${APP}.azurewebsites.net/swagger-ui/index.html"
  echo ">> OpenAPI JSON Spec:  https://${APP}.azurewebsites.net/v3/api-docs"
  echo ">> Actuator Health:    https://${APP}.azurewebsites.net/actuator/health"
  echo "=========================================================="
}

if [ "$TARGET" = "qa" ] || [ "$TARGET" = "all" ]; then
  deploy_to "qa"
fi

if [ "$TARGET" = "prod" ] || [ "$TARGET" = "all" ]; then
  deploy_to "prod"
fi

echo ""
echo "=========================================================="
echo "  ¡DESPLIEGUE FINALIZADO EXITOSAMENTE!"
echo "=========================================================="
