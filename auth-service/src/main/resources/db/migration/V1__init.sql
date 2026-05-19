-- ServiceOS auth-service — initial schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone            VARCHAR(15)  NOT NULL UNIQUE,
    email            VARCHAR(255),
    name             VARCHAR(255),
    role             VARCHAR(30)  NOT NULL,
    password_hash    VARCHAR(255),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT users_role_check
        CHECK (role IN ('ADMIN', 'TECHNICIAN_HIRED', 'TECHNICIAN_FREE', 'CUSTOMER'))
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = TRUE;

CREATE TABLE otp_sessions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone        VARCHAR(15)  NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,
    purpose      VARCHAR(20)  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT otp_purpose_check
        CHECK (purpose IN ('LOGIN', 'REGISTER', 'RESET'))
);

CREATE INDEX idx_otp_phone_active ON otp_sessions(phone, used, expires_at);
CREATE INDEX idx_otp_expires_at ON otp_sessions(expires_at);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_active ON refresh_tokens(user_id, revoked, expires_at);
