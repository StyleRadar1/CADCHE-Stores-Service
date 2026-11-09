#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (AISLAMIENTO TOTAL: QA Y PROD INDEPENDIENTES)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# REGLA ARQUITECTÓNICA ESTRICTA:
# Base de datos desplegada en cada ambiente. QA y PROD NUNCA comparten base de datos,
# servidor de base de datos, ni cuenta de almacenamiento Blob.
# ==============================================================================

set -euo pipefail

ENVIRONMENT="${1:-qa}" # Opciones: qa | prod | all
LOCATION="eastus"
ADMIN_USER="styleradaradmin"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-StyleRadarPass2026!}"

deploy_environment() {
  local ENV_NAME="$1"
  local RG="rg-styleradar-${ENV_NAME}"
  local PG_SERVER="styleradar-pg-${ENV_NAME}"
  local APP_NAME="styleradar-tiendas-${ENV_NAME}"
  local PLAN_NAME="plan-styleradar-${ENV_NAME}"
  local STORAGE_NAME="styleradarblob${ENV_NAME}"

  echo ""
  echo "=========================================================="
  echo "  Desplegando Ambiente Exclusivo: ${ENV_NAME^^}"
  echo "=========================================================="
  echo "Resource Group:    $RG"
  echo "PostgreSQL Server: $PG_SERVER"
  echo "Base de Datos:     styleradar_tiendas (Totalmente aislada)"
  echo "Web App Service:   $APP_NAME"
  echo "Blob Storage:      $STORAGE_NAME"
  echo "=========================================================="

  # 1. Resource Group
  echo "[1/6] Creando Resource Group '$RG'..."
  az group create --name "$RG" --location "$LOCATION" --output none

  # 2. Servidor PostgreSQL Flexible Server Independiente (Free Tier Standard_B1ms, 32GB)
  echo "[2/6] Aprovisionando Servidor PostgreSQL Flexible Server Dedicado..."
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

  # 3. Base de Datos propia del ambiente
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

  # 4. Azure Blob Storage Dedicado para este ambiente
  echo "[4/6] Aprovisionando Storage Account '$STORAGE_NAME'..."
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

  # 5. App Service Plan y Web App Dedicada (Puerto Oficial 8140)
  echo "[5/6] Creando App Service Plan y Web App..."
  az appservice plan create \
    --name "$PLAN_NAME" \
    --resource-group "$RG" \
    --is-linux \
    --sku B1 \
    --location "$LOCATION" \
    --output none

  az webapp create \
    --resource-group "$RG" \
    --plan "$PLAN_NAME" \
    --name "$APP_NAME" \
    --deployment-container-image-name "eclipse-temurin:17-jre" \
    --output none

  # 6. Configurar Variables de Entorno del Ambiente
  echo "[6/6] Configurando App Settings en $APP_NAME..."
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
  echo "Swagger UI en Vivo:  https://${APP_NAME}.azurewebsites.net/swagger-ui/index.html"
  echo "OpenAPI JSON Spec:   https://${APP_NAME}.azurewebsites.net/v3/api-docs"
  echo "Health Check:        https://${APP_NAME}.azurewebsites.net/actuator/health"
  echo "PostgreSQL Server:   ${PG_SERVER}.postgres.database.azure.com:5432"
  echo "Base de Datos:       styleradar_tiendas (Exclusiva para $ENV_NAME)"
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
