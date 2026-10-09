-- Credit system: costs, wallets, append-only ledger, packages, payments (payment-ready only).

CREATE TABLE credit_costs (
    action     text PRIMARY KEY,
    cost_units int NOT NULL CHECK (cost_units >= 0),
    is_active  boolean NOT NULL DEFAULT true
);

CREATE TABLE credit_wallets (
    user_id       uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    balance_units bigint NOT NULL DEFAULT 0 CHECK (balance_units >= 0),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE credit_ledger (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_type          text NOT NULL CHECK (entry_type IN ('GRANT_SIGNUP', 'GRANT_ADMIN', 'PURCHASE', 'SPEND', 'REFUND', 'ADJUSTMENT')),
    amount_units        bigint NOT NULL CHECK (amount_units <> 0),
    balance_after_units bigint NOT NULL CHECK (balance_after_units >= 0),
    action              text,
    ref_type            text,
    ref_id              uuid,
    related_entry_id    uuid REFERENCES credit_ledger (id),
    idempotency_key     text,
    note                text,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_credit_ledger_user_idempotency
    ON credit_ledger (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uq_credit_ledger_related_entry
    ON credit_ledger (related_entry_id)
    WHERE entry_type = 'REFUND';

CREATE INDEX idx_credit_ledger_user_created_at ON credit_ledger (user_id, created_at DESC);

CREATE TABLE credit_packages (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code          text NOT NULL UNIQUE,
    name          text NOT NULL,
    credits_units int NOT NULL CHECK (credits_units > 0),
    price_cents   int NOT NULL CHECK (price_cents >= 0),
    currency      char(3) NOT NULL DEFAULT 'USD',
    is_active     boolean NOT NULL DEFAULT true,
    sort_order    int NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- Unused until real payments are implemented; shape is webhook-ready.
CREATE TABLE payments (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    package_id         uuid NOT NULL REFERENCES credit_packages (id),
    provider           text NOT NULL,
    provider_payment_id text UNIQUE,
    status             text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    amount_cents       int NOT NULL CHECK (amount_cents >= 0),
    currency           char(3) NOT NULL DEFAULT 'USD',
    ledger_entry_id    uuid REFERENCES credit_ledger (id),
    raw_event          jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_user_id ON payments (user_id);
