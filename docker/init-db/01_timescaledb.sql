-- TimescaleDB initialization script
-- Runs once on first container start after the database is created.

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Convert VitalRecords into a hypertable (time-series optimised).
-- EF Core creates the table as "VitalRecords" (quoted PascalCase in PostgreSQL).
-- We try both casing variants to be robust across migration versions.
DO $$
DECLARE
    tbl text;
BEGIN
    -- Prefer the EF-generated PascalCase name; fall back to snake_case.
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'VitalRecords'
    ) THEN
        tbl := '"VitalRecords"';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'vital_records'
    ) THEN
        tbl := 'vital_records';
    ELSE
        RAISE NOTICE 'VitalRecords table not found yet — hypertable conversion skipped.';
        RETURN;
    END IF;

    PERFORM create_hypertable(tbl, 'RecordedAt',
        if_not_exists => TRUE,
        migrate_data  => TRUE);
    RAISE NOTICE 'Hypertable created on %', tbl;

    -- Compress chunks older than 7 days to save storage.
    EXECUTE format('ALTER TABLE %s SET (
        timescaledb.compress,
        timescaledb.compress_orderby = ''"RecordedAt" DESC'',
        timescaledb.compress_segmentby = ''"PatientId"''
    )', tbl);

    PERFORM add_compression_policy(tbl, INTERVAL '7 days', if_not_exists => TRUE);
END;
$$;
