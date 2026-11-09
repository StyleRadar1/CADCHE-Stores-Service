# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (NIVEL 100% GRATIS / AZURE FOR STUDENTS)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# Este script aprovisiona la infraestructura en Azure utilizando la capa GRATUITA
# de Azure Database for PostgreSQL Flexible Server (750 horas/mes gratis de B1ms
# y 32 GB de almacenamiento sin costo) para que el gasto sea $0.00 USD.
# Aloja las dos bases de datos (QA y PROD) en la misma instancia gratuita.
# ==============================================================================

param (
    [string]$SubscriptionId = "",
    [string]$Location = "eastus",
    [string]$ResourceGroup = "rg-styleradar-free",
    [string]$PostgresServer = "styleradar-pg-free",
    [string]$AdminUser = "styleradaradmin",
    [string]$AdminPassword = "",
    [string]$AppServicePlan = "plan-styleradar-free",
    [string]$AppServiceSku = "F1",  # F1 = 100% Gratis (60 min CPU/día), o B1 Basic (~$12/mes con crédito)
    [string]$StorageAccount = "styleradarblobfree"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  StyleRadar · Despliegue Azure Gratuito (Capa Free Tier) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verificar inicio de sesión en Azure
Write-Host "`n[1/7] Verificando sesión en Azure..." -ForegroundColor Yellow
$currentAccount = az account show 2>$null | ConvertFrom-Json
if (-not $currentAccount) {
    Write-Host "Iniciando sesión en Azure... Por favor autentícate en el navegador." -ForegroundColor Yellow
    az login
    $currentAccount = az account show | ConvertFrom-Json
}

if ($SubscriptionId -ne "") {
    az account set --subscription $SubscriptionId
}
Write-Host "Suscripción activa: $($currentAccount.name) ($($currentAccount.id))" -ForegroundColor Green

# 2. Crear Grupo de Recursos
Write-Host "`n[2/7] Creando Resource Group '$ResourceGroup' en '$Location'..." -ForegroundColor Yellow
az group create --name $ResourceGroup --location $Location --output none
Write-Host "Resource Group listo." -ForegroundColor Green

# 3. Aprovisionar Azure Database for PostgreSQL Flexible Server (Free Tier)
# NOTA: Standard_B1ms con 32GB califica para el beneficio de 750 horas/mes gratis en Azure.
Write-Host "`n[3/7] Aprovisionando PostgreSQL Flexible Server Gratuito (Standard_B1ms, 32GB)..." -ForegroundColor Yellow
if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    $AdminPassword = "StyleRadarPass" + (Get-Random -Minimum 1000 -Maximum 9999) + "!"
    Write-Host "Contraseña generada para PostgreSQL: $AdminPassword" -ForegroundColor Magenta
}

az postgres flexible-server create `
    --resource-group $ResourceGroup `
    --name $PostgresServer `
    --location $Location `
    --admin-user $AdminUser `
    --admin-password $AdminPassword `
    --sku-name Standard_B1ms `
    --tier Burstable `
    --storage-size 32 `
    --version 16 `
    --public-access 0.0.0.0 `
    --yes `
    --output none

Write-Host "Servidor PostgreSQL Free Tier activo: $PostgresServer.postgres.database.azure.com" -ForegroundColor Green

# Crear base de datos para QA
Write-Host "Creando base de datos QA 'styleradar_tiendas_qa'..." -ForegroundColor Yellow
az postgres flexible-server db create `
    --resource-group $ResourceGroup `
    --server-name $PostgresServer `
    --database-name styleradar_tiendas_qa `
    --output none

# Crear base de datos para PROD en la misma instancia para no consumir horas extra
Write-Host "Creando base de datos PROD 'styleradar_tiendas_prod'..." -ForegroundColor Yellow
az postgres flexible-server db create `
    --resource-group $ResourceGroup `
    --server-name $PostgresServer `
    --database-name styleradar_tiendas_prod `
    --output none

# Habilitar acceso de Azure Services en Firewall
az postgres flexible-server firewall-rule create `
    --resource-group $ResourceGroup `
    --server-name $PostgresServer `
    --rule-name AllowAllAzureServices `
    --start-ip-address 0.0.0.0 `
    --end-ip-address 0.0.0.0 `
    --output none

