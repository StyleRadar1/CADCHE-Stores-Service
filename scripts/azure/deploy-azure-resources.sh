#!/usr/bin/env bash
# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (NIVEL 100% GRATIS / AZURE FOR STUDENTS)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# Ejecutable directamente en Azure Cloud Shell (https://shell.azure.com)
# ==============================================================================

set -euo pipefail

LOCATION="eastus"
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-styleradar-free}"
POSTGRES_SERVER="${POSTGRES_SERVER:-styleradar-pg-free-$RANDOM}"
ADMIN_USER="${ADMIN_USER:-styleradaradmin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-StyleRadarPass$(date +%s)!}"
APP_SERVICE_PLAN="${APP_SERVICE_PLAN:-plan-styleradar-free}"
APP_SERVICE_SKU="${APP_SERVICE_SKU:-F1}"  # F1 = 100% Gratis (60 min CPU/día), o B1 Basic (~$12/mes con crédito)
STORAGE_ACCOUNT="${STORAGE_ACCOUNT:-styleradarblobfree$RANDOM}"
QA_APP_NAME="styleradar-tiendas-qa"
PROD_APP_NAME="styleradar-tiendas-prod"

echo "=========================================================="
echo "  StyleRadar · Despliegue Azure Gratuito (Capa Free Tier) "
echo "=========================================================="

# 1. Crear Resource Group
echo "[1/6] Creando Resource Group '$RESOURCE_GROUP' en '$LOCATION'..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

# 2. Aprovisionar Azure Database for PostgreSQL Flexible Server (Free Tier)
# NOTA: Standard_B1ms con 32GB califica para las 750 horas/mes gratis de Azure.
echo "[2/6] Aprovisionando PostgreSQL Flexible Server Gratuito (Standard_B1ms, 32GB)..."
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

# Crear base de datos para QA
echo "Creando base de datos QA 'styleradar_tiendas_qa'..."
az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --database-name styleradar_tiendas_qa \
  --output none

# Crear base de datos para PROD en la misma instancia para no consumir horas extra
echo "Creando base de datos PROD 'styleradar_tiendas_prod'..."
az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --database-name styleradar_tiendas_prod \
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

# 4. Aprovisionar App Service Plan (F1 Free o B1)
echo "[4/6] Aprovisionando App Service Plan ($APP_SERVICE_SKU)..."
az appservice plan create \
  --name "$APP_SERVICE_PLAN" \
  --resource-group "$RESOURCE_GROUP" \
  --is-linux \
  --sku "$APP_SERVICE_SKU" \
  --location "$LOCATION" \
  --output none

# Crear Web App QA
echo "Creando Web App QA '$QA_APP_NAME'..."
az webapp create \
  --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" \
  --name "$QA_APP_NAME" \
  --deployment-container-image-name "eclipse-temurin:17-jre" \
  --output none

# Crear Web App PROD
echo "Creando Web App PROD '$PROD_APP_NAME'..."
az webapp create \
  --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" \
  --name "$PROD_APP_NAME" \
  --deployment-container-image-name "eclipse-temurin:17-jre" \
  --output none

# 5. Configurar App Settings
echo "[5/6] Configurando App Settings en Web Apps..."
BLOB_ENDPOINT="https://${STORAGE_ACCOUNT}.blob.core.windows.net"
DB_URL_QA="jdbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/styleradar_tiendas_qa?sslmode=require"
DB_URL_PROD="jdbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/styleradar_tiendas_prod?sslmode=require"

# Configurar QA
az webapp config appsettings set \
  --name "$QA_APP_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --settings \
    WEBSITES_PORT=8140 \
    PORT=8140 \
    SPRING_PROFILES_ACTIVE=qa \
    DB_URL="$DB_URL_QA" \
    DB_USERNAME="$ADMIN_USER" \
    DB_PASSWORD="$ADMIN_PASSWORD" \
    AZURE_STORAGE_BLOB_ENDPOINT="$BLOB_ENDPOINT" \
    SWAGGER_SERVER_URL="https://${QA_APP_NAME}.azurewebsites.net" \
  --output none

# Configurar PROD
az webapp config appsettings set \
  --name "$PROD_APP_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --settings \
    WEBSITES_PORT=8140 \
    PORT=8140 \
    SPRING_PROFILES_ACTIVE=prod \
    DB_URL="$DB_URL_PROD" \
    DB_USERNAME="$ADMIN_USER" \
    DB_PASSWORD="$ADMIN_PASSWORD" \
    AZURE_STORAGE_BLOB_ENDPOINT="$BLOB_ENDPOINT" \
    SWAGGER_SERVER_URL="https://${PROD_APP_NAME}.azurewebsites.net" \
  --output none

echo "=========================================================="
echo "  ¡DESPLIEGUE EN AZURE (TIER GRATUITO) COMPLETADO!"
echo "=========================================================="
echo "Costo Mensual:             \$0.00 USD (Aprovecha las 750h/mes gratis de PostgreSQL B1ms)"
echo "Swagger UI QA (En vivo):   https://${QA_APP_NAME}.azurewebsites.net/swagger-ui/index.html"
echo "Swagger UI PROD (En vivo): https://${PROD_APP_NAME}.azurewebsites.net/swagger-ui/index.html"
echo "PostgreSQL Server:         ${POSTGRES_SERVER}.postgres.database.azure.com:5432"
echo "Base de Datos QA:          styleradar_tiendas_qa"
echo "Base de Datos PROD:        styleradar_tiendas_prod"
echo "Usuario Administrador:     ${ADMIN_USER}"
echo "Contraseña BD:             ${ADMIN_PASSWORD}"
echo "Azure Blob Endpoint:       ${BLOB_ENDPOINT}"
echo "=========================================================="
