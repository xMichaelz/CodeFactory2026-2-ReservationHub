# Identificación de vulnerabilidades con OWASP — ReservaHub (Sprint 2)

## 1. Alcance y método
- **Sistema analizado**: backend Spring Boot 4.1 / Java 25 (repo `CodeFactory2026-2-ReservationHub`, rama `feature/frontend-inicial`), frontend React 19 + Nginx (repo `CodeFactory2026-2-ReservationHub-frontend`) y su despliegue con Docker Compose.
- **Técnica**: revisión manual de código y configuración (análisis estático, sin pruebas de penetración en vivo).
- **Marcos de referencia**:
  - **OWASP Top 10:2025** (aplicaciones web).
  - **OWASP API Security Top 10:2023** (específico para APIs REST).
- **Severidad**: Alta / Media / Baja, según la probabilidad de explotación y el impacto en confidencialidad, integridad o disponibilidad, en un escenario de **despliegue en producción** con la configuración actual.

## 2. Resumen

| ID | Vulnerabilidad | OWASP Top 10:2025 | OWASP API 2023 | Severidad |
|---|---|---|---|---|
| V-01 | Secreto JWT y credenciales de BD por defecto en el código | A04 Fallas criptográficas · A02 Configuración insegura | API8 Configuración de seguridad incorrecta | **Alta** |
| V-02 | Sin HTTPS/TLS en el despliegue | A04 Fallas criptográficas | API8 | **Alta** |
| V-03 | Sin limitación de tasa (rate limiting) en endpoints públicos y costosos | A06 Diseño inseguro | API4 Consumo de recursos sin restricción | **Media** |
| V-04 | Bloqueo de cuenta explotable para denegar el servicio y enumerar usuarios | A07 Fallas de autenticación | API2 Autenticación rota | **Media** |
| V-05 | Enumeración de usuarios en el registro y en la búsqueda de clientes | A07 Fallas de autenticación | API3 / API6 Flujos de negocio sensibles | **Media** |
| V-06 | Swagger UI y OpenAPI públicos | A02 Configuración insegura | API9 Gestión inadecuada del inventario | **Media** |
| V-07 | Base de datos expuesta al host (puerto 5433) | A02 Configuración insegura | API8 | **Media** |
| V-08 | Sin cabeceras de seguridad HTTP (CSP, HSTS, X-Frame-Options…) | A02 Configuración insegura | API8 | **Media** |
| V-09 | JWT guardado en `sessionStorage` (accesible ante un XSS) | A07 Fallas de autenticación | API2 | **Baja** |
| V-10 | Contenedores ejecutándose como `root` e imágenes sin fijar por digest | A02 · A03 Fallas en la cadena de suministro | API8 | **Baja** |
| V-11 | Sin análisis automático de dependencias vulnerables (SCA) | A03 Fallas en la cadena de suministro | API10 | **Baja** |
| V-12 | Auditoría solo en logs locales, sin alertas | A09 Fallas en registro y alertas | — | **Baja** |
| V-13 | Auto-registro abierto de proveedores | A06 Diseño inseguro | API6 Flujos de negocio sensibles | **Baja** |

Total: **2 altas, 6 medias, 5 bajas**.

## 3. Detalle de las vulnerabilidades

### V-01 · Secreto JWT y credenciales por defecto — Alta
- **OWASP**: A04:2025 Fallas criptográficas, A02:2025 Configuración insegura · API8:2023.
- **Evidencia**:
  - `src/main/resources/application.properties:25` → `app.security.jwt.secret=${JWT_SECRET:change-me-to-a-long-random-secret-at-least-256-bits-long!!}`
  - `src/main/resources/application.properties:6` → contraseña de BD por defecto `reservahub`.
  - `compose.yaml:8,29,30` → `POSTGRES_PASSWORD: reservahub`, `JWT_SECRET: change-me-…` en texto plano.
- **Escenario de ataque**: si se despliega sin definir `JWT_SECRET`, cualquiera que lea el repositorio conoce la clave HMAC (HS256) y puede **firmar tokens con rol `PROVIDER` o `ADMIN`** para cualquier usuario: toma total de cuentas.
- **Recomendación**: quitar el valor por defecto (que la aplicación no arranque sin `JWT_SECRET`), generar un secreto aleatorio de ≥ 256 bits por entorno, guardarlo en un gestor de secretos (GitHub Secrets, Vault, Azure Key Vault) o un `.env` fuera del repositorio, y rotarlo periódicamente.

