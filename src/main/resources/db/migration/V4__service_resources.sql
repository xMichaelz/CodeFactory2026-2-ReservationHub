-- #205 recursos que usa cada servicio
CREATE TABLE service_resources (
    service_id  UUID NOT NULL REFERENCES catalog_services (id),
    resource_id UUID NOT NULL REFERENCES resources (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (service_id, resource_id)
);
CREATE INDEX idx_service_resources_resource ON service_resources (resource_id);
