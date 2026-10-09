-- Chat messages and finalizations.

CREATE TABLE chat_messages (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idea_id             uuid NOT NULL REFERENCES ideas (id) ON DELETE CASCADE,
    user_id             uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role                text NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content             text NOT NULL,
    status              text NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED')),
    resulting_version_id uuid REFERENCES idea_versions (id) ON DELETE SET NULL,
    spend_ledger_id     uuid REFERENCES credit_ledger (id),
    refund_ledger_id    uuid REFERENCES credit_ledger (id),
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_idea_created_at ON chat_messages (idea_id, created_at);
CREATE INDEX idx_chat_messages_user_id ON chat_messages (user_id);

CREATE TABLE finalizations (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    idea_id         uuid NOT NULL UNIQUE REFERENCES ideas (id) ON DELETE CASCADE,
    version_id      uuid NOT NULL REFERENCES idea_versions (id) ON DELETE CASCADE,
    status          text NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    output          jsonb,
    build_prompt_md text,
    agents_md       text,
    spend_ledger_id uuid REFERENCES credit_ledger (id),
    refund_ledger_id uuid REFERENCES credit_ledger (id),
    prompt_version  text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    completed_at    timestamptz
);
