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

echo "[1/3] Compilando JAR con Maven..."
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

  echo "[2/3] Configurando runtime nativo Java 17 (Microsoft OpenJDK)..."
  az webapp config set \
    --resource-group "$RG" \
    --name "$APP" \
    --linux-fx-version "JAVA|17-java17" \
    --output none

  echo "[3/3] Subiendo y desplegando artefacto $JAR_PATH..."
  az webapp deploy \
    --resource-group "$RG" \
    --name "$APP" \
    --src-path "$JAR_PATH" \
    --type jar \
    --output none

  echo "Iniciando servicio..."
  az webapp start --resource-group "$RG" --name "$APP" --output none

  echo ""
  echo ">> Estado del servicio:"
  az webapp show --resource-group "$RG" --name "$APP" --query "{Nombre:name, Estado:state, Host:defaultHostName}" -o table

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
echo "  ¡DESPLIEGUE COMPLETADO CON ÉXITO!"
echo "=========================================================="
