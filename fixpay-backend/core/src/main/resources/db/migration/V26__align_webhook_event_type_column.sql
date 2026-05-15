-- V26: Align webhook_endpoint_events schema with JPA mapping (@Column(name = "event_type"))

ALTER TABLE webhook_endpoint_events
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(120);

-- Backfill from legacy column name if present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'webhook_endpoint_events'
          AND column_name = 'event'
    ) THEN
        UPDATE webhook_endpoint_events
        SET event_type = event
        WHERE event_type IS NULL;
    END IF;
END $$;

ALTER TABLE webhook_endpoint_events
    ALTER COLUMN event_type SET NOT NULL;

ALTER TABLE webhook_endpoint_events
    DROP CONSTRAINT IF EXISTS webhook_endpoint_events_pkey;

ALTER TABLE webhook_endpoint_events
    ADD CONSTRAINT webhook_endpoint_events_pkey PRIMARY KEY (webhook_id, event_type);

-- Keep compatibility with prior schema, then clean up legacy column.
ALTER TABLE webhook_endpoint_events
    DROP COLUMN IF EXISTS event;