### V-02 · Sin HTTPS/TLS — Alta
- **OWASP**: A04:2025 Fallas criptográficas · API8:2023.
- **Evidencia**: `nginx.conf` (frontend) solo tiene `listen 80;`; la API expone `8080` en HTTP plano (`compose.yaml`).
- **Escenario**: en una red compartida (Wi-Fi del campus), un atacante intercepta el `POST /auth/login` (contraseña en claro) o el header `Authorization: Bearer …` y suplanta al usuario durante 1 hora.
- **Recomendación**: terminar TLS en Nginx (Let's Encrypt) o en un balanceador, redirigir 80 → 443, activar HSTS y no publicar el puerto 8080 fuera de la red interna de Docker.

### V-03 · Sin limitación de tasa — Media
- **OWASP**: A06:2025 Diseño inseguro · API4:2023 Consumo de recursos sin restricción.
- **Evidencia**: no hay filtro de rate limiting en `SecurityConfig.java` ni en `nginx.conf`. Endpoints afectados: `POST /auth/login`, `POST /users`, `POST /providers` (públicos), `GET /services/{id}/availability` (calcula hasta 31 días de franjas), `GET /catalog/services` y `GET /reservations/mine` (devuelven listas sin paginación).
- **Escenario**: un script envía miles de peticiones por minuto: relleno de credenciales repartido entre muchos emails, registros masivos falsos o saturación de la BD con consultas de disponibilidad.
- **Recomendación**: rate limiting por IP y por usuario (Bucket4j en Spring o `limit_req` en Nginx), paginación (`Pageable`) en los listados y límites de tamaño de respuesta.

### V-04 · Bloqueo de cuenta usado para DoS y enumeración — Media
- **OWASP**: A07:2025 Fallas de autenticación · API2:2023.
- **Evidencia**: `AuthService.login` revisa `lockedUntil` **antes** de verificar la contraseña y responde `423 Locked` con `Retry-After` (`AccountLockedException.java:10`, `GlobalExceptionHandler.java:39`). El bloqueo se aplica tras 5 intentos (`LOCKOUT_MAX_ATTEMPTS`).
- **Escenario**: (a) un atacante que conoce el email de un proveedor envía 5 contraseñas falsas cada 15 minutos y lo mantiene **bloqueado permanentemente**; (b) la respuesta 423 (distinta del 401 genérico) confirma que el email existe.
- **Recomendación**: responder con el mismo 401 genérico, bloquear por combinación IP + cuenta o usar retardo progresivo en vez de bloqueo duro, CAPTCHA tras N fallos y aviso por correo al titular.

### V-05 · Enumeración de usuarios — Media
- **OWASP**: A07:2025 · API3:2023 Autorización a nivel de propiedad / API6:2023 Flujos de negocio sensibles.
- **Evidencia**:
  - `UserService.java:38,60` → `409 Conflict "Email is already registered"` en el registro público.
  - `ProviderService.java:40` → `409 "Business name is already registered"`.
  - `GET /api/v1/users/lookup?email=` (rol PROVIDER) devuelve `id` y `name` del cliente (`ClientLookupDTO`).
- **Escenario**: un atacante comprueba qué correos están registrados (base para phishing o fuerza bruta dirigida); un proveedor malicioso recorre listas de correos para obtener nombres de clientes.
- **Recomendación**: mensaje genérico en el registro ("si el correo es válido, recibirás un email de confirmación"), rate limiting específico en `lookup` y registro en auditoría de cada búsqueda.
- **Nota positiva**: `ClientLookupDTO` ya minimiza los datos (no devuelve email ni teléfono).

### V-06 · Swagger UI y OpenAPI públicos — Media
- **OWASP**: A02:2025 · API9:2023 Gestión inadecuada del inventario.
- **Evidencia**: `SecurityConfig.java:54` → `/v3/api-docs/**`, `/swagger-ui/**` con `permitAll()`.
- **Escenario**: en producción, un atacante obtiene el mapa completo de endpoints, parámetros y esquemas, lo que reduce el esfuerzo de reconocimiento.
- **Recomendación**: deshabilitar en producción (`springdoc.api-docs.enabled=false` en un perfil `prod`) o protegerlo con autenticación o red interna.

### V-07 · Base de datos expuesta al host — Media
- **OWASP**: A02:2025 · API8:2023.
- **Evidencia**: `compose.yaml:11` → `"5433:5432"` publica PostgreSQL en todas las interfaces del host, con credenciales triviales (V-01).
- **Escenario**: si el servidor tiene IP pública o está en la red del campus, cualquiera conecta con `psql -h <host> -p 5433 -U reservahub` y lee o borra todos los datos.
- **Recomendación**: no publicar el puerto en producción (la API ya llega a la BD por la red interna de Docker) o enlazarlo a `127.0.0.1:5433:5432`, y usar un usuario de BD con privilegios mínimos distinto del dueño del esquema.

### V-08 · Sin cabeceras de seguridad HTTP — Media
- **OWASP**: A02:2025 · API8:2023.
- **Evidencia**: `nginx.conf` no tiene ninguna directiva `add_header`.
- **Escenario**: sin `Content-Security-Policy`, un XSS puede cargar scripts externos y robar el JWT (V-09); sin `X-Frame-Options`/`frame-ancestors` es posible el clickjacking; sin HSTS es posible el downgrade a HTTP.
- **Recomendación**: añadir `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` y `Referrer-Policy: no-referrer`.

### V-09 · JWT en `sessionStorage` — Baja
- **OWASP**: A07:2025 · API2:2023.
- **Evidencia**: `src/auth/session.ts:47,65` (frontend) guarda la sesión con el token en `sessionStorage`.
- **Escenario**: cualquier XSS en la SPA puede leer el token y usarlo hasta que expire (1 h). El riesgo es bajo porque React escapa la salida por defecto y `sessionStorage` se borra al cerrar la pestaña.
- **Recomendación**: a mediano plazo, mover el token a una cookie `HttpOnly; Secure; SameSite=Strict` (y entonces reactivar CSRF), o acortar la expiración y añadir refresh tokens; mientras tanto, aplicar CSP (V-08).

### V-10 · Contenedores como root e imágenes sin digest — Baja
- **OWASP**: A02:2025 · A03:2025 Fallas en la cadena de suministro.
- **Evidencia**: `Dockerfile` (backend) no declara `USER`; las imágenes base (`eclipse-temurin:25-jre`, `postgres:16-alpine`, `nginx:1.27-alpine`) se referencian por etiqueta móvil.
- **Recomendación**: crear y usar un usuario sin privilegios (`USER app`), fijar las imágenes por `@sha256:` y escanearlas (Trivy, Docker Scout).

### V-11 · Sin análisis de dependencias (SCA) — Baja
- **OWASP**: A03:2025 · API10:2023.
- **Evidencia**: `.github/workflows/sonarcloud.yml` ejecuta SonarQube Cloud (análisis estático del código), pero no hay Dependabot, OWASP Dependency-Check ni `npm audit` en el CI.
- **Recomendación**: activar Dependabot en ambos repositorios y añadir `dependency-check-maven` u `osv-scanner` al pipeline, haciendo fallar el build con CVEs de severidad alta.
- **Nota positiva**: las GitHub Actions ya están fijadas por SHA (buena práctica contra ataques a la cadena de suministro).

### V-12 · Auditoría sin centralización ni alertas — Baja
- **OWASP**: A09:2025 Fallas en registro y alertas.
- **Evidencia**: `AuditLogger` solo escribe en el log de la aplicación (`log.info("audit action=…")`); existen eventos `LOGIN_FAILED`, `LOGIN_LOCKED`, `SERVICE_CREATED`, etc., pero no se envían a un sistema central ni disparan alertas.
- **Recomendación**: enviar los logs a un sistema central (ELK, Grafana Loki, Azure Monitor) y crear alertas por picos de `LOGIN_FAILED`/`LOGIN_LOCKED` o de respuestas 403.

### V-13 · Auto-registro abierto de proveedores — Baja
- **OWASP**: A06:2025 Diseño inseguro · API6:2023.
- **Evidencia**: `SecurityConfig.java:56` → `POST /api/v1/providers` con `permitAll()`; cualquier persona obtiene el rol `PROVIDER` (incluido el acceso a `users/lookup`, V-05).
- **Recomendación**: verificación del correo y/o aprobación por un `ADMIN` antes de activar el proveedor (estado `PENDING`).

## 4. Controles de seguridad ya implementados (fortalezas)

| Control | OWASP mitigado | Evidencia |
|---|---|---|
| Control de acceso por rol (RBAC) declarativo y por método | A01 · API5 (BFLA) | `SecurityConfig.java` + `@EnableMethodSecurity` |
| Validación de dueño del objeto (anti-BOLA) | A01 · API1 (BOLA) | `ReservationService.requireOwner`, `CatalogServiceService` → `ForbiddenException("You are not allowed to modify this service")` |
| Contraseñas con BCrypt y política de complejidad (8–72, patrón) | A04 · A07 | `SecurityConfig.passwordEncoder`, `CreateUserRequestDTO` |
| JWT con expiración (1 h) y revocación por JTI al cerrar sesión | A07 · API2 | `TokenRevocationService`, tabla `revoked_tokens` |
| Bloqueo tras 5 intentos fallidos | A07 · API2 | `AuthService.registerFailure` |
| Mensaje genérico "Invalid credentials" en el login | A07 | `AuthService.login` |
| Consultas parametrizadas (JPA/Hibernate), sin SQL concatenado | A05 Inyección | repositorios Spring Data |
| Validación de entrada con Bean Validation (`@Valid`, `@Size`, `@Email`) y 400 ante parámetros inválidos | A05 · A10 | DTOs, `GlobalExceptionHandler` |
| Manejo centralizado de errores sin stack traces y con `traceId` | A10 Manejo inadecuado de excepciones | `GlobalExceptionHandler` (handler genérico `Exception.class`) |
| Actuator limitado a `health` y sin detalles | A02 | `application.properties` |
| CORS con lista blanca de orígenes, métodos y cabeceras | A02 · API8 | `SecurityConfig.corsConfigurationSource` |
| Sesión `STATELESS`; CSRF deshabilitado de forma justificada (token Bearer, no cookies) | A01 | `SecurityConfig` |
| Concurrencia en reservas (bloqueo pesimista + bloqueo optimista) | A06 · API6 | ADR-015, `findByIdForUpdate`, `ObjectOptimisticLockingFailureException` |
| Esquema versionado con Flyway y `ddl-auto=validate` | A08 Integridad | `db/migration/V1…V5` |
| GitHub Actions fijadas por SHA y análisis con SonarQube Cloud | A03 · A08 | `.github/workflows/sonarcloud.yml` |

## 5. Plan de remediación sugerido (por prioridad)

| Orden | Acción | Cierra | Esfuerzo |
|---|---|---|---|
| 1 | Secretos fuera del código; la app no arranca sin `JWT_SECRET` | V-01 | Bajo |
| 2 | TLS en Nginx + HSTS; no publicar 8080 ni 5433 | V-02, V-07 | Bajo-Medio |
| 3 | Cabeceras de seguridad en Nginx | V-08 | Bajo |
| 4 | Perfil `prod` que deshabilite Swagger | V-06 | Bajo |
| 5 | Rate limiting (login, registro, lookup, availability) + paginación | V-03, V-05 | Medio |
| 6 | Rediseño del bloqueo (401 genérico, retardo progresivo, CAPTCHA) | V-04 | Medio |
| 7 | Dependabot + Dependency-Check + Trivy; `USER` no root | V-10, V-11 | Bajo |
| 8 | Logs centralizados y alertas | V-12 | Medio |
| 9 | Aprobación de proveedores; token en cookie HttpOnly | V-13, V-09 | Medio-Alto |

## 6. Relación con la priorización de APIs
Los endpoints **P1** de `priorizacion-apis.md` (`/auth/login`, `/users`, `/providers`, `/reservations`, `/availability`) son exactamente los afectados por V-01 a V-05, por lo que la remediación 1–6 debe hacerse antes de un despliegue productivo.
