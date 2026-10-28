# TalentBridge Enterprise ATS — Architecture & Engineering Specification

## 1. Domain Model & Entity Relationships

The TalentBridge domain is modeled around high-volume recruitment workflows with strict transactional integrity:

```
[User] 1 <---> * [Role]
  |
  +-- 1:1 --> [ApplicantProfile]
                 |
                 +-- 1:* --> [Application] <--- * [Job] <--- 1 [Department]
                                |     |
                                |     +-- 1:* --> [ApplicationStageHistory]
                                |
                                +-- 1:* --> [Interview] <--- 1:1 [InterviewFeedback]
                                |
                                +-- 1:1 --> [Offer]
                                |
                                +-- * [Resume]
```

### Key Domain Principles:
1. **Audit Immutability:** `ApplicationStageHistory` and `AuditEvent` records are write-only. Stage histories use `Propagation.REQUIRES_NEW` via `AuditService` so audit entries persist even if an outer business transaction rolls back.
2. **Duplicate Application Prevention:** A database unique constraint on `(job_id, applicant_id)` guarantees idempotent application submission under concurrent requests.
3. **Storage Abstraction:** The `StorageService` interface separates domain logic from storage implementation (`LocalStorageService` for on-prem/local, extensible to `AzureBlobStorageService`). Files are renamed with secure UUIDs and content types validated against strict MIME allowlists (`application/pdf`, `msword`, `docx`).

---

## 2. Security & Authorization Architecture

TalentBridge enforces Defense-in-Depth security:

- **Authentication:** Form-based login with BCrypt password hashing (cost factor 12) + HTTP Basic authentication for REST API endpoints.
- **CSRF Protection:** Synchronizer token pattern enabled for all browser state-mutating requests (`POST`, `PUT`, `DELETE`), explicitly excluded for stateless `/api/**` REST routes.
- **Security Headers:**
  - `X-Frame-Options: SAMEORIGIN`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- **Method-Level Security:** `@PreAuthorize("hasRole('RECRUITER')")` on critical domain operations and stage mutations.
- **Resource Ownership Verification:** Resumes and offer letters verify that the requesting principal either owns the resource or holds privileged recruiter/admin roles.

---

## 3. Observability Architecture

- **Prometheus Metric Collection:**
  - Scrapes Spring Boot Actuator at `/actuator/prometheus`.
  - Exposes RED metrics (Rate, Errors, Duration) via Micrometer HTTP request meters.
  - Monitors JVM GC frequency, pause time distributions, and heap/non-heap allocation.
  - Monitors HikariCP pool health (`hikaricp_connections_active`, `hikaricp_connections_idle`, `hikaricp_connections_pending`).
- **Grafana Provisioning:**
  - Automated provisioning via YAML files in `/etc/grafana/provisioning/` ensures zero-touch dashboard deployment on cluster start.
