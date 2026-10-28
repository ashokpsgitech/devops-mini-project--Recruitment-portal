# ⚡ TalentBridge Enterprise ATS + Full DevOps Ecosystem

TalentBridge is an enterprise-grade **Recruitment and Applicant Tracking System (ATS)** engineered with **Spring Boot 3**, **Java 21 LTS**, **PostgreSQL 16**, and a complete production DevOps ecosystem featuring **Docker**, **Kubernetes/Azure Container Apps**, **Terraform (IaC)**, **GitHub Actions & Azure DevOps CI/CD**, **Prometheus & Grafana observability**, and **Trivy DevSecOps scanning**.

---

## 1. System Architecture & Capabilities

```mermaid
graph TD
    Client["Browser / REST Client"] --> WebApp["TalentBridge ATS (Spring Boot 3 + Java 21)"]
    WebApp --> Security["Spring Security (RBAC + BCrypt + CSRF)"]
    WebApp --> JPA["Spring Data JPA + Hibernate"]
    JPA --> Postgres[("PostgreSQL 16 (Flyway Migrations)")]
    WebApp --> Storage["LocalStorageService / Azure Blob Storage"]
    WebApp --> Mail["Mailpit / Azure SMTP"]
    WebApp --> Actuator["Spring Actuator + Micrometer"]
    Actuator --> Prometheus["Prometheus (:9090)"]
    Prometheus --> Grafana["Grafana Dashboards (:3000)"]
```

### Key Modules & Workflows
- **Role-Based Access Control (RBAC):** `ADMIN`, `RECRUITER`, `HIRING_MANAGER`, `APPLICANT`.
- **Requisition Management:** Job posting lifecycle (`DRAFT → PUBLISHED → CLOSED`), department mapping, salary ranges, full-text role search.
- **Applicant Portal:** Profile registration, resume upload (PDF/DOCX), cover letters, application status tracking.
- **Candidate Pipeline Kanban:** Visual ATS stage management (`APPLIED → SCREENING → SHORTLISTED → INTERVIEW → OFFERED → HIRED → REJECTED`) with audit trail.
- **Interview Scheduling & Feedback:** Multi-round evaluation (Technical, Behavioral, Executive), scoring (1-5), and recommendation logging.
- **Compensation & Offer Management:** Offer extension, joining date, validity window, candidate acceptance/decline lifecycle.
- **In-App & Email Notifications:** Real-time event notifications with unread badges and SMTP delivery.
- **REST API & OpenAPI 3.0:** Programmatic endpoints documented via Swagger UI.

---

## 2. Technology Stack

| Layer | Technologies |
|---|---|
| **Core Framework** | Java 21 LTS, Spring Boot 3.5.7, Spring Security 6 |
| **Persistence** | Spring Data JPA, Hibernate ORM 6.6, PostgreSQL 16, Flyway 10 |
| **API & Documentation** | SpringDoc OpenAPI 2.8.5, Swagger UI, Spring MVC REST |
| **Frontend UI** | Thymeleaf 3, Plus Jakarta Sans, Modern Glassmorphism CSS |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (test scope) |
| **Observability** | Spring Actuator, Micrometer Prometheus, Prometheus 2.52, Grafana 11 |
| **Containerization** | Docker multi-stage build (distroless-style JRE, non-root user), Docker Compose |
| **CI/CD** | GitHub Actions (`.github/workflows/ci.yml`), Azure DevOps (`azure-pipelines.yml`) |
| **Infrastructure as Code** | HashiCorp Terraform 1.5+ (Azure App Service, PostgreSQL Flexible, Key Vault) |

---

## 3. Quickstart: Local Development

### Option A: Run with Docker Compose (Full Stack)
Spin up the complete application stack including PostgreSQL, Mailpit, Prometheus, and Grafana:

```bash
docker compose up --build -d
```

| Service | URL | Default Credentials |
|---|---|---|
| **TalentBridge Web UI** | `http://localhost:8080/jobs` | *See seed users below* |
| **Swagger UI (REST API)** | `http://localhost:8080/swagger-ui/index.html` | Any valid user |
| **Actuator Metrics** | `http://localhost:8080/actuator/prometheus` | None |
| **Grafana Telemetry** | `http://localhost:3000` | `admin` / `admin` |
| **Prometheus Server** | `http://localhost:9090` | None |
| **Mailpit Webmail** | `http://localhost:8025` | None |

### Option B: Run Locally with Maven & Local PostgreSQL
1. Ensure PostgreSQL is running on port 5432 with database `recruitment_db`.
2. Configure environment variables or `.env`:
   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/recruitment_db"
   $env:DB_USERNAME="recruitment_app"
   $env:DB_PASSWORD="your_password"
   ```
3. Run with Maven Wrapper:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

---

## 4. Default Seed Accounts

The database migrations (`V2__seed_roles_and_admin.sql`) provision seed credentials for immediate local testing:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Administrator** | `admin` | `Admin@12345` | Complete system access & API docs |
| **Recruiter** | *Register or create via admin* | - | Job posting, Kanban pipeline, offers |
| **Hiring Manager** | *Register or create via admin* | - | Interview evaluations & scorecards |
| **Applicant** | *Register via `/register`* | - | Job search, apply, accept/decline offers |

---

## 5. Verification & Test Execution

The project includes unit, service, repository, security, and REST controller tests:

```powershell
# Run all tests and generate JaCoCo coverage reports
.\mvnw.cmd clean test

# Package production executable JAR (skipping tests)
.\mvnw.cmd package -DskipTests
```

---

## 6. Infrastructure as Code (Azure Terraform)

To provision production Azure cloud infrastructure:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your Azure parameters and DB password

terraform init
terraform plan
terraform apply
```

Provisioned Azure Resources:
- **Resource Group:** Dedicated project environment.
- **Azure Database for PostgreSQL Flexible Server:** Highly available managed database.
- **Azure Linux App Service:** Hosts containerized TalentBridge image.
- **Azure Key Vault:** Secures database credentials using Managed Identity access.
- **Azure Storage Account:** Private blob container for candidate resumes.
- **Application Insights & Log Analytics:** Cloud telemetry and distributed tracing.

---

## 7. CI/CD Pipelines

- **GitHub Actions (`.github/workflows/ci.yml`):**
  - Java 21 build & verification with Maven wrapper.
  - Automated unit and integration testing.
  - JaCoCo test coverage report upload.
  - **Trivy container security scanning** (SARIF upload to GitHub Security tab).
  - Docker image smoke testing.
- **Azure DevOps (`azure-pipelines.yml`):**
  - Multi-stage pipeline covering Build, Test, ACR Push, and Deployment to Azure Web App Container.
