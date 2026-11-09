# ==============================================================================
# STYLE RADAR - DESPLIEGUE AZURE CLOUD (AISLAMIENTO TOTAL: QA Y PROD INDEPENDIENTES)
# Microservicio: CADCHE-Stores-Service (ms-tiendas :8140)
# ==============================================================================
# REGLA ARQUITECTÓNICA ESTRICTA:
# Base de datos desplegada en cada ambiente. QA y PROD NUNCA comparten base de datos,
# servidor de base de datos, ni cuenta de almacenamiento Blob.
# ==============================================================================

param (
    [ValidateSet("qa", "prod", "all")]
    [string]$Environment = "qa",
    [string]$Location = "eastus",
    [string]$AdminUser = "styleradaradmin",
    [string]$AdminPassword = ""
)

$ErrorActionPreference = "Stop"

function Deploy-StyleRadarEnvironment {
    param (
        [string]$EnvName
    )

    $rg = "rg-styleradar-$EnvName"
    $pgServer = "styleradar-pg-$EnvName"
    $appName = "styleradar-tiendas-$EnvName"
    $planName = "plan-styleradar-$EnvName"
    $storageName = "styleradarblob$EnvName"

    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "  Desplegando Ambiente Exclusivo: $($EnvName.ToUpper())" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Resource Group:    $rg" -ForegroundColor Gray
    Write-Host "PostgreSQL Server: $pgServer" -ForegroundColor Gray
    Write-Host "Base de Datos:     styleradar_tiendas (Totalmente aislada)" -ForegroundColor Gray
    Write-Host "Web App Service:   $appName" -ForegroundColor Gray
    Write-Host "Blob Storage:      $storageName" -ForegroundColor Gray
    Write-Host "==========================================================" -ForegroundColor Cyan

    # 1. Resource Group
    Write-Host "[1/6] Creando Resource Group '$rg'..." -ForegroundColor Yellow
    az group create --name $rg --location $Location --output none

    # 2. Servidor PostgreSQL Flexible Server Dedicado
    Write-Host "[2/6] Aprovisionando Servidor PostgreSQL Flexible Server Dedicado..." -ForegroundColor Yellow
    az postgres flexible-server create `
        --resource-group $rg `
        --name $pgServer `
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

    # 3. Base de Datos propia del ambiente
    Write-Host "[3/6] Creando Base de Datos 'styleradar_tiendas' en $pgServer..." -ForegroundColor Yellow
    az postgres flexible-server db create `
        --resource-group $rg `
        --server-name $pgServer `
        --database-name styleradar_tiendas `
        --output none

    # Firewall
    az postgres flexible-server firewall-rule create `
        --resource-group $rg `
        --server-name $pgServer `
        --rule-name AllowAllAzureServices `
        --start-ip-address 0.0.0.0 `
        --end-ip-address 0.0.0.0 `
        --output none

    # 4. Azure Blob Storage Dedicado
    Write-Host "[4/6] Aprovisionando Storage Account '$storageName'..." -ForegroundColor Yellow
    az storage account create `
        --resource-group $rg `
        --name $storageName `
        --location $Location `
        --sku Standard_LRS `
        --kind StorageV2 `
        --allow-blob-public-access true `
        --output none

    $storageKey = az storage account keys list --resource-group $rg --account-name $storageName --query "[0].value" -o tsv

    az storage container create `
        --account-name $storageName `
        --account-key $storageKey `
        --name tiendas `
        --public-access blob `
        --output none

    # 5. App Service Plan y Web App Dedicada (Puerto 8140)
    Write-Host "[5/6] Creando App Service Plan y Web App..." -ForegroundColor Yellow
    az appservice plan create `
        --name $planName `
        --resource-group $rg `
        --is-linux `
        --sku B1 `
        --location $Location `
        --output none

    az webapp create `
        --resource-group $rg `
        --plan $planName `
        --name $appName `
        --deployment-container-image-name "eclipse-temurin:17-jre" `
        --output none

    # 6. Variables de Entorno del Ambiente
    Write-Host "[6/6] Configurando App Settings en $appName..." -ForegroundColor Yellow
    $dbUrl = "jdbc:postgresql://$pgServer.postgres.database.azure.com:5432/styleradar_tiendas?sslmode=require"
    $blobEndpoint = "https://$storageName.blob.core.windows.net"

    az webapp config appsettings set `
        --name $appName `
        --resource-group $rg `
        --settings `
            WEBSITES_PORT=8140 `
            PORT=8140 `
            SPRING_PROFILES_ACTIVE="$EnvName" `
            DB_URL="$dbUrl" `
            DB_USERNAME="$AdminUser" `
            DB_PASSWORD="$AdminPassword" `
            AZURE_STORAGE_BLOB_ENDPOINT="$blobEndpoint" `
            SWAGGER_SERVER_URL="https://$appName.azurewebsites.net" `
        --output none

    Write-Host "`n==========================================================" -ForegroundColor Green
    Write-Host "  ¡AMBIENTE $($EnvName.ToUpper()) DESPLEGADO EXITOSAMENTE!" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "Swagger UI en Vivo:  https://$appName.azurewebsites.net/swagger-ui/index.html" -ForegroundColor Cyan
    Write-Host "OpenAPI JSON Spec:   https://$appName.azurewebsites.net/v3/api-docs" -ForegroundColor Cyan
    Write-Host "Health Check:        https://$appName.azurewebsites.net/actuator/health" -ForegroundColor Cyan
    Write-Host "PostgreSQL Server:   $pgServer.postgres.database.azure.com:5432" -ForegroundColor Yellow
    Write-Host "Base de Datos:       styleradar_tiendas (Exclusiva para $EnvName)" -ForegroundColor Yellow
    Write-Host "Blob Endpoint:       $blobEndpoint" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Green
}

# 1. Verificar sesión de Azure
$currentAccount = az account show 2>$null | ConvertFrom-Json
if (-not $currentAccount) {
    Write-Host "Iniciando sesión en Azure..." -ForegroundColor Yellow
    az login
}

if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    $AdminPassword = "StyleRadarPass" + (Get-Random -Minimum 1000 -Maximum 9999) + "!"
}

if ($Environment -eq "all") {
    Deploy-StyleRadarEnvironment -EnvName "qa"
    Deploy-StyleRadarEnvironment -EnvName "prod"
} else {
    Deploy-StyleRadarEnvironment -EnvName $Environment
}
