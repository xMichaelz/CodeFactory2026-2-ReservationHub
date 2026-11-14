#!/bin/sh
# Crea el usuario de la aplicación con mínimo privilegio (V-07/V-10): solo DML, sin DDL.
# El dueño (POSTGRES_USER) ejecuta las migraciones de Flyway; la API se conecta como reservahub_app.
# Se ejecuta una sola vez, al inicializar un volumen vacío (docker compose down -v para repetirlo).
set -eu

: "${DB_APP_USERNAME:?defina DB_APP_USERNAME}"
: "${DB_APP_PASSWORD:?defina DB_APP_PASSWORD}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v app_user="$DB_APP_USERNAME" -v app_password="$DB_APP_PASSWORD" -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE', :'app_user', :'app_password') \gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'db') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'db', :'app_user') \gexec
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
-- Tablas y secuencias que cree el dueño (Flyway) en adelante
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', :'owner', :'app_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I', :'owner', :'app_user') \gexec
SQL
