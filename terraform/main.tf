terraform {
  required_version = ">= 1.5.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.90.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6.0"
    }
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = false
      recover_soft_deleted_key_vaults = true
    }
  }
}

data "azurerm_client_config" "current" {}

resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

# ==============================================================================
# RESOURCE GROUP
# ==============================================================================
resource "azurerm_resource_group" "rg" {
  name     = "rg-${var.prefix}-${var.environment}-${var.location}"
  location = var.location

  tags = {
    Environment = var.environment
    Project     = "TalentBridge-ATS"
    ManagedBy   = "Terraform"
  }
}

# ==============================================================================
# OBSERVABILITY: Log Analytics Workspace & Application Insights
# ==============================================================================
resource "azurerm_log_analytics_workspace" "logs" {
  name                = "log-${var.prefix}-${var.environment}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_application_insights" "appinsights" {
  name                = "appi-${var.prefix}-${var.environment}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  workspace_id        = azurerm_log_analytics_workspace.logs.id
  application_type    = "web"
}

# ==============================================================================
# STORAGE ACCOUNT (Resumes, Audit Exports)
# ==============================================================================
resource "azurerm_storage_account" "storage" {
  name                     = "st${var.prefix}${var.environment}${random_string.suffix.result}"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  blob_properties {
    versioning_enabled = true
    delete_retention_policy {
      days = 7
    }
  }
}

resource "azurerm_storage_container" "resumes" {
  name                  = "resumes"
  storage_account_name  = azurerm_storage_account.storage.name
  container_access_type = "private"
}

# ==============================================================================
# KEY VAULT (Zero-Trust Secret Storage)
# ==============================================================================
resource "azurerm_key_vault" "kv" {
  name                       = "kv-${var.prefix}-${random_string.suffix.result}"
  location                   = azurerm_resource_group.rg.location
  resource_group_name        = azurerm_resource_group.rg.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  soft_delete_retention_days = 7
  purge_protection_enabled   = false

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = [
      "Get", "List", "Set", "Delete", "Purge"
    ]
  }
}

resource "azurerm_key_vault_secret" "db_password" {
  name         = "database-password"
  value        = var.db_admin_password
  key_vault_id = azurerm_key_vault.kv.id
}

# ==============================================================================
# POSTGRESQL FLEXIBLE SERVER
# ==============================================================================
resource "azurerm_postgresql_flexible_server" "postgres" {
  name                   = "psql-${var.prefix}-${var.environment}-${random_string.suffix.result}"
  resource_group_name    = azurerm_resource_group.rg.name
  location               = azurerm_resource_group.rg.location
  version                = "16"
  administrator_login    = var.db_admin_user
  administrator_password = var.db_admin_password
  storage_mb             = 32768
  sku_name               = var.postgres_sku

  backup_retention_days        = 7
  geo_redundant_backups_enabled = false

  lifecycle {
    ignore_changes = [
      zone
    ]
  }
}

resource "azurerm_postgresql_flexible_server_database" "recruitment_db" {
  name      = "recruitment_db"
  server_id = azurerm_postgresql_flexible_server.postgres.id
  collation = "en_US.utf8"
  charset   = "UTF8"
}

# Allow Azure services to access PostgreSQL
resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.postgres.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

# ==============================================================================
# APP SERVICE PLAN & LINUX WEB APP CONTAINER
# ==============================================================================
resource "azurerm_service_plan" "asp" {
  name                = "asp-${var.prefix}-${var.environment}"
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  os_type             = "Linux"
  sku_name            = var.app_service_sku
}

resource "azurerm_linux_web_app" "app" {
  name                = "app-${var.prefix}-${var.environment}-${random_string.suffix.result}"
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  service_plan_id     = azurerm_service_plan.asp.id
  https_only          = true

  site_config {
    always_on = true

    application_stack {
      docker_image_name   = "talentbridge-ats:latest"
      docker_registry_url = "https://index.docker.io/v1"
    }

    health_check_path                 = "/actuator/health"
    health_check_eviction_time_in_min = 5
  }

  app_settings = {
    "WEBSITES_PORT"                        = "8080"
    "SPRING_PROFILES_ACTIVE"               = "prod"
    "DB_URL"                               = "jdbc:postgresql://${azurerm_postgresql_flexible_server.postgres.fqdn}:5432/recruitment_db?sslmode=require"
    "DB_USERNAME"                          = var.db_admin_user
    "DB_PASSWORD"                          = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.db_password.versionless_id})"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"= azurerm_application_insights.appinsights.connection_string
    "ApplicationInsightsAgent_EXTENSION_VERSION" = "~3"
  }

  identity {
    type = "SystemAssigned"
  }
}

# Grant App Service Managed Identity access to read Key Vault secrets
resource "azurerm_key_vault_access_policy" "app_kv_policy" {
  key_vault_id = azurerm_key_vault.kv.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_linux_web_app.app.identity[0].principal_id

  secret_permissions = [
    "Get"
  ]
}
