-- ============================================================
-- V2__seed_roles_and_admin.sql
-- Seed: required roles + default admin user
-- Admin password is 'Admin@123!' BCrypt-hashed below
-- CHANGE THIS PASSWORD before any production deployment!
-- ============================================================

INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('RECRUITER'),
    ('HIRING_MANAGER'),
    ('APPLICANT')
ON CONFLICT (name) DO NOTHING;

-- Default admin user — password hash = Admin@123!
-- Generated with BCrypt strength 12
INSERT INTO users (username, email, password_hash, enabled)
VALUES (
    'admin',
    'admin@talentbridge.local',
    '$2a$12$0VMe5b0QdxMMI2dFHASgVuKQAXvcJCHKsB2frhu56XkB/gLGnfYz6',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

-- Grant ADMIN role to the default admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Seed departments
INSERT INTO departments (name) VALUES
    ('Engineering'),
    ('Product Management'),
    ('Design'),
    ('Marketing'),
    ('Sales'),
    ('Human Resources'),
    ('Finance'),
    ('Operations'),
    ('Data & Analytics'),
    ('Customer Success')
ON CONFLICT (name) DO NOTHING;
