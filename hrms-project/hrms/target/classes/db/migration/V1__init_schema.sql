

CREATE TYPE designation AS ENUM ('MASON', 'ELECTRICIAN', 'PLUMBER', 'SUPERVISOR', 'HELPER');
CREATE TYPE settlement_status AS ENUM ('PENDING', 'SETTLED');


CREATE TABLE workers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(15) NOT NULL UNIQUE,
    designation designation NOT NULL,
    daily_wage  NUMERIC(10, 2) NOT NULL CHECK (daily_wage > 0),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workers_active ON workers(active);
CREATE INDEX idx_workers_phone  ON workers(phone);


CREATE TABLE sites (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    location   VARCHAR(255) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sites_active ON sites(active);


CREATE TABLE attendance_logs (
    id             BIGSERIAL PRIMARY KEY,
    worker_id      BIGINT NOT NULL REFERENCES workers(id),
    site_id        BIGINT NOT NULL REFERENCES sites(id),
    clock_in       TIMESTAMPTZ NOT NULL,
    clock_out      TIMESTAMPTZ,
    total_hours    NUMERIC(5, 2),
    overtime_hours NUMERIC(5, 2) DEFAULT 0,
    flagged        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_clock_out_after_in CHECK (clock_out IS NULL OR clock_out > clock_in),
    CONSTRAINT chk_total_hours_positive CHECK (total_hours IS NULL OR total_hours > 0)
);

CREATE UNIQUE INDEX idx_attendance_active_clockin
    ON attendance_logs(worker_id)
    WHERE clock_out IS NULL;

CREATE INDEX idx_attendance_worker_id   ON attendance_logs(worker_id);
CREATE INDEX idx_attendance_site_id     ON attendance_logs(site_id);
CREATE INDEX idx_attendance_clock_in    ON attendance_logs(clock_in);
CREATE INDEX idx_attendance_worker_date ON attendance_logs(worker_id, clock_in);


CREATE TABLE overtime_entries (
    id                 BIGSERIAL PRIMARY KEY,
    worker_id          BIGINT NOT NULL REFERENCES workers(id),
    attendance_id      BIGINT NOT NULL REFERENCES attendance_logs(id),
    date               DATE NOT NULL,
    overtime_hours     NUMERIC(5, 2) NOT NULL CHECK (overtime_hours > 0),
    overtime_rate      NUMERIC(10, 2) NOT NULL,
    amount             NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    settlement_status  settlement_status NOT NULL DEFAULT 'PENDING',
    settled_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_overtime_per_attendance UNIQUE (attendance_id)
);

CREATE INDEX idx_overtime_worker_month ON overtime_entries(worker_id, date);
CREATE INDEX idx_overtime_status       ON overtime_entries(settlement_status);
