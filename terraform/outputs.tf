output "resource_group_name" {
  value       = azurerm_resource_group.rg.name
  description = "Name of the provisioned Azure Resource Group"
}

output "web_app_url" {
  value       = "https://${azurerm_linux_web_app.app.default_hostname}"
  description = "Public URL for TalentBridge ATS web application"
}

output "postgres_fqdn" {
  value       = azurerm_postgresql_flexible_server.postgres.fqdn
  description = "Fully Qualified Domain Name of PostgreSQL Flexible Server"
}

output "storage_account_name" {
  value       = azurerm_storage_account.storage.name
  description = "Azure Storage Account for candidate resumes"
}

output "key_vault_uri" {
  value       = azurerm_key_vault.kv.vault_uri
  description = "URI of Azure Key Vault for application secrets"
}

output "app_insights_connection_string" {
  value       = azurerm_application_insights.appinsights.connection_string
  description = "Application Insights telemetry connection string"
  sensitive   = true
}
