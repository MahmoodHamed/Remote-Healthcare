-- TimescaleDB initialization script
-- Runs once on first container start after the database is created.

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Convert VitalRecords into a hypertable (time-series optimised)
-- This runs AFTER EF Core migrations create the table.
-- Wrap in a DO block so it is idempotent.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'VitalRecords'
    ) THEN
        PERFORM create_hypertable('"VitalRecords"', '"RecordedAt"',
            if_not_exists => TRUE,
            migrate_data  => TRUE);
    END IF;
END;
$$;
