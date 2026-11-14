# ReservaHub

[![CI](https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub/actions/workflows/sonarcloud.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=alert_status)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=coverage)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=bugs)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=security_rating)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)

Plataforma de reservas de servicios — **Caso 14 CodeF@ctory UdeA** (Sprint 2).

Arquitectura: **monolito modular** (una sola aplicación Spring Boot), organizada por dominios de negocio con capas `controller / DTO / entity / mapper / repository / service`.

## Tecnologías

| Tecnología | Versión / uso |
|------------|---------------|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven | Wrapper (`mvnw`) |
| PostgreSQL | 16 (Docker) |
| Flyway | migraciones versionadas |
| Spring Security + JWT | autenticación/autorización |
| SpringDoc OpenAPI | Swagger UI |
| Actuator | `/actuator/health` |
| Testcontainers | pruebas de integración |

Dependencias extra justificadas: **JJWT** (tokens), **springdoc-openapi** (documentación API exigida), **Testcontainers** (integración con PostgreSQL real).

## Requisitos

- JDK 25+
- Docker (recomendado para PostgreSQL y tests)
- Maven Wrapper incluido (`./mvnw`)

## Configuración

```bash
cp .env.example .env
# Rellene DB_PASSWORD, DB_APP_PASSWORD y JWT_SECRET (sin ellos la API y el compose no arrancan):
openssl rand -base64 48   # úsalo como JWT_SECRET
```

`.env` está en `.gitignore`: nunca se commitea.

Variables:

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la API: usuario `reservahub_app`, solo DML (obligatorias, sin valor por defecto) |
| `DB_OWNER_USERNAME` / `DB_OWNER_PASSWORD` | Dueño de la BD (`reservahub`), solo para las migraciones de Flyway (obligatorias) |
| `DB_APP_PASSWORD` | Clave de `reservahub_app` que usa el compose (la crea `docker/db-init/01-app-user.sh`) |
| `JWT_SECRET` | Secreto HS256, obligatorio, ≥ 32 bytes (la API no arranca sin él) |
| `JWT_EXPIRATION_MS` | Expiración del token (default 1800000 = 30 min) |
| `SWAGGER_ENABLED` | `true` publica Swagger/OpenAPI (solo desarrollo); por defecto `false` y las rutas dan 404 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Alternativa a `DB_URL` (la usa Render) |
| `DB_POOL_MAX` | Tamaño máximo del pool de conexiones (default 10) |
| `SERVER_PORT` | Puerto HTTP local (default 8080). En Render manda `PORT` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma |

## Ejecución local

### 1) PostgreSQL con Docker

```bash
docker compose -f compose.yaml -f compose.dev.yaml up -d db
```

`compose.yaml` (despliegue) **no publica puertos**; `compose.dev.yaml` abre la BD en `127.0.0.1:5433` y la API en `127.0.0.1:8080` (con Swagger). Dueño: `reservahub` (clave `DB_PASSWORD`); la API usa `reservahub_app` (clave `DB_APP_PASSWORD`), que solo tiene SELECT/INSERT/UPDATE/DELETE.

> **Primera vez tras O7:** el usuario `reservahub_app` solo se crea al inicializar el volumen. Ejecute `docker compose down -v` (borra los datos de desarrollo) y vuelva a levantar.

Exportar variables (o cargar el `.env` en tu shell: `set -a; source .env; set +a`):

```bash
export DB_URL=jdbc:postgresql://localhost:5433/reservahub
export DB_USERNAME=reservahub_app
export DB_PASSWORD='<DB_APP_PASSWORD de tu .env>'
export DB_OWNER_USERNAME=reservahub
export DB_OWNER_PASSWORD='<DB_PASSWORD de tu .env>'
export JWT_SECRET="$(openssl rand -base64 48)"
export SWAGGER_ENABLED=true   # opcional: Swagger/Postman local
```

### 2) Arrancar la API

```bash
./mvnw spring-boot:run
```

Flyway aplicará `V1__initial_schema.sql` al iniciar.

### Todo con Docker Compose

```bash
docker compose up --build                                   # despliegue: sin puertos al host, Swagger apagado
docker compose -f compose.yaml -f compose.dev.yaml up --build   # desarrollo: 127.0.0.1:8080 y :5433
```

