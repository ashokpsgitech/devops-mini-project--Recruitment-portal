# TalentBridge ATS — DevOps Operational Runbook

This runbook outlines operational procedures, incident response workflows, and maintenance guidelines for TalentBridge production environments.

---

## 1. Routine Operations & Maintenance

### 1.1 Database Schema Migrations
TalentBridge uses Flyway for automated schema evolution.
- **Location:** `src/main/resources/db/migration`
- **Naming Convention:** `V<Version>__<Description>.sql` (e.g. `V3__add_department_budget.sql`)
- **Execution:** Migrations run automatically on application startup.
- **Rollback Procedure:** Flyway Community does not support automatic down migrations. To revert a migration:
  1. Author a compensating forward migration (e.g. `V4__revert_department_budget.sql`).
  2. Test migration locally with `./mvnw spring-boot:run`.
  3. Deploy via standard CI/CD pipeline.

### 1.2 Database Backup & Disaster Recovery
- **Azure PostgreSQL Flexible Server:** Automated point-in-time recovery (PITR) is configured with 7-day retention.
- **Manual Backup (via CLI):**
  ```bash
  pg_dump -h <host> -U recruitment_app -d recruitment_db -Fc -f backup_$(date +%Y%m%d).dump
  ```
- **Restore:**
  ```bash
  pg_restore -h <host> -U recruitment_app -d recruitment_db -c backup_<date>.dump
  ```

---

## 2. Health Monitoring & Alerts

| Metric / Endpoint | Normal Threshold | Action if Breached |
|---|---|---|
| `http://<host>:8080/actuator/health` | `{"status":"UP"}` | Check container logs: `docker logs talentbridge-app` |
| `jvm_memory_used_bytes{area="heap"}` | < 80% Max RAM | Investigate memory leaks; scale container memory |
| `hikaricp_connections_pending` | 0 | Increase connection pool size or investigate slow DB queries |
| `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` | 0 | Inspect application exception logs in Log Analytics / Grafana |

---

## 3. Incident Response Playbook

### High HTTP 500 Error Rate
1. Check live container logs:
   ```bash
   docker logs --tail 200 talentbridge-app
   ```
2. Check database connectivity:
   ```bash
   docker exec -it talentbridge-postgres pg_isready -U recruitment_app
   ```
3. If database is unreachable, check PostgreSQL disk space and restart:
   ```bash
   docker compose restart postgres
   ```

### High CPU or OutOfMemoryError (OOM)
1. Container is configured with `-XX:+ExitOnOutOfMemoryError` so it will restart automatically upon OOM.
2. Check Grafana JVM Heap panel to see if memory climbed steadily (leak) or spiked suddenly (large file upload).
3. Verify file upload limit is respected (5MB configured in `application.properties`).

---

## 4. GitHub & Azure Automation Setup Guide

TalentBridge supports two primary enterprise automation pathways: **GitHub Actions** and **Azure DevOps Pipelines**.

### 4.1 GitHub Actions -> Azure Automation

#### Required GitHub Repository Secrets
Navigate to **Settings -> Secrets and variables -> Actions** in your GitHub repository and add:

| Secret Name | Description | Example / Source |
|---|---|---|
| `AZURE_CLIENT_ID` | App Registration (SPN) Application/Client ID | Azure AD App Registration |
| `AZURE_TENANT_ID` | Azure Active Directory Tenant ID | Azure Portal |
| `AZURE_SUBSCRIPTION_ID` | Target Azure Subscription ID | Azure Subscription |
| `ACR_LOGIN_SERVER` | Azure Container Registry server | `talentbridgeacr.azurecr.io` |
| `ACR_USERNAME` | ACR Admin Username or SPN | ACR Access Keys |
| `ACR_PASSWORD` | ACR Admin Password or SPN Secret | ACR Access Keys |
| `AZURE_WEBAPP_NAME` | Name of the Azure Linux Web App Container | `app-talentbridge-prod` |
| `TF_VAR_DB_PASSWORD` | Strong PostgreSQL admin password for Terraform | `SecretPsqlPassword!2026` |

#### Workflows Included in `.github/workflows/`:
1. **`ci.yml`**:
   - Triggers on PRs and pushes to `main` and `develop`.
   - Runs Maven verify, JaCoCo coverage (with artifact upload), Trivy container vulnerability scanning, and local smoke test.
2. **`deploy-azure.yml`**:
   - Triggers on push to `main` or manual trigger via `workflow_dispatch`.
   - Compiles JAR, logs into Azure via OIDC/SPN, builds & pushes Docker image to ACR, and deploys to Azure Web App Container with health verification.
3. **`terraform.yml`**:
   - Triggers on changes to `terraform/**`.
   - Runs `terraform fmt -check`, `terraform init`, `terraform validate`, and generates `terraform plan`. On merge to `main`, executes `terraform apply`.

---

### 4.2 Azure DevOps Multi-Stage Pipeline (`azure-pipelines.yml`)

#### Setup Prerequisites in Azure DevOps:
1. **Service Connection 1 (Azure Resource Manager):**
   - Name: `Azure-DevOps-TalentBridge-SPN`
   - Scope: Target Resource Group / Subscription
2. **Service Connection 2 (Docker Registry):**
   - Name: `TalentBridge-ACR-ServiceConnection`
   - Registry URL: `https://talentbridgeacr.azurecr.io`
3. **Pipeline Stages in `azure-pipelines.yml`:**
   - **Stage 1 (Build & Test):** JDK 21 setup, Maven package caching, JUnit test publication, JaCoCo coverage publishing, drop artifact staging.
   - **Stage 2 (Containerize):** Docker Build & Push to ACR tagged with `$(Build.BuildId)` and `latest`.
   - **Stage 3 (DeployProduction):** Automatic deployment to Azure Linux Web App Container via `AzureWebAppContainer@1`.

