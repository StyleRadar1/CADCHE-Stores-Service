# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (AZURE FOR STUDENTS)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# Este script aprovisiona toda la infraestructura en Azure optimizada para 
# aprovechar al máximo los 100 USD de crédito gratuito de Azure for Students.
# ==============================================================================

param (
    [string]$SubscriptionId = "",
    [string]$Location = "eastus",
    [string]$ResourceGroup = "rg-styleradar-qa",
    [string]$PostgresServer = "styleradar-pg-qa",
    [string]$AdminUser = "styleradaradmin",
    [string]$AdminPassword = "",
    [string]$AppServiceName = "styleradar-tiendas-qa",
    [string]$AppServicePlan = "plan-styleradar-qa",
    [string]$StorageAccount = "styleradarblobqa"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  StyleRadar · Despliegue en Microsoft Azure (Estudiantes)" -ForegroundColor Cyan
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

# 3. Aprovisionar Azure Database for PostgreSQL Flexible Server
# Usamos Standard_B1ms (Burstable: 1 vCPU, 2GB RAM, 32GB Storage), cuesta ~$15/mes cubierto por créditos
Write-Host "`n[3/7] Aprovisionando Azure Database for PostgreSQL Flexible Server..." -ForegroundColor Yellow
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

Write-Host "Servidor PostgreSQL activo: $PostgresServer.postgres.database.azure.com" -ForegroundColor Green

# Crear base de datos para ms-tiendas
Write-Host "Creando base de datos 'styleradar_tiendas'..." -ForegroundColor Yellow
az postgres flexible-server db create `
    --resource-group $ResourceGroup `
    --server-name $PostgresServer `
    --database-name styleradar_tiendas `
    --output none

# Habilitar acceso de Azure Services en Firewall
az postgres flexible-server firewall-rule create `
    --resource-group $ResourceGroup `
    --server-name $PostgresServer `
    --rule-name AllowAllAzureServices `
    --start-ip-address 0.0.0.0 `
    --end-ip-address 0.0.0.0 `
    --output none

# 4. Aprovisionar Azure Blob Storage (Para fotos de tiendas y RUT PDF)
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

Write-Host "Contenedor de blobs 'tiendas' listo con acceso público de lectura." -ForegroundColor Green

# 5. Aprovisionar Azure App Service (Linux B1 Basic o F1 Free)
Write-Host "`n[5/7] Aprovisionando App Service Plan y Web App..." -ForegroundColor Yellow
az appservice plan create `
    --name $AppServicePlan `
    --resource-group $ResourceGroup `
    --is-linux `
    --sku B1 `
    --location $Location `
    --output none

az webapp create `
    --resource-group $ResourceGroup `
    --plan $AppServicePlan `
    --name $AppServiceName `
    --deployment-container-image-name "eclipse-temurin:17-jre" `
    --output none

# 6. Configurar Variables de Entorno (App Settings)
Write-Host "`n[6/7] Configurando variables de entorno en el App Service..." -ForegroundColor Yellow
$dbUrl = "jdbc:postgresql://$PostgresServer.postgres.database.azure.com:5432/styleradar_tiendas?sslmode=require"
$blobEndpoint = "https://$StorageAccount.blob.core.windows.net"

az webapp config appsettings set `
    --name $AppServiceName `
    --resource-group $ResourceGroup `
    --settings `
        WEBSITES_PORT=8140 `
        PORT=8140 `
        SPRING_PROFILES_ACTIVE=qa `
        DB_URL="$dbUrl" `
        DB_USERNAME="$AdminUser" `
        DB_PASSWORD="$AdminPassword" `
        AZURE_STORAGE_BLOB_ENDPOINT="$blobEndpoint" `
        SWAGGER_SERVER_URL="https://$AppServiceName.azurewebsites.net" `
    --output none

# 7. Resumen de Conexión y Swagger URLs
Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ¡DESPLIEGUE EN AZURE COMPLETADO CON ÉXITO!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Swagger UI QA (En vivo):   https://$AppServiceName.azurewebsites.net/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "OpenAPI JSON Spec:         https://$AppServiceName.azurewebsites.net/v3/api-docs" -ForegroundColor Cyan
Write-Host "Health Check:              https://$AppServiceName.azurewebsites.net/actuator/health" -ForegroundColor Cyan
Write-Host "PostgreSQL Server:         $PostgresServer.postgres.database.azure.com:5432" -ForegroundColor Yellow
Write-Host "Base de Datos:             styleradar_tiendas" -ForegroundColor Yellow
Write-Host "Usuario BD:                $AdminUser" -ForegroundColor Yellow
Write-Host "Contraseña BD:             $AdminPassword" -ForegroundColor Yellow
Write-Host "Azure Blob Endpoint:       $blobEndpoint" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green

