variable "prefix" {
  type        = string
  description = "Prefix for all Azure resource names"
  default     = "talentbridge"
}

variable "environment" {
  type        = string
  description = "Deployment environment (dev, staging, prod)"
  default     = "prod"
}

variable "location" {
  type        = string
  description = "Azure region for resource deployment"
  default     = "eastus"
}

variable "db_admin_user" {
  type        = string
  description = "Administrator username for PostgreSQL Flexible Server"
  default     = "talentadmin"
}

variable "db_admin_password" {
  type        = string
  description = "Administrator password for PostgreSQL Flexible Server"
  sensitive   = true
}

variable "app_service_sku" {
  type        = string
  description = "App Service Plan pricing SKU (e.g. B1, P1v3)"
  default     = "B1"
}

variable "postgres_sku" {
  type        = string
  description = "PostgreSQL Flexible Server SKU"
  default     = "B_Standard_B1ms"
}
