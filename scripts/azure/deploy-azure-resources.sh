#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (AZURE FOR STUDENTS)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# Ejecutable directamente en Azure Cloud Shell (https://shell.azure.com)
# ==============================================================================

set -euo pipefail

LOCATION="eastus"
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-styleradar-qa}"
POSTGRES_SERVER="${POSTGRES_SERVER:-styleradar-pg-qa-$RANDOM}"
ADMIN_USER="${ADMIN_USER:-styleradaradmin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-StyleRadarPass$(date +%s)!}"
APP_SERVICE_NAME="${APP_SERVICE_NAME:-styleradar-tiendas-qa-$RANDOM}"
APP_SERVICE_PLAN="${APP_SERVICE_PLAN:-plan-styleradar-qa}"
STORAGE_ACCOUNT="${STORAGE_ACCOUNT:-styleradarblobqa$RANDOM}"

echo "=========================================================="
echo "  StyleRadar · Despliegue en Microsoft Azure (Estudiantes)"
echo "=========================================================="

# 1. Crear Resource Group
echo "[1/6] Creando Resource Group '$RESOURCE_GROUP' en '$LOCATION'..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

# 2. Aprovisionar Azure Database for PostgreSQL Flexible Server
echo "[2/6] Aprovisionando PostgreSQL Flexible Server '$POSTGRES_SERVER'..."
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER" \
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

# Crear base de datos para ms-tiendas
echo "Creando base de datos 'styleradar_tiendas'..."
az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --database-name styleradar_tiendas \
  --output none

# Firewall para permitir servicios de Azure
az postgres flexible-server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --rule-name AllowAllAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0 \
  --output none

# 3. Aprovisionar Azure Blob Storage
echo "[3/6] Aprovisionando Storage Account '$STORAGE_ACCOUNT'..."
az storage account create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$STORAGE_ACCOUNT" \
  --location "$LOCATION" \
  --sku Standard_LRS \
  --kind StorageV2 \
  --allow-blob-public-access true \
  --output none

STORAGE_KEY=$(az storage account keys list --resource-group "$RESOURCE_GROUP" --account-name "$STORAGE_ACCOUNT" --query "[0].value" -o tsv)

az storage container create \
  --account-name "$STORAGE_ACCOUNT" \
  --account-key "$STORAGE_KEY" \
  --name tiendas \
  --public-access blob \
  --output none

# 4. Aprovisionar App Service
echo "[4/6] Aprovisionando App Service Plan y Web App..."
az appservice plan create \
  --name "$APP_SERVICE_PLAN" \
  --resource-group "$RESOURCE_GROUP" \
  --is-linux \
  --sku B1 \
  --location "$LOCATION" \
  --output none

az webapp create \
  --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" \
  --name "$APP_SERVICE_NAME" \
  --deployment-container-image-name "eclipse-temurin:17-jre" \
  --output none

# 5. Configurar App Settings
echo "[5/6] Configurando App Settings en Web App..."
DB_URL="jdbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/styleradar_tiendas?sslmode=require"
BLOB_ENDPOINT="https://${STORAGE_ACCOUNT}.blob.core.windows.net"

az webapp config appsettings set \
  --name "$APP_SERVICE_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --settings \
    WEBSITES_PORT=8140 \
    PORT=8140 \
    SPRING_PROFILES_ACTIVE=qa \
    DB_URL="$DB_URL" \
    DB_USERNAME="$ADMIN_USER" \
    DB_PASSWORD="$ADMIN_PASSWORD" \
    AZURE_STORAGE_BLOB_ENDPOINT="$BLOB_ENDPOINT" \
    SWAGGER_SERVER_URL="https://${APP_SERVICE_NAME}.azurewebsites.net" \
  --output none

echo "=========================================================="
echo "  ¡DESPLIEGUE EN AZURE COMPLETADO!"
echo "=========================================================="
echo "Swagger UI QA (En vivo):   https://${APP_SERVICE_NAME}.azurewebsites.net/swagger-ui/index.html"
echo "OpenAPI JSON Spec:         https://${APP_SERVICE_NAME}.azurewebsites.net/v3/api-docs"
echo "Health Check:              https://${APP_SERVICE_NAME}.azurewebsites.net/actuator/health"
echo "PostgreSQL Server:         ${POSTGRES_SERVER}.postgres.database.azure.com:5432"
echo "Base de Datos:             styleradar_tiendas"
echo "Usuario BD:                ${ADMIN_USER}"
echo "Contraseña BD:             ${ADMIN_PASSWORD}"
echo "Azure Blob Endpoint:       ${BLOB_ENDPOINT}"
echo "=========================================================="

