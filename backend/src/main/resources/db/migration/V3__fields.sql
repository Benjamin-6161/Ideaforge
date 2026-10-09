-- Business fields (top-level) and sub-niches (children).

CREATE TABLE fields (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   uuid REFERENCES fields (id) ON DELETE CASCADE,
    slug        text NOT NULL UNIQUE,
    name        text NOT NULL,
    description text NOT NULL DEFAULT '',
    icon        text NOT NULL DEFAULT '',
    sort_order  int NOT NULL DEFAULT 0,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_fields_parent_id ON fields (parent_id);
CREATE INDEX idx_fields_active_sort ON fields (is_active, sort_order);
