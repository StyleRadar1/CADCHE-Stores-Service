#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE DIRECTO DE APLICACIÓN Y SWAGGER EN AZURE APP SERVICE
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================

set -euo pipefail

TARGET="${1:-all}" # qa | prod | all

echo "=========================================================="
echo "  STYLE RADAR - COMPILACIÓN Y DESPLIEGUE DE MS-TIENDAS"
echo "=========================================================="

JAR_PATH="target/ms-tiendas-1.0.0-SNAPSHOT.jar"

# 1. Compilar solo si no existe o se requiere
if [ ! -f "$JAR_PATH" ]; then
  echo "[1/4] Compilando artefacto ejecutable con Maven..."
  mvn clean package -DskipTests
else
  echo "[1/4] Artefacto $JAR_PATH detectado listo para despliegue."
fi

deploy_to() {
  local ENV_NAME="$1"
  local RG="rg-styleradar-${ENV_NAME}"
  local APP="styleradar-tiendas-${ENV_NAME}"

  echo ""
  echo "=========================================================="
  echo "  Desplegando en Ambiente: ${ENV_NAME^^} ($APP)"
  echo "=========================================================="

  echo "[2/5] Habilitando permisos SCM y configurando Java 21 LTS..."
  # Habilitar permisos SCM si Azure los bloqueó por defecto
  az resource update \
    --resource-group "$RG" \
    --name scm \
    --resource-type basicPublishingCredentialsPolicies \
    --parent "sites/$APP" \
    --set properties.allow=true \
    --output none 2>/dev/null || true

  # Fijar runtime oficial Java 21 LTS
  az webapp config set \
    --resource-group "$RG" \
    --name "$APP" \
    --linux-fx-version "JAVA|21-java21" \
    --output none

  echo "[3/5] Arrancando Web App para activar motor de despliegue Kudu..."
  az webapp start --resource-group "$RG" --name "$APP" --output none

  echo "[4/5] Configurando variables de entorno y puerto :8140..."
  az webapp config appsettings set \
    --resource-group "$RG" \
    --name "$APP" \
    --settings \
      WEBSITES_PORT=8140 \
      PORT=8140 \
      JAVA_OPTS="-Dserver.port=8140 -Djava.security.egd=file:/dev/./urandom" \
      SPRING_PROFILES_ACTIVE="$ENV_NAME" \
    --output none

  # Breve pausa para asegurar que el endpoint SCM esté activo
  sleep 8

  echo "[5/5] Subiendo y desplegando artefacto $JAR_PATH en Azure..."
  az webapp deploy \
    --resource-group "$RG" \
    --name "$APP" \
    --src-path "$JAR_PATH" \
    --type jar \
    --restart true \
    --output none

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
