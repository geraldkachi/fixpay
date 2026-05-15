-- V24: Add join table for ApiKey.scopes element collection
-- Required after moving scopes mapping from TEXT[] column to @ElementCollection.

CREATE TABLE IF NOT EXISTS api_key_scopes (
    api_key_id UUID NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    scope      VARCHAR(120) NOT NULL,
    PRIMARY KEY (api_key_id, scope)
);

CREATE INDEX IF NOT EXISTS idx_api_key_scopes_key ON api_key_scopes(api_key_id);

-- Backfill from legacy api_keys.scopes array if data exists.
INSERT INTO api_key_scopes (api_key_id, scope)
SELECT ak.id, s.scope
FROM api_keys ak
CROSS JOIN LATERAL unnest(COALESCE(ak.scopes, ARRAY[]::TEXT[])) AS s(scope)
ON CONFLICT (api_key_id, scope) DO NOTHING;
