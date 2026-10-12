-- #201 tipo de servicio del proveedor
ALTER TABLE providers ADD COLUMN service_type VARCHAR(100) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE providers ALTER COLUMN service_type DROP DEFAULT;

-- #204 cantidad disponible del recurso
ALTER TABLE resources ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1;
ALTER TABLE resources ADD CONSTRAINT ck_resources_quantity CHECK (quantity > 0);
