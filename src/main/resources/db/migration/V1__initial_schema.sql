-- V1__initial_schema.sql
-- ReservaHub Sprint 1 schema (HU-01 .. HU-10)

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(30),
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CLIENT', 'PROVIDER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_users_role ON users (role);

CREATE TABLE providers (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    address         VARCHAR(300),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_providers_user_id UNIQUE (user_id),
    CONSTRAINT uq_providers_business_name UNIQUE (business_name),
    CONSTRAINT fk_providers_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_providers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE catalog_services (
    id                  UUID PRIMARY KEY,
    provider_id         UUID           NOT NULL,
    name                VARCHAR(150)   NOT NULL,
    description         VARCHAR(1000),
    duration_minutes    INTEGER        NOT NULL,
    price               NUMERIC(12, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_catalog_services_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT uq_catalog_services_provider_name UNIQUE (provider_id, name),
    CONSTRAINT ck_catalog_services_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_catalog_services_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_catalog_services_price CHECK (price >= 0)
);

CREATE INDEX idx_catalog_services_provider ON catalog_services (provider_id);

CREATE TABLE attention_schedules (
    id              UUID PRIMARY KEY,
    provider_id     UUID        NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attention_schedules_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT ck_attention_schedules_day CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    CONSTRAINT ck_attention_schedules_range CHECK (start_time < end_time)
);

CREATE INDEX idx_attention_schedules_provider_day ON attention_schedules (provider_id, day_of_week);

CREATE TABLE schedule_blocks (
    id              UUID PRIMARY KEY,
    schedule_id     UUID         NOT NULL,
    block_date      DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_schedule_blocks_schedule FOREIGN KEY (schedule_id) REFERENCES attention_schedules (id) ON DELETE CASCADE,
    CONSTRAINT ck_schedule_blocks_range CHECK (start_time < end_time)
);

CREATE INDEX idx_schedule_blocks_schedule_date ON schedule_blocks (schedule_id, block_date);

CREATE TABLE resources (
    id              UUID PRIMARY KEY,
    provider_id     UUID         NOT NULL,
    name            VARCHAR(150) NOT NULL,
    resource_type   VARCHAR(50)  NOT NULL,
    description     VARCHAR(1000),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_resources_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT uq_resources_provider_name UNIQUE (provider_id, name),
    CONSTRAINT ck_resources_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_resources_provider ON resources (provider_id);

-- JWT revocation store for logout (HU-04)
CREATE TABLE revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