# 4. Aprovisionar Azure Blob Storage
Write-Host "`n[4/7] Aprovisionando Azure Blob Storage '$StorageAccount'..." -ForegroundColor Yellow
az storage account create `
    --resource-group $ResourceGroup `
    --name $StorageAccount `
    --location $Location `
    --sku Standard_LRS `
    --kind StorageV2 `
    --allow-blob-public-access true `
    --output none

$storageKey = az storage account keys list --resource-group $ResourceGroup --account-name $StorageAccount --query "[0].value" -o tsv

az storage container create `
    --account-name $StorageAccount `
    --account-key $storageKey `
    --name tiendas `
    --public-access blob `
    --output none

Write-Host "Contenedor de blobs 'tiendas' listo." -ForegroundColor Green

# 5. Aprovisionar Azure App Service Plan (F1 Free o B1)
Write-Host "`n[5/7] Aprovisionando App Service Plan ($AppServiceSku)..." -ForegroundColor Yellow
az appservice plan create `
    --name $AppServicePlan `
    --resource-group $ResourceGroup `
    --is-linux `
    --sku $AppServiceSku `
    --location $Location `
    --output none

# Crear Web App QA
$qaAppName = "styleradar-tiendas-qa"
Write-Host "Creando Web App QA '$qaAppName'..." -ForegroundColor Yellow
az webapp create `
    --resource-group $ResourceGroup `
    --plan $AppServicePlan `
    --name $qaAppName `
    --deployment-container-image-name "eclipse-temurin:17-jre" `
    --output none

# Crear Web App PROD
$prodAppName = "styleradar-tiendas-prod"
Write-Host "Creando Web App PROD '$prodAppName'..." -ForegroundColor Yellow
az webapp create `
    --resource-group $ResourceGroup `
    --plan $AppServicePlan `
    --name $prodAppName `
    --deployment-container-image-name "eclipse-temurin:17-jre" `
    --output none

# 6. Configurar Variables de Entorno (App Settings)
Write-Host "`n[6/7] Configurando App Settings en Web Apps..." -ForegroundColor Yellow
$blobEndpoint = "https://$StorageAccount.blob.core.windows.net"
$dbUrlQa = "jdbc:postgresql://$PostgresServer.postgres.database.azure.com:5432/styleradar_tiendas_qa?sslmode=require"
$dbUrlProd = "jdbc:postgresql://$PostgresServer.postgres.database.azure.com:5432/styleradar_tiendas_prod?sslmode=require"

# Configurar QA
az webapp config appsettings set `
    --name $qaAppName `
    --resource-group $ResourceGroup `
    --settings `
        WEBSITES_PORT=8140 `
        PORT=8140 `
        SPRING_PROFILES_ACTIVE=qa `
        DB_URL="$dbUrlQa" `
        DB_USERNAME="$AdminUser" `
        DB_PASSWORD="$AdminPassword" `
        AZURE_STORAGE_BLOB_ENDPOINT="$blobEndpoint" `
        SWAGGER_SERVER_URL="https://$qaAppName.azurewebsites.net" `
    --output none

# Configurar PROD
az webapp config appsettings set `
    --name $prodAppName `
    --resource-group $ResourceGroup `
    --settings `
        WEBSITES_PORT=8140 `
        PORT=8140 `
        SPRING_PROFILES_ACTIVE=prod `
        DB_URL="$dbUrlProd" `
        DB_USERNAME="$AdminUser" `
        DB_PASSWORD="$AdminPassword" `
        AZURE_STORAGE_BLOB_ENDPOINT="$blobEndpoint" `
        SWAGGER_SERVER_URL="https://$prodAppName.azurewebsites.net" `
    --output none

# 7. Resumen de Despliegue
Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ¡DESPLIEGUE EN AZURE (TIER GRATUITO) COMPLETADO!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Costo Mensual Estimado:    $0.00 USD (Aprovecha las 750h/mes gratis de PostgreSQL B1ms)" -ForegroundColor Magenta
Write-Host "Swagger UI QA (En vivo):   https://$qaAppName.azurewebsites.net/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "Swagger UI PROD (En vivo): https://$prodAppName.azurewebsites.net/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "PostgreSQL Server:         $PostgresServer.postgres.database.azure.com:5432" -ForegroundColor Yellow
Write-Host "Base de Datos QA:          styleradar_tiendas_qa" -ForegroundColor Yellow
Write-Host "Base de Datos PROD:        styleradar_tiendas_prod" -ForegroundColor Yellow
Write-Host "Usuario Administrador:     $AdminUser" -ForegroundColor Yellow
Write-Host "Contraseña BD:             $AdminPassword" -ForegroundColor Yellow
Write-Host "Azure Blob Endpoint:       $blobEndpoint" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
