-- Generation runs, ideas, immutable versions, validations.

CREATE TABLE generation_runs (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    field_id        uuid NOT NULL REFERENCES fields (id),
    niche           text,
    parameters      jsonb NOT NULL DEFAULT '{}'::jsonb,
    status          text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    progress_stage  text CHECK (progress_stage IN ('IDEATING', 'RESEARCHING', 'SCORING', 'DONE')),
    error_code      text,
    spend_ledger_id uuid REFERENCES credit_ledger (id),
    refund_ledger_id uuid REFERENCES credit_ledger (id),
    prompt_version  text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    started_at      timestamptz,
    completed_at    timestamptz
);

CREATE INDEX idx_generation_runs_user_created_at ON generation_runs (user_id, created_at DESC);
CREATE INDEX idx_generation_runs_status ON generation_runs (status);

CREATE TABLE ideas (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    generation_run_id  uuid REFERENCES generation_runs (id) ON DELETE SET NULL,
    field_id           uuid NOT NULL REFERENCES fields (id),
    niche              text,
    status             text NOT NULL DEFAULT 'GENERATED' CHECK (status IN ('GENERATED', 'IN_PROGRESS', 'FINALIZED')),
    is_favorite        boolean NOT NULL DEFAULT false,
    is_archived        boolean NOT NULL DEFAULT false,
    title              text NOT NULL,
    tagline            text NOT NULL DEFAULT '',
    current_version_id uuid,
    message_count      int NOT NULL DEFAULT 0 CHECK (message_count >= 0),
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    finalized_at       timestamptz,
    deleted_at         timestamptz
);

CREATE INDEX idx_ideas_user_field_status_created_at ON ideas (user_id, field_id, status, created_at DESC);
CREATE INDEX idx_ideas_user_favorite ON ideas (user_id, is_favorite);
CREATE INDEX idx_ideas_generation_run_id ON ideas (generation_run_id);
CREATE INDEX idx_ideas_field_id ON ideas (field_id);

CREATE TABLE idea_versions (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idea_id                 uuid NOT NULL REFERENCES ideas (id) ON DELETE CASCADE,
    version_number          int NOT NULL CHECK (version_number > 0),
    source                  text NOT NULL CHECK (source IN ('GENERATION', 'CHAT', 'RESTORE')),
    schema_version          int NOT NULL DEFAULT 1,
    content                 jsonb NOT NULL,
    change_summary          text,
    created_from_message_id uuid,
    created_at              timestamptz NOT NULL DEFAULT now(),
    UNIQUE (idea_id, version_number)
);

CREATE INDEX idx_idea_versions_idea_id ON idea_versions (idea_id, version_number DESC);

ALTER TABLE ideas
    ADD CONSTRAINT fk_ideas_current_version
    FOREIGN KEY (current_version_id) REFERENCES idea_versions (id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE idea_validations (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idea_id             uuid NOT NULL REFERENCES ideas (id) ON DELETE CASCADE,
    version_id          uuid NOT NULL REFERENCES idea_versions (id) ON DELETE CASCADE,
    depth               text NOT NULL CHECK (depth IN ('LIGHT', 'DEEP')),
    demand_score        smallint NOT NULL CHECK (demand_score BETWEEN 1 AND 10),
    profitability_score smallint NOT NULL CHECK (profitability_score BETWEEN 1 AND 10),
    competition_score   smallint NOT NULL CHECK (competition_score BETWEEN 1 AND 10),
    novelty_score       smallint NOT NULL CHECK (novelty_score BETWEEN 1 AND 10),
    evidence_available  boolean NOT NULL DEFAULT false,
    rationale           jsonb NOT NULL DEFAULT '{}'::jsonb,
    competitors         jsonb NOT NULL DEFAULT '[]'::jsonb,
    evidence            jsonb NOT NULL DEFAULT '[]'::jsonb,
    queries             jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_idea_validations_idea_created_at ON idea_validations (idea_id, created_at DESC);
CREATE INDEX idx_idea_validations_version_id ON idea_validations (version_id);
