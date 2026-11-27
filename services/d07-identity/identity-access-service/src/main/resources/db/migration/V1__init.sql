-- Schema of MS-17 identity-access-service (Data Model · D-07 Identity and Access, KAN-16).
-- Classification marks in the comments: [P] personal data, [S] secret (KAN-16 §4.1).

CREATE TABLE subjects (
    sub        uuid PRIMARY KEY,
    member_id  uuid UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE subjects IS 'Opaque subjects used in tokens and in every other service.';
COMMENT ON COLUMN subjects.sub IS 'Opaque subject identifier (UUIDv7), the sub claim of every token.';
COMMENT ON COLUMN subjects.member_id IS 'Member of MS-01 behind the subject; null for staff.';
COMMENT ON COLUMN subjects.created_at IS 'Instant the subject was created (UTC).';

CREATE TABLE member_accounts (
    member_id       uuid PRIMARY KEY,
    sub             uuid NOT NULL UNIQUE REFERENCES subjects (sub),
    member_number   text NOT NULL UNIQUE,
    password_hash   text,
    totp_secret_enc bytea,
    status          text NOT NULL CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'DISABLED'))
);
COMMENT ON TABLE member_accounts IS 'Member accounts, created on MemberAdmitted.';
COMMENT ON COLUMN member_accounts.member_id IS 'Member of MS-01 (MemberAdmitted).';
COMMENT ON COLUMN member_accounts.sub IS 'Subject of the member.';
COMMENT ON COLUMN member_accounts.member_number IS 'Member number the member signs in with.';
COMMENT ON COLUMN member_accounts.password_hash IS '[S] Argon2id hash of the password; null until activation.';
COMMENT ON COLUMN member_accounts.totp_secret_enc IS '[S] TOTP secret, AWS Encryption SDK ciphertext under the KMS key.';
COMMENT ON COLUMN member_accounts.status IS 'PENDING_ACTIVATION, ACTIVE or DISABLED (MembershipEnded).';

CREATE TABLE activation_codes (
    code_id    uuid PRIMARY KEY,
    member_id  uuid NOT NULL REFERENCES member_accounts (member_id),
    code_hash  text NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used       boolean NOT NULL DEFAULT false
);
CREATE INDEX activation_codes_member_id_idx ON activation_codes (member_id, created_at DESC);
COMMENT ON TABLE activation_codes IS 'One-time activation codes, valid 72 hours (RF-17-04).';
COMMENT ON COLUMN activation_codes.code_id IS 'Activation code identifier.';
COMMENT ON COLUMN activation_codes.member_id IS 'Member the code activates.';
COMMENT ON COLUMN activation_codes.code_hash IS '[S] Argon2id hash of the code.';
COMMENT ON COLUMN activation_codes.created_at IS 'Instant the teller issued the code (UTC).';
COMMENT ON COLUMN activation_codes.expires_at IS 'Instant the code expires, 72 hours after issue (UTC).';
COMMENT ON COLUMN activation_codes.used IS 'True once the code activated the account.';

CREATE TABLE staff_profiles (
    sub                    uuid PRIMARY KEY REFERENCES subjects (sub),
    directory_account      text NOT NULL UNIQUE,
    roles                  text[] NOT NULL,
    functions              text[] NOT NULL,
    totp_secret_enc        bytea,
    mfa_reset_at           timestamptz,
    mfa_reset_by           uuid,
    mfa_enrolment_deadline timestamptz,
    CONSTRAINT staff_profiles_mfa_reset_check CHECK (
        (mfa_reset_at IS NULL AND mfa_reset_by IS NULL AND mfa_enrolment_deadline IS NULL)
        OR (mfa_reset_at IS NOT NULL AND mfa_reset_by IS NOT NULL AND mfa_enrolment_deadline IS NOT NULL))
);
COMMENT ON TABLE staff_profiles IS 'Staff linked to their directory account; roles derived from groups (ADR-084).';
COMMENT ON COLUMN staff_profiles.sub IS 'Subject of the staff member.';
COMMENT ON COLUMN staff_profiles.directory_account IS '[P] Active Directory account.';
COMMENT ON COLUMN staff_profiles.roles IS 'Roles derived from the directory groups at the last sign-in.';
COMMENT ON COLUMN staff_profiles.functions IS 'Board functions (PRESIDENT, COMPLIANCE) derived from the directory groups.';
COMMENT ON COLUMN staff_profiles.totp_secret_enc IS '[S] TOTP secret, AWS Encryption SDK ciphertext under the KMS key.';
COMMENT ON COLUMN staff_profiles.mfa_reset_at IS 'Instant of the pending in-person authenticator reset (ADR-126); null once a new authenticator is enrolled.';
COMMENT ON COLUMN staff_profiles.mfa_reset_by IS 'Subject of the Board member who reset the authenticator (ADR-126); set with mfa_reset_at.';
COMMENT ON COLUMN staff_profiles.mfa_enrolment_deadline IS 'Deadline to enrol a new authenticator, 24 hours after the reset (ADR-126); set with mfa_reset_at.';

