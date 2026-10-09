-- Deep validation runs (re-validate action): tracked so the UI can poll and
-- failures can refund exactly once.
CREATE TABLE validation_runs (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idea_id          uuid NOT NULL REFERENCES ideas (id) ON DELETE CASCADE,
    user_id          uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    version_id       uuid NOT NULL REFERENCES idea_versions (id) ON DELETE CASCADE,
    status           text NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    spend_ledger_id  uuid REFERENCES credit_ledger (id),
    refund_ledger_id uuid REFERENCES credit_ledger (id),
    error_code       text,
    created_at       timestamptz NOT NULL DEFAULT now(),
    completed_at     timestamptz
);

CREATE INDEX idx_validation_runs_idea_created_at ON validation_runs (idea_id, created_at DESC);

-- At most one active revalidation per idea.
CREATE UNIQUE INDEX uq_validation_runs_active ON validation_runs (idea_id) WHERE status = 'RUNNING';
