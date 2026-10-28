-- ============================================================
-- V1__initial_schema.sql
-- TalentBridge ATS — Full Initial Schema
-- ============================================================

-- ============================================================
-- USERS & ROLES
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(80)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(50)     NOT NULL UNIQUE   -- ADMIN, RECRUITER, HIRING_MANAGER, APPLICANT
);

CREATE TABLE user_roles (
    user_id     BIGINT  NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    role_id     BIGINT  NOT NULL REFERENCES roles(id)  ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- APPLICANT PROFILES
-- ============================================================
CREATE TABLE applicant_profiles (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    phone           VARCHAR(30),
    address         VARCHAR(500),
    linkedin_url    VARCHAR(500),
    portfolio_url   VARCHAR(500),
    summary         TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- JOBS
-- ============================================================
CREATE TABLE departments (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE jobs (
    id                  BIGSERIAL       PRIMARY KEY,
    title               VARCHAR(120)    NOT NULL,
    department_id       BIGINT          REFERENCES departments(id) ON DELETE SET NULL,
    location            VARCHAR(100)    NOT NULL,
    employment_type     VARCHAR(30)     NOT NULL DEFAULT 'FULL_TIME',
                        -- FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP
    salary_min          NUMERIC(12,2),
    salary_max          NUMERIC(12,2),
    description         TEXT            NOT NULL,
    requirements        TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
                        -- DRAFT | PUBLISHED | CLOSED | ARCHIVED
    created_by          BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMP WITH TIME ZONE,
    closing_date        DATE,
    CONSTRAINT chk_salary CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

CREATE INDEX idx_jobs_status     ON jobs(status);
CREATE INDEX idx_jobs_created_by ON jobs(created_by);

-- ============================================================
-- RESUMES
-- ============================================================
CREATE TABLE resumes (
    id              BIGSERIAL       PRIMARY KEY,
    applicant_id    BIGINT          NOT NULL REFERENCES applicant_profiles(id) ON DELETE CASCADE,
    original_name   VARCHAR(255)    NOT NULL,
    stored_path     VARCHAR(1000)   NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    uploaded_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_resumes_applicant ON resumes(applicant_id);

-- ============================================================
-- APPLICATIONS
-- ============================================================
CREATE TABLE applications (
    id              BIGSERIAL       PRIMARY KEY,
    job_id          BIGINT          NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    applicant_id    BIGINT          NOT NULL REFERENCES applicant_profiles(id) ON DELETE CASCADE,
    resume_id       BIGINT          REFERENCES resumes(id) ON DELETE SET NULL,
    cover_letter    TEXT,
    stage           VARCHAR(30)     NOT NULL DEFAULT 'APPLIED',
                    -- APPLIED | SHORTLISTED | INTERVIEW | OFFERED | HIRED | REJECTED | WITHDRAWN
    applied_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(job_id, applicant_id)
);

CREATE INDEX idx_applications_job       ON applications(job_id);
CREATE INDEX idx_applications_applicant ON applications(applicant_id);
CREATE INDEX idx_applications_stage     ON applications(stage);

-- ============================================================
-- APPLICATION STAGE HISTORY (immutable audit trail)
-- ============================================================
CREATE TABLE application_stage_history (
    id              BIGSERIAL       PRIMARY KEY,
    application_id  BIGINT          NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    from_stage      VARCHAR(30),
    to_stage        VARCHAR(30)     NOT NULL,
    changed_by      BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    notes           TEXT,
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stage_history_app ON application_stage_history(application_id);

-- ============================================================
-- INTERVIEWS
-- ============================================================
CREATE TABLE interviews (
    id              BIGSERIAL       PRIMARY KEY,
    application_id  BIGINT          NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    scheduled_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_mins   INTEGER         NOT NULL DEFAULT 60,
    location        VARCHAR(500),   -- room, video link, etc.
    interview_type  VARCHAR(30)     NOT NULL DEFAULT 'VIDEO',
                    -- VIDEO | IN_PERSON | PHONE | TECHNICAL
    interviewer_id  BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED',
                    -- SCHEDULED | COMPLETED | CANCELLED | RESCHEDULED
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interviews_application ON interviews(application_id);
CREATE INDEX idx_interviews_interviewer ON interviews(interviewer_id);
CREATE INDEX idx_interviews_scheduled   ON interviews(scheduled_at);

-- ============================================================
-- INTERVIEW FEEDBACK
-- ============================================================
CREATE TABLE interview_feedback (
    id              BIGSERIAL       PRIMARY KEY,
    interview_id    BIGINT          NOT NULL UNIQUE REFERENCES interviews(id) ON DELETE CASCADE,
    submitted_by    BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    rating          SMALLINT        CHECK (rating BETWEEN 1 AND 5),
    recommendation  VARCHAR(20),    -- STRONG_YES | YES | NEUTRAL | NO | STRONG_NO
    strengths       TEXT,
    concerns        TEXT,
    summary         TEXT,
    submitted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- OFFERS
-- ============================================================
CREATE TABLE offers (
    id              BIGSERIAL       PRIMARY KEY,
    application_id  BIGINT          NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    salary          NUMERIC(12,2)   NOT NULL,
    currency        CHAR(3)         NOT NULL DEFAULT 'INR',
    start_date      DATE,
    expiry_date     DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                    -- PENDING | ACCEPTED | DECLINED | EXPIRED | REVOKED
    notes           TEXT,
    created_by      BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255)    NOT NULL,
    message         TEXT            NOT NULL,
    link            VARCHAR(500),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, is_read);

-- ============================================================
-- AUDIT EVENTS
-- ============================================================
CREATE TABLE audit_events (
    id              BIGSERIAL       PRIMARY KEY,
    actor_id        BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    actor_username  VARCHAR(80),
    event_type      VARCHAR(80)     NOT NULL,
    entity_type     VARCHAR(80),
    entity_id       BIGINT,
    detail          TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor      ON audit_events(actor_id);
CREATE INDEX idx_audit_created    ON audit_events(created_at DESC);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
