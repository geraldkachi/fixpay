-- V25: Add join table for WebhookEndpoint.events element collection
-- Required after moving events mapping from TEXT[] column to @ElementCollection.

CREATE TABLE IF NOT EXISTS webhook_endpoint_events (
    webhook_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    event      VARCHAR(120) NOT NULL,
    PRIMARY KEY (webhook_id, event)
);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoint_events_webhook ON webhook_endpoint_events(webhook_id);

-- Backfill from legacy webhook_endpoints.events array if data exists.
INSERT INTO webhook_endpoint_events (webhook_id, event)
SELECT w.id, e.event
FROM webhook_endpoints w
CROSS JOIN LATERAL unnest(COALESCE(w.events, ARRAY[]::TEXT[])) AS e(event)
ON CONFLICT (webhook_id, event) DO NOTHING;
