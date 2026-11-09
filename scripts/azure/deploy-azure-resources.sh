#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (TIER F1 / GRATIS Y AISLAMIENTO TOTAL)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# REGLAS ARQUITECTÓNICAS Y DE COSTOS:
# 1. Base de datos desplegada en cada ambiente. QA y PROD NUNCA comparten BD ni servidor.
# 2. App Service Plan configurado en Tier F1 (Free Tier = $0.00 USD).
# 3. Azure PostgreSQL Flexible Server configurado en Tier Gratuito (Standard_B1ms, 32GB = $0.00 USD).
# 4. Azure Blob Storage en Standard_LRS (consumo < $1.00 USD al mes).
# ==============================================================================

set -euo pipefail

ENVIRONMENT="${1:-qa}" # Opciones: qa | prod | all
LOCATION="${2:-${LOCATION:-centralus}}"
ADMIN_USER="${ADMIN_USER:-styleradaradmin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-StyleRadarPass2026!}"
APP_SERVICE_SKU="${APP_SERVICE_SKU:-F1}" # F1 = 100% Gratis ($0 USD), o B1 Basic

deploy_environment() {
  local ENV_NAME="$1"
  local RG="rg-styleradar-${ENV_NAME}"
  local PG_SERVER="styleradar-pg-${ENV_NAME}"
  local APP_NAME="styleradar-tiendas-${ENV_NAME}"
  local PLAN_NAME="plan-styleradar-${ENV_NAME}"
  local STORAGE_NAME="styleradarblob${ENV_NAME}"

  echo ""
  echo "=========================================================="
  echo "  Desplegando Ambiente Exclusivo: ${ENV_NAME^^} (Tier F1 / Free)"
  echo "=========================================================="
  echo "Resource Group:    $RG"
  echo "App Service Tier:  $APP_SERVICE_SKU (Free Tier: \$0.00 USD)"
  echo "PostgreSQL Server: $PG_SERVER (Free Tier Standard_B1ms, 32GB: \$0.00 USD)"
  echo "Base de Datos:     styleradar_tiendas (Exclusiva para $ENV_NAME, no compartida)"
  echo "Web App Service:   $APP_NAME (Puerto Oficial :8140)"
  echo "Blob Storage:      $STORAGE_NAME (Standard_LRS: < \$1.00 USD/mes)"
  echo "=========================================================="

  # 1. Resource Group
  echo "[1/6] Creando Resource Group '$RG' en '$LOCATION'..."
  az group create --name "$RG" --location "$LOCATION" --output none

  # 2. Servidor PostgreSQL Flexible Server Dedicado e Independiente (Free Tier Standard_B1ms, 32GB)
  echo "[2/6] Aprovisionando Servidor PostgreSQL Flexible Server Gratuito..."
  az postgres flexible-server create \
    --resource-group "$RG" \
    --name "$PG_SERVER" \
    --location "$LOCATION" \
    --admin-user "$ADMIN_USER" \
    --admin-password "$ADMIN_PASSWORD" \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --storage-size 32 \
    --version 16 \
    --public-access 0.0.0.0 \
    --yes \
    --output none

  # 3. Base de Datos propia del ambiente (QA y PROD no comparten)
  echo "[3/6] Creando Base de Datos 'styleradar_tiendas' en $PG_SERVER..."
  az postgres flexible-server db create \
    --resource-group "$RG" \
    --server-name "$PG_SERVER" \
    --database-name styleradar_tiendas \
    --output none

  # Firewall para servicios de Azure
  az postgres flexible-server firewall-rule create \
    --resource-group "$RG" \
    --server-name "$PG_SERVER" \
    --rule-name AllowAllAzureServices \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0 \
    --output none

  # 4. Azure Blob Storage Dedicado para este ambiente (< $1.00 USD/mes)
  echo "[4/6] Aprovisionando Azure Blob Storage '$STORAGE_NAME'..."
  az storage account create \
    --resource-group "$RG" \
    --name "$STORAGE_NAME" \
    --location "$LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --allow-blob-public-access true \
    --output none

  STORAGE_KEY=$(az storage account keys list --resource-group "$RG" --account-name "$STORAGE_NAME" --query "[0].value" -o tsv)

  az storage container create \
    --account-name "$STORAGE_NAME" \
    --account-key "$STORAGE_KEY" \
    --name tiendas \
    --public-access blob \
    --output none

  # 5. App Service Plan en Tier F1 (Free - $0.00 USD) y Web App Dedicada (Puerto 8140)
  echo "[5/6] Creando App Service Plan (Tier $APP_SERVICE_SKU Gratuito) y Web App..."
  az appservice plan create \
    --name "$PLAN_NAME" \
    --resource-group "$RG" \
    --is-linux \
    --sku "$APP_SERVICE_SKU" \
    --location "$LOCATION" \
    --output none

  az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_NAME" \
    --deployment-container-image-name "eclipse-temurin:17-jre" \
    --output none

  # 6. Configurar Variables de Entorno del Ambiente
  echo "[6/6] Configurando App Settings en $APP_NAME (Puerto 8140)..."
  DB_URL="jdbc:postgresql://${PG_SERVER}.postgres.database.azure.com:5432/styleradar_tiendas?sslmode=require"
  BLOB_ENDPOINT="https://${STORAGE_NAME}.blob.core.windows.net"

  az webapp config appsettings set \
    --name "$APP_NAME" \
    --resource-group "$RG" \
    --settings \
      WEBSITES_PORT=8140 \
      PORT=8140 \
      SPRING_PROFILES_ACTIVE="$ENV_NAME" \
      DB_URL="$DB_URL" \
      DB_USERNAME="$ADMIN_USER" \
      DB_PASSWORD="$ADMIN_PASSWORD" \
      AZURE_STORAGE_BLOB_ENDPOINT="$BLOB_ENDPOINT" \
      SWAGGER_SERVER_URL="https://${APP_NAME}.azurewebsites.net" \
    --output none

  echo "=========================================================="
  echo "  ¡AMBIENTE ${ENV_NAME^^} DESPLEGADO EXITOSAMENTE!"
  echo "=========================================================="
  echo "App Service Tier:    $APP_SERVICE_SKU (Free: \$0.00 USD)"
  echo "PostgreSQL Tier:     Standard_B1ms (Free Tier: \$0.00 USD)"
  echo "Blob Storage:        $STORAGE_NAME (Standard_LRS: < \$1.00 USD/mes)"
  echo "Swagger UI en Vivo:  https://${APP_NAME}.azurewebsites.net/swagger-ui/index.html"
  echo "OpenAPI JSON Spec:   https://${APP_NAME}.azurewebsites.net/v3/api-docs"
  echo "Health Check:        https://${APP_NAME}.azurewebsites.net/actuator/health"
  echo "PostgreSQL Server:   ${PG_SERVER}.postgres.database.azure.com:5432"
  echo "Base de Datos:       styleradar_tiendas (Totalmente aislada para $ENV_NAME)"
  echo "Blob Endpoint:       $BLOB_ENDPOINT"
  echo "=========================================================="
}

if [ "$ENVIRONMENT" = "qa" ]; then
  deploy_environment "qa"
elif [ "$ENVIRONMENT" = "prod" ]; then
  deploy_environment "prod"
elif [ "$ENVIRONMENT" = "all" ]; then
  deploy_environment "qa"
  deploy_environment "prod"
else
  echo "Uso: $0 [qa|prod|all]"
  exit 1
fi
