-- V5__reservations.sql
-- EP-03 Gestión de Reservas (HU-11 .. HU-16): reservas e historial de cambios.
-- Fecha y hora van en la zona del negocio (app.business.zone), igual que schedule_blocks.

CREATE TABLE reservations (
    id                UUID PRIMARY KEY,
    client_id         UUID        NOT NULL,
    service_id        UUID        NOT NULL,
    provider_id       UUID        NOT NULL,
    reservation_date  DATE        NOT NULL,
    start_time        TIME        NOT NULL,
    end_time          TIME        NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    version           BIGINT      NOT NULL DEFAULT 0,
    created_by        UUID        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reservations_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT fk_reservations_service FOREIGN KEY (service_id) REFERENCES catalog_services (id),
    CONSTRAINT fk_reservations_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT fk_reservations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_reservations_range CHECK (start_time < end_time),
    CONSTRAINT ck_reservations_status CHECK (
        status IN ('CONFIRMED', 'CANCELLED_BY_CLIENT', 'CANCELLED_BY_PROVIDER')
    )
);

CREATE INDEX idx_reservations_provider_date ON reservations (provider_id, reservation_date);
CREATE INDEX idx_reservations_client_date ON reservations (client_id, reservation_date);

CREATE TABLE reservation_history (
    id              UUID PRIMARY KEY,
    reservation_id  UUID         NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    actor_user_id   UUID         NOT NULL,
    reason          VARCHAR(500),
    old_date        DATE,
    old_start       TIME,
    old_end         TIME,
    new_date        DATE,
    new_start       TIME,
    new_end         TIME,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reservation_history_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_history_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT ck_reservation_history_action CHECK (action IN ('CREATED', 'RESCHEDULED', 'CANCELLED'))
);

CREATE INDEX idx_reservation_history_reservation ON reservation_history (reservation_id);