Servicios: `db` (PostgreSQL 16, imagen fijada por digest) y `app` (API, corre como usuario `app` uid 10001, sin root; escucha en el puerto 8080 solo dentro de la red de Compose). Para alcanzar la API desde otro servicio (Nginx del front) hay que unirse a la red de este compose (`reservahub-backend`, declarada `external` en el otro compose) y apuntar a `http://app:8080`.

Seguridad de la cadena: `.github/dependabot.yml` (Maven, Docker y Actions, semanal) y `.github/workflows/seguridad.yml` (OSV-Scanner y Trivy, fallan con HIGH/CRITICAL).

## Despliegue en Render

`render.yaml` crea la API (Docker, Java 25) y PostgreSQL 16 en la región `oregon`. Flyway aplica las migraciones al arrancar, igual que en local. Render genera `JWT_SECRET` y conecta la base por la red privada.

1. El blueprint tiene que estar en `main`.
2. Abre [este enlace de Blueprint](https://render.com/deploy?repo=https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub) e inicia sesión en Render.
3. Confirma el plan gratuito. Render crea `reservahub-api` y `reservahub-db`.

Cuando termine el deploy:

- Health: `https://<servicio>.onrender.com/actuator/health`
- Swagger y OpenAPI están apagados por defecto (V-06); para verlos, `SWAGGER_ENABLED=true` en el dashboard.

Si el frontend no corre en `http://localhost:5173`, cambia `CORS_ALLOWED_ORIGINS` en el dashboard (orígenes separados por coma) y vuelve a desplegar.

En el plan gratuito la API se suspende tras un rato sin tráfico: el primer request después de eso tarda más. La base gratuita de Render caduca a los 30 días.

## Tests

```bash
./mvnw clean test
./mvnw clean package
```

## Documentación interactiva

Solo con `SWAGGER_ENABLED=true`; si no, `/v3/api-docs` y `/swagger-ui*` responden 404.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Endpoints principales (HU-01 .. HU-16)

| Método | Ruta | HU |
|--------|------|----|
| POST | `/api/v1/users` | HU-01 |
| POST | `/api/v1/providers` | HU-02 |
| POST | `/api/v1/auth/login` | HU-03 |
| POST | `/api/v1/auth/logout` | HU-04 |
| POST | `/api/v1/services` | HU-05 |
| PUT | `/api/v1/services/{id}` | HU-06 |
| PATCH | `/api/v1/services/{id}/deactivate` | HU-07 |
| POST | `/api/v1/schedules` | HU-08 |
| POST | `/api/v1/schedules/{id}/blocks` | HU-09 |
| POST | `/api/v1/resources` | HU-10 |
| GET | `/api/v1/services/{id}/availability` | HU-11 |
| POST | `/api/v1/reservations` | HU-12, HU-13 |
| PATCH | `/api/v1/reservations/{id}/cancel` | HU-14, HU-15 |
| PATCH | `/api/v1/reservations/{id}/reschedule` | HU-16 |

HU-12 es la reserva del cliente para sí mismo. HU-13 es el mismo `POST`: el proveedor envía el `clientId`. En la cancelación, el proveedor debe indicar motivo (HU-15).

Priorización, roles y política de `/api/v1`: `docs/api/apis-priorizadas.md`.

Colecciones Postman: `postman/ReservaHub-Sprint1.postman_collection.json` y `postman/ReservaHub-Sprint2.postman_collection.json`.

## Estructura del proyecto

```text
reservahub/
├── src/main/java/co/udea/codefactory/reservahub/
│   ├── user/ provider/ servicecatalog/ schedule/ resource/
│   ├── reservation/                  # Sprint 2
│   ├── report/                       # placeholder Sprint futuro
│   ├── security/ audit/ observability/ shared/
├── src/main/resources/db/migration/
├── docs/adr/ docs/architecture/ docs/api/
├── postman/
├── Dockerfile
├── compose.yaml
└── pom.xml
```

## Documentación adicional

- Arquitectura: `docs/architecture/overview.md`
- Diagramas: `docs/architecture/diagrams.md`
- Supuestos: `docs/architecture/assumptions.md`
- ADRs: `docs/adr/`