CREATE TABLE sessions (
    session_id uuid PRIMARY KEY,
    sub        uuid NOT NULL REFERENCES subjects (sub),
    created_at timestamptz NOT NULL,
    revoked_at timestamptz
);
CREATE INDEX sessions_sub_idx ON sessions (sub);
COMMENT ON TABLE sessions IS 'Sessions and their revocation (RF-17-06 to RF-17-09).';
COMMENT ON COLUMN sessions.session_id IS 'Session identifier, the sid claim; equals the oauth2_authorization id.';
COMMENT ON COLUMN sessions.sub IS 'Subject of the session.';
COMMENT ON COLUMN sessions.created_at IS 'Instant the session started (UTC).';
COMMENT ON COLUMN sessions.revoked_at IS 'Instant the session was revoked (UTC); null while active.';

CREATE TABLE refresh_token_families (
    family_id          uuid PRIMARY KEY,
    session_id         uuid NOT NULL UNIQUE REFERENCES sessions (session_id),
    current_token_hash text NOT NULL
);
COMMENT ON TABLE refresh_token_families IS 'Refresh token rotation with reuse detection: reuse revokes the session.';
COMMENT ON COLUMN refresh_token_families.family_id IS 'Refresh token family identifier.';
COMMENT ON COLUMN refresh_token_families.session_id IS 'Session of the family, one family per session.';
COMMENT ON COLUMN refresh_token_families.current_token_hash IS '[S] SHA-256 of the current refresh token.';

