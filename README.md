# ReservaHub

Plataforma de reservas de servicios — **Caso 14 CodeF@ctory UdeA** (Sprint 1).

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
# Rellene DB_PASSWORD y JWT_SECRET (sin ellos la API y el compose no arrancan):
openssl rand -base64 48   # úsalo como JWT_SECRET
```

`.env` está en `.gitignore`: nunca se commitea.

Variables:

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales DB (obligatorias, sin valor por defecto) |
| `JWT_SECRET` | Secreto HS256, obligatorio, ≥ 32 bytes (la API no arranca sin él) |
| `JWT_EXPIRATION_MS` | Expiración del token (default 1800000 = 30 min) |
| `SWAGGER_ENABLED` | `true` publica Swagger/OpenAPI (solo desarrollo); por defecto `false` y las rutas dan 404 |
| `SERVER_PORT` | Puerto HTTP (default 8080) |

## Ejecución local

### 1) PostgreSQL con Docker

```bash
docker compose up -d db
```

PostgreSQL queda en `localhost:5433` (usuario/db: `reservahub`; la clave es la de `DB_PASSWORD` en `.env`).

Exportar variables (o cargar el `.env` en tu shell: `set -a; source .env; set +a`):

```bash
export DB_URL=jdbc:postgresql://localhost:5433/reservahub
export DB_USERNAME=reservahub
export DB_PASSWORD='<la de tu .env>'
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
docker compose up --build
```

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

## Endpoints principales (HU-01 .. HU-10)

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

Colección Postman: `postman/ReservaHub-Sprint1.postman_collection.json`

## Estructura del proyecto

```text
reservahub/
├── src/main/java/co/udea/codefactory/reservahub/
│   ├── user/ provider/ servicecatalog/ schedule/ resource/
│   ├── reservation/ report/          # placeholders Sprint futuro
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
