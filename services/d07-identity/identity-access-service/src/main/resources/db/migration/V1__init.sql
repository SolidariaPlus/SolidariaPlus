CREATE TABLE subjects (
    sub        uuid PRIMARY KEY,
    member_id  uuid UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE member_accounts (
    member_id       uuid PRIMARY KEY,
    sub             uuid NOT NULL UNIQUE REFERENCES subjects (sub),
    member_number   text NOT NULL UNIQUE,
    password_hash   text,
    totp_secret_enc bytea,
    status          text NOT NULL CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'DISABLED'))
);

CREATE TABLE activation_codes (
    code_id    uuid PRIMARY KEY,
    member_id  uuid NOT NULL REFERENCES member_accounts (member_id),
    code_hash  text NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used       boolean NOT NULL DEFAULT false
);
CREATE INDEX activation_codes_member_id_idx ON activation_codes (member_id, created_at DESC);

CREATE TABLE staff_profiles (
    sub                    uuid PRIMARY KEY REFERENCES subjects (sub),
    directory_account      text NOT NULL UNIQUE,
    roles                  text[] NOT NULL,
    functions              text[] NOT NULL,
    totp_secret_enc        bytea,
    mfa_reset_at           timestamptz,
    mfa_reset_by           uuid,
    mfa_enrolment_deadline timestamptz
);

CREATE TABLE sessions (
    session_id uuid PRIMARY KEY,
    sub        uuid NOT NULL REFERENCES subjects (sub),
    created_at timestamptz NOT NULL,
    revoked_at timestamptz
);
CREATE INDEX sessions_sub_idx ON sessions (sub);

CREATE TABLE refresh_token_families (
    family_id          uuid PRIMARY KEY,
    session_id         uuid NOT NULL UNIQUE REFERENCES sessions (session_id),
    current_token_hash text NOT NULL
);

CREATE TABLE oauth2_authorization (
    id                            varchar(100) PRIMARY KEY,
    registered_client_id          varchar(100) NOT NULL,
    principal_name                varchar(200) NOT NULL,
    authorization_grant_type      varchar(100) NOT NULL,
    authorized_scopes             varchar(1000),
    attributes                    text,
    state                         varchar(500),
    authorization_code_value      text,
    authorization_code_issued_at  timestamptz,
    authorization_code_expires_at timestamptz,
    authorization_code_metadata   text,
    access_token_value            text,
    access_token_issued_at        timestamptz,
    access_token_expires_at       timestamptz,
    access_token_metadata         text,
    access_token_type             varchar(100),
    access_token_scopes           varchar(1000),
    oidc_id_token_value           text,
    oidc_id_token_issued_at       timestamptz,
    oidc_id_token_expires_at      timestamptz,
    oidc_id_token_metadata        text,
    refresh_token_value           text,
    refresh_token_issued_at       timestamptz,
    refresh_token_expires_at      timestamptz,
    refresh_token_metadata        text,
    user_code_value               text,
    user_code_issued_at           timestamptz,
    user_code_expires_at          timestamptz,
    user_code_metadata            text,
    device_code_value             text,
    device_code_issued_at         timestamptz,
    device_code_expires_at        timestamptz,
    device_code_metadata          text
);

CREATE TABLE outbox (
    event_id     uuid PRIMARY KEY,
    topic        text NOT NULL,
    event_key    text NOT NULL,
    event_type   text NOT NULL,
    payload      jsonb NOT NULL,
    occurred_at  timestamptz NOT NULL,
    published_at timestamptz
);
CREATE INDEX outbox_unpublished_idx ON outbox (occurred_at) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    event_id     uuid PRIMARY KEY,
    event_type   text NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now()
);
