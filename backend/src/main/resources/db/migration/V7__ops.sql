-- Operational tables: search cache and AI usage logs.

CREATE TABLE search_cache (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider   text NOT NULL,
    query_hash text NOT NULL UNIQUE,
    query      text NOT NULL,
    results    jsonb NOT NULL DEFAULT '[]'::jsonb,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE INDEX idx_search_cache_expires_at ON search_cache (expires_at);

CREATE TABLE ai_usage_logs (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid REFERENCES users (id) ON DELETE SET NULL,
    purpose             text NOT NULL CHECK (purpose IN ('IDEATION', 'SCORING', 'CHAT', 'DEEP_VALIDATION', 'FINALIZE')),
    provider            text NOT NULL,
    model               text NOT NULL,
    tokens_in           int NOT NULL DEFAULT 0,
    tokens_out          int NOT NULL DEFAULT 0,
    latency_ms          int NOT NULL DEFAULT 0,
    est_cost_micro_usd  bigint NOT NULL DEFAULT 0,
    success             boolean NOT NULL DEFAULT true,
    ref_type            text,
    ref_id              uuid,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_logs_created_at ON ai_usage_logs (created_at DESC);
CREATE INDEX idx_ai_usage_logs_purpose_created_at ON ai_usage_logs (purpose, created_at DESC);
