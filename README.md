# TalentBridge Enterprise ATS

TalentBridge is an enterprise-grade Recruitment and Applicant Tracking System (ATS) engineered with Spring Boot 3, Java 21, PostgreSQL, and a comprehensive enterprise DevOps ecosystem.

---

## 1. Project Overview & Architecture

TalentBridge streamlines the entire recruitment lifecycle:
- **Roles:** Administrator (`ADMIN`), Recruiter (`RECRUITER`), Hiring Manager (`HIRING_MANAGER`), Applicant (`APPLICANT`).
- **Core Workflows:** Job requisition management, candidate application submissions, private resume storage, recruitment stage transitions (`APPLIED → SHORTLISTED → INTERVIEW → OFFERED → HIRED`), interview scheduling, offer management, notifications, and analytics dashboards.

---

## 2. Technology Stack

- **Java Runtime:** Java 21 LTS (OpenJDK Temurin 21.0.12.1+)
- **Application Framework:** Spring Boot 3.5.7
- **Persistence:** Spring Data JPA, Hibernate ORM 6.6
- **Database:** PostgreSQL (with Flyway migration management)
- **Observability:** Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5
- **Build Tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Testing:** JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (test-scope in-memory profile)

---

## 3. Local Development Setup

### Prerequisites
1. **Java 21**: Verify with `java -version`
2. **PostgreSQL 16+**: Running locally on port `5432` with database `recruitment_db` and user `recruitment_app`

### Environment Configuration
1. Copy `.env.example` to `.env` (or set environment variables in your shell):
   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/recruitment_db"
   $env:DB_USERNAME="recruitment_app"
   $env:DB_PASSWORD="your_database_password"
   ```
2. Note: For unit and integration tests, an isolated in-memory H2 database is configured automatically in `src/test/resources/application.properties`. No external database is needed to run `.\mvnw.cmd test`.

---

## 4. Build and Test Verification

Run all test suites:
```powershell
.\mvnw.cmd test
```

Build executable JAR package:
```powershell
.\mvnw.cmd package
```

Run application locally:
```powershell
.\mvnw.cmd spring-boot:run
```

Access the application in your browser:
- Application UI: `http://localhost:8080/jobs`
- Health Check: `http://localhost:8080/actuator/health`
