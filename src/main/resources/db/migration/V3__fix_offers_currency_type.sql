-- ============================================================
-- V3: Fix offers.currency column type from CHAR(3) to VARCHAR(3)
-- to align with Hibernate 6 JPA schema validation.
-- ============================================================
ALTER TABLE offers ALTER COLUMN currency TYPE VARCHAR(3);
