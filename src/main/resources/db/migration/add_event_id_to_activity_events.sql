-- Migration: Add event_id to activity_events table safely
-- 1. Add event_id column as nullable initially so existing records are unaffected
ALTER TABLE activity_events ADD COLUMN IF NOT EXISTS event_id VARCHAR(255);

-- 2. Backfill existing records with unique UUIDs
UPDATE activity_events SET event_id = gen_random_uuid()::text WHERE event_id IS NULL;

-- 3. Enforce NOT NULL constraint now that all existing rows are populated
ALTER TABLE activity_events ALTER COLUMN event_id SET NOT NULL;

-- 4. Add UNIQUE constraint to enforce idempotency
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_activity_events_event_id'
    ) THEN
        ALTER TABLE activity_events ADD CONSTRAINT uk_activity_events_event_id UNIQUE (event_id);
    END IF;
END $$;