-- Default JDBC schema of Spring Authorization Server, PostgreSQL variant (text instead of blob).
CREATE TABLE oauth2_registered_client (
    id                            varchar(100) PRIMARY KEY,
    client_id                     varchar(100) NOT NULL UNIQUE,
    client_id_issued_at           timestamptz NOT NULL DEFAULT now(),
    client_secret                 varchar(200),
    client_secret_expires_at      timestamptz,
    client_name                   varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types     varchar(1000) NOT NULL,
    redirect_uris                 varchar(1000),
    post_logout_redirect_uris     varchar(1000),
    scopes                        varchar(1000) NOT NULL,
    client_settings               varchar(2000) NOT NULL,
    token_settings                varchar(2000) NOT NULL
);
COMMENT ON TABLE oauth2_registered_client IS 'Spring Authorization Server registered clients, synchronised from application.yml at start-up.';
COMMENT ON COLUMN oauth2_registered_client.id IS 'Registration identifier.';
COMMENT ON COLUMN oauth2_registered_client.client_id IS 'OAuth client_id.';
COMMENT ON COLUMN oauth2_registered_client.client_id_issued_at IS 'Instant the client was registered (UTC).';
COMMENT ON COLUMN oauth2_registered_client.client_secret IS '[S] Argon2id hash of the client secret of a service (ADR-124); null for public clients.';
COMMENT ON COLUMN oauth2_registered_client.client_secret_expires_at IS 'Instant the client secret expires (UTC); null when it does not.';
COMMENT ON COLUMN oauth2_registered_client.client_name IS 'Display name of the client.';
COMMENT ON COLUMN oauth2_registered_client.client_authentication_methods IS 'Comma-separated client authentication methods.';
COMMENT ON COLUMN oauth2_registered_client.authorization_grant_types IS 'Comma-separated grant types.';
COMMENT ON COLUMN oauth2_registered_client.redirect_uris IS 'Comma-separated redirect URIs.';
COMMENT ON COLUMN oauth2_registered_client.post_logout_redirect_uris IS 'Comma-separated post-logout redirect URIs.';
COMMENT ON COLUMN oauth2_registered_client.scopes IS 'Comma-separated scopes.';
COMMENT ON COLUMN oauth2_registered_client.client_settings IS 'Client settings as JSON (PKCE, consent).';
COMMENT ON COLUMN oauth2_registered_client.token_settings IS 'Token settings as JSON (lifetimes, rotation, ID token algorithm).';

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
COMMENT ON TABLE oauth2_authorization IS 'Spring Authorization Server authorizations; the id is the session identifier.';
COMMENT ON COLUMN oauth2_authorization.id IS 'Authorization identifier, equal to sessions.session_id.';
COMMENT ON COLUMN oauth2_authorization.registered_client_id IS 'Registered client (oauth2_registered_client.id).';
COMMENT ON COLUMN oauth2_authorization.principal_name IS 'Subject of the authorization.';
COMMENT ON COLUMN oauth2_authorization.authorization_grant_type IS 'Grant type that created the authorization.';
COMMENT ON COLUMN oauth2_authorization.authorized_scopes IS 'Comma-separated authorized scopes.';
COMMENT ON COLUMN oauth2_authorization.attributes IS 'Authorization attributes as JSON (request, principal).';
COMMENT ON COLUMN oauth2_authorization.state IS 'OAuth state of a pending authorization request.';
COMMENT ON COLUMN oauth2_authorization.authorization_code_value IS '[S] Authorization code.';
COMMENT ON COLUMN oauth2_authorization.authorization_code_issued_at IS 'Instant the authorization code was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.authorization_code_expires_at IS 'Instant the authorization code expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.authorization_code_metadata IS 'Authorization code metadata as JSON.';
COMMENT ON COLUMN oauth2_authorization.access_token_value IS '[S] Access token.';
COMMENT ON COLUMN oauth2_authorization.access_token_issued_at IS 'Instant the access token was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.access_token_expires_at IS 'Instant the access token expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.access_token_metadata IS 'Access token metadata and claims as JSON.';
COMMENT ON COLUMN oauth2_authorization.access_token_type IS 'Access token type (Bearer).';
COMMENT ON COLUMN oauth2_authorization.access_token_scopes IS 'Comma-separated access token scopes.';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_value IS '[S] OpenID Connect ID token.';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_issued_at IS 'Instant the ID token was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_expires_at IS 'Instant the ID token expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.oidc_id_token_metadata IS 'ID token metadata and claims as JSON.';
COMMENT ON COLUMN oauth2_authorization.refresh_token_value IS '[S] Refresh token (<sid>.<random>); reuse is detected with refresh_token_families.';
COMMENT ON COLUMN oauth2_authorization.refresh_token_issued_at IS 'Instant the refresh token was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.refresh_token_expires_at IS 'Instant the refresh token expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.refresh_token_metadata IS 'Refresh token metadata as JSON.';
COMMENT ON COLUMN oauth2_authorization.user_code_value IS '[S] Device flow user code (unused by the current clients).';
COMMENT ON COLUMN oauth2_authorization.user_code_issued_at IS 'Instant the user code was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.user_code_expires_at IS 'Instant the user code expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.user_code_metadata IS 'User code metadata as JSON.';
COMMENT ON COLUMN oauth2_authorization.device_code_value IS '[S] Device flow device code (unused by the current clients).';
COMMENT ON COLUMN oauth2_authorization.device_code_issued_at IS 'Instant the device code was issued (UTC).';
COMMENT ON COLUMN oauth2_authorization.device_code_expires_at IS 'Instant the device code expires (UTC).';
COMMENT ON COLUMN oauth2_authorization.device_code_metadata IS 'Device code metadata as JSON.';

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
COMMENT ON TABLE outbox IS 'Events written in the business transaction and relayed to Kafka (ADR-045, KAN-16 §6.1).';
COMMENT ON COLUMN outbox.event_id IS 'CloudEvents id.';
COMMENT ON COLUMN outbox.topic IS 'Kafka topic of the event.';
COMMENT ON COLUMN outbox.event_key IS 'Kafka record key (partition key).';
COMMENT ON COLUMN outbox.event_type IS 'CloudEvents type.';
COMMENT ON COLUMN outbox.payload IS 'Whole CloudEvent in structured mode.';
COMMENT ON COLUMN outbox.occurred_at IS 'Instant the event occurred (UTC).';
COMMENT ON COLUMN outbox.published_at IS 'Instant the relay published the event (UTC); null while pending.';

CREATE TABLE processed_events (
    event_id     uuid PRIMARY KEY,
    event_type   text NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE processed_events IS 'Consumed events already applied; a duplicate key means already processed (KAN-2 §7.3).';
COMMENT ON COLUMN processed_events.event_id IS 'CloudEvents id of the consumed event.';
COMMENT ON COLUMN processed_events.event_type IS 'CloudEvents type of the consumed event.';
COMMENT ON COLUMN processed_events.processed_at IS 'Instant the event was applied (UTC).';
