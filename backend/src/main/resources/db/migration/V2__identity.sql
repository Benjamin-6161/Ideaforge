-- Identity, authentication, and abuse-tracking tables.

CREATE TABLE users (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email             text NOT NULL,
    email_normalized  text NOT NULL UNIQUE,
    display_name      text,
    avatar_url        text,
    role              text NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    status            text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    email_verified_at timestamptz,
    signup_ip_hash    text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    last_login_at     timestamptz
);

CREATE INDEX idx_users_signup_ip_hash_created_at ON users (signup_ip_hash, created_at);

CREATE TABLE user_identities (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         text NOT NULL CHECK (provider IN ('EMAIL_OTP', 'GOOGLE')),
    provider_subject text NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);

CREATE TABLE otp_challenges (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email_normalized text NOT NULL,
    code_hash        text NOT NULL,
    attempts         int NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    max_attempts     int NOT NULL DEFAULT 5 CHECK (max_attempts > 0),
    expires_at       timestamptz NOT NULL,
    consumed_at      timestamptz,
    ip_hash          text,
    created_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_challenges_email_created_at ON otp_challenges (email_normalized, created_at DESC);

CREATE TABLE auth_sessions (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   text NOT NULL UNIQUE,
    ip_hash      text,
    user_agent   text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    expires_at   timestamptz NOT NULL,
    revoked_at   timestamptz
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions (expires_at);

CREATE TABLE security_events (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    type       text NOT NULL,
    user_id    uuid REFERENCES users (id) ON DELETE SET NULL,
    ip_hash    text,
    details    jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_events_type_created_at ON security_events (type, created_at DESC);
