-- Student discounts.
--
-- A student may have one discount in force at a time: either a flat som amount off each billing-plan
-- charge (FIXED) or a share of it (PERCENT). It runs for duration_months from start_date, or forever
-- when duration_months / end_date are null.
--
-- ddl-auto is "none", so this has to be applied by hand before deploying the code.

CREATE TABLE IF NOT EXISTS student_discounts
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID        NOT NULL UNIQUE,
    student_id      BIGINT      NOT NULL REFERENCES students (id),
    type            VARCHAR(16) NOT NULL,
    amount          BIGINT,
    percent         INTEGER,
    start_date      DATE        NOT NULL,
    duration_months INTEGER,
    end_date        DATE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    note            TEXT,
    created_by      BIGINT REFERENCES users (id),
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now()
);

-- Resolving "the discount in force for this student today" is on every billing-plan charge.
CREATE INDEX IF NOT EXISTS idx_student_discounts_student ON student_discounts (student_id);
CREATE INDEX IF NOT EXISTS idx_student_discounts_window ON student_discounts (student_id, is_active, start_date, end_date);

-- The part of the full price a student was let off on this transaction, always >= 0. amount stays what
-- actually moved on the balance, so the undiscounted price of a charge is (-amount + discount_amount).
-- Teacher payroll reads this column to keep paying on the full price. Everything already in the table
-- was charged without a discount, hence the 0 default.
ALTER TABLE finance_transactions
    ADD COLUMN IF NOT EXISTS discount_amount BIGINT NOT NULL DEFAULT 0;
