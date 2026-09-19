# Arquitectura ReservaHub — Sprint 1

## 1. Arquitectura general

ReservaHub es una **única aplicación Spring Boot** (JAR) organizada como **monolito modular**.
No hay microservicios, API Gateway ni bases de datos por módulo.

```text
[Client / Postman / Swagger UI]
            |
            | HTTPS/HTTP JSON
            v
   [ReservaHub Spring Boot]
            |
            v
      [PostgreSQL]
```

## 2. Monolito modular

Un solo proceso desplegable contiene módulos de negocio con paquetes separados y dependencias controladas hacia `shared` y `security`.

## 3. Módulos Sprint 1

| Módulo | Responsabilidad |
|--------|-----------------|
| `user` | Registro de clientes (HU-01) |
| `provider` | Registro de proveedores (HU-02) |
| `security` | Login/logout JWT, RBAC, revocación |
| `servicecatalog` | Crear/editar/desactivar servicios |
| `schedule` | Horarios de atención y bloqueos |
| `resource` | Registro de recursos |
| `shared` | Errores, enums, DTO de error |
| `audit` | Logging de auditoría mínimo |
| `observability` | TraceId + Actuator |
| `reservation` / `report` | Reservados (sin endpoints Sprint 1) |

## 4. Dependencias entre módulos (conceptual)

```text
controller.*  --> service.* --> repository.*
service.provider --> service.user
service.servicecatalog/schedule/resource --> service.provider
security --> user
* --> shared
```

## 5. Flujo de una request

1. `TraceIdFilter` asigna `X-Trace-Id`
2. `JwtAuthenticationFilter` valida Bearer JWT (no revocado)
3. `SecurityFilterChain` aplica RBAC
4. Controller valida DTO (`@Valid`)
5. Service aplica reglas de negocio
6. Mapper convierte Entity ↔ DTO
7. Repository persiste en PostgreSQL
8. Errores → `GlobalExceptionHandler` → `ApiErrorResponse`

## 6. Seguridad

- Roles: `CLIENT`, `PROVIDER`, `ADMIN`
- Passwords: BCrypt
- Auth: JWT HS256 (`JWT_SECRET`, expiración configurable)
- Logout: revoca JTI en `revoked_tokens` hasta `expires_at`
- Endpoints de proveedor protegidos con `ROLE_PROVIDER`

## 7. Persistencia

- PostgreSQL + Flyway (`V1__initial_schema.sql`)
- `spring.jpa.hibernate.ddl-auto=validate`

## 8. API

Base: `/api/v1`  
Docs: `/swagger-ui.html`, `/v3/api-docs`  
Health: `/actuator/health`

## 9. Despliegue local

- `docker compose up -d db` → PostgreSQL en host `5433`
- `./mvnw spring-boot:run` con variables de `.env.example`
- o `docker compose up --build` para app + DB
