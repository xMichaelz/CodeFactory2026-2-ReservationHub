# Identificación de vulnerabilidades con OWASP — ReservaHub (Sprint 2)

## 1. Alcance y método
- **Sistema analizado**: backend Spring Boot 4.1 / Java 25 (repo `CodeFactory2026-2-ReservationHub`, rama `feature/frontend-inicial`; correcciones en `fix/owasp`), frontend React 19 + Nginx (repo `CodeFactory2026-2-ReservationHub-frontend`) y su despliegue con Docker Compose.
- **Técnica**: revisión manual de código y configuración (análisis estático, sin pruebas de penetración en vivo).
- **Marcos de referencia**:
  - **OWASP Top 10:2025** (aplicaciones web).
  - **OWASP API Security Top 10:2023** (específico para APIs REST).
- **Severidad**: Alta / Media / Baja, según la probabilidad de explotación y el impacto en confidencialidad, integridad o disponibilidad, en un escenario de **despliegue en producción** con la configuración actual.

## 2. Resumen

| ID | Vulnerabilidad | OWASP Top 10:2025 | OWASP API 2023 | Severidad | Estado |
|---|---|---|---|---|---|
| V-01 | Secreto JWT y credenciales de BD por defecto en el código | A04 Fallas criptográficas · A02 Configuración insegura | API8 Configuración de seguridad incorrecta | **Alta** | Corregida |
| V-02 | Sin HTTPS/TLS en el despliegue | A04 Fallas criptográficas | API8 | **Alta** | Corregida |
| V-03 | Sin limitación de tasa (rate limiting) en endpoints públicos y costosos | A06 Diseño inseguro | API4 Consumo de recursos sin restricción | **Media** | Corregida |
| V-04 | Bloqueo de cuenta explotable para denegar el servicio y enumerar usuarios | A07 Fallas de autenticación | API2 Autenticación rota | **Media** | Corregida |
| V-05 | Enumeración de usuarios en el registro y en la búsqueda de clientes | A07 Fallas de autenticación | API3 / API6 Flujos de negocio sensibles | **Media** | Corregida con riesgo residual (409 en el registro, ADR-016) |
| V-06 | Swagger UI y OpenAPI públicos | A02 Configuración insegura | API9 Gestión inadecuada del inventario | **Media** | Corregida |
| V-07 | Base de datos expuesta al host (puerto 5433) | A02 Configuración insegura | API8 | **Media** | Corregida |
| V-08 | Sin cabeceras de seguridad HTTP (CSP, HSTS, X-Frame-Options…) | A02 Configuración insegura | API8 | **Media** | Corregida |
| V-09 | JWT guardado en `sessionStorage` (accesible ante un XSS) | A07 Fallas de autenticación | API2 | **Baja** | Mitigada (CSP y sesión que caduca; el token sigue en sessionStorage) |
| V-10 | Contenedores ejecutándose como `root` e imágenes sin fijar por digest | A02 · A03 Fallas en la cadena de suministro | API8 | **Baja** | Corregida |
| V-11 | Sin análisis automático de dependencias vulnerables (SCA) | A03 Fallas en la cadena de suministro | API10 | **Baja** | Corregida (Trivy corre en CI) |
| V-12 | Auditoría solo en logs locales, sin alertas | A09 Fallas en registro y alertas | — | **Baja** | Mitigada (logs JSON + alerta por umbral, sin ELK) |
| V-13 | Auto-registro abierto de proveedores | A06 Diseño inseguro | API6 Flujos de negocio sensibles | **Baja** | Mitigada (límite 3/h por IP + auditoría, sin aprobación) |

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
- **Corrección aplicada (H3)**: backend O3 (commit `b83f373`, merge `116527a`). `application.properties` usa `${JWT_SECRET}`, `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` sin valor por defecto y la app no arranca sin un secreto de ≥ 32 bytes; `compose.yaml` exige `${VAR:?…}`; plantilla `.env.example`; expiración del token a 30 min. Prueba: `security/owasp/SecretsConfigTest`. Evidencia: [evidencias/V-01-secretos.txt](evidencias/V-01-secretos.txt).

### V-02 · Sin HTTPS/TLS — Alta
- **OWASP**: A04:2025 Fallas criptográficas · API8:2023.
- **Evidencia**: `nginx.conf` (frontend) solo tiene `listen 80;`; la API expone `8080` en HTTP plano (`compose.yaml`).
- **Escenario**: en una red compartida (Wi-Fi del campus), un atacante intercepta el `POST /auth/login` (contraseña en claro) o el header `Authorization: Bearer …` y suplanta al usuario durante 1 hora.
- **Recomendación**: terminar TLS en Nginx (Let's Encrypt) o en un balanceador, redirigir 80 → 443, activar HSTS y no publicar el puerto 8080 fuera de la red interna de Docker.
- **Corrección aplicada (H3)**: front O8 (commit `e2720ad`, merge `a5e1479`). Nginx termina TLS 1.2/1.3 con http2 en `8443` (certificado autofirmado EC P-256 generado por `scripts/generar-certificado.sh`, clave sin commitear y montada `:ro`), `8081` responde 301 a `https://…:8443` y se envía HSTS. Pruebas: `src/seguridad/nginx.test.ts`, e2e por HTTPS. Evidencia: [evidencias/V-02-tls.txt](evidencias/V-02-tls.txt).

### V-03 · Sin limitación de tasa — Media
- **OWASP**: A06:2025 Diseño inseguro · API4:2023 Consumo de recursos sin restricción.
- **Evidencia**: no hay filtro de rate limiting en `SecurityConfig.java` ni en `nginx.conf`. Endpoints afectados: `POST /auth/login`, `POST /users`, `POST /providers` (públicos), `GET /services/{id}/availability` (calcula hasta 31 días de franjas), `GET /catalog/services` y `GET /reservations/mine` (devuelven listas sin paginación).
- **Escenario**: un script envía miles de peticiones por minuto: relleno de credenciales repartido entre muchos emails, registros masivos falsos o saturación de la BD con consultas de disponibilidad.
- **Recomendación**: rate limiting por IP y por usuario (Bucket4j en Spring o `limit_req` en Nginx), paginación (`Pageable`) en los listados y límites de tamaño de respuesta.
- **Corrección aplicada (H3)**: backend O4 (commit `5def4d4`, merge `4d96325`). Bucket4j + Caffeine (`security/ratelimit/RateLimitFilter`): login 10/min por IP, registro 20/h, alta de proveedor 3/h, lecturas costosas 60/min y búsqueda de clientes 10/min por usuario; responde 429 `RATE_LIMITED` con `Retry-After`; IP real tras Nginx (`ClientIpResolver`). Segunda capa en Nginx (`limit_req` 20 r/s, ráfaga 40). Sin paginación: los rangos ya están acotados a 31 días. Pruebas: `RateLimitCoverageTest`, `RateLimitDisabledTest`. Evidencia: [evidencias/V-03-rate-limit.txt](evidencias/V-03-rate-limit.txt).

### V-04 · Bloqueo de cuenta usado para DoS y enumeración — Media
- **OWASP**: A07:2025 Fallas de autenticación · API2:2023.
- **Evidencia**: `AuthService.login` revisa `lockedUntil` **antes** de verificar la contraseña y responde `423 Locked` con `Retry-After` (`AccountLockedException.java:10`, `GlobalExceptionHandler.java:39`). El bloqueo se aplica tras 5 intentos (`LOCKOUT_MAX_ATTEMPTS`).
- **Escenario**: (a) un atacante que conoce el email de un proveedor envía 5 contraseñas falsas cada 15 minutos y lo mantiene **bloqueado permanentemente**; (b) la respuesta 423 (distinta del 401 genérico) confirma que el email existe.
- **Recomendación**: responder con el mismo 401 genérico, bloquear por combinación IP + cuenta o usar retardo progresivo en vez de bloqueo duro, CAPTCHA tras N fallos y aviso por correo al titular.
- **Corrección aplicada (H3)**: backend O5 (commit `a78b82c`, merge `9232df1`). El login responde siempre el mismo 401 genérico (BCrypt contra un hash ficticio si el correo no existe) y el bloqueo temporal es por pareja correo + IP: un atacante ya no bloquea la cuenta de otro ni distingue cuentas por el 423 (desaparecen `ACCOUNT_LOCKED` y el 423; el front lo refleja en O9). Pruebas: `AuthEnumerationTest`, `AuthServiceTest`. Evidencia: [evidencias/V-04-login-bloqueo.txt](evidencias/V-04-login-bloqueo.txt).

### V-05 · Enumeración de usuarios — Media
- **OWASP**: A07:2025 · API3:2023 Autorización a nivel de propiedad / API6:2023 Flujos de negocio sensibles.
- **Evidencia**:
  - `UserService.java:38,60` → `409 Conflict "Email is already registered"` en el registro público.
  - `ProviderService.java:40` → `409 "Business name is already registered"`.
  - `GET /api/v1/users/lookup?email=` (rol PROVIDER) devuelve `id` y `name` del cliente (`ClientLookupDTO`).
- **Escenario**: un atacante comprueba qué correos están registrados (base para phishing o fuerza bruta dirigida); un proveedor malicioso recorre listas de correos para obtener nombres de clientes.
- **Recomendación**: mensaje genérico en el registro ("si el correo es válido, recibirás un email de confirmación"), rate limiting específico en `lookup` y registro en auditoría de cada búsqueda.
- **Nota positiva**: `ClientLookupDTO` ya minimiza los datos (no devuelve email ni teléfono).
- **Corrección aplicada (H3)**: backend O5 (commit `a78b82c`). El registro repetido devuelve 409 con mensaje genérico («No se pudo completar el registro con los datos enviados») y queda auditado (`REGISTRATION_REJECTED`), con límite 20/h por IP; la búsqueda de clientes queda limitada (10/min) y auditada (`CLIENT_LOOKUP`). **Riesgo residual aceptado** (ADR-016): el código 409 sigue revelando que el correo existe, porque el front y la HU-02 lo usan; el límite por IP y la auditoría acotan la enumeración. Pruebas: `AuthEnumerationTest`, `UserServiceTest`. Evidencia: [evidencias/V-05-registro-enumeracion.txt](evidencias/V-05-registro-enumeracion.txt).

### V-06 · Swagger UI y OpenAPI públicos — Media
- **OWASP**: A02:2025 · API9:2023 Gestión inadecuada del inventario.
- **Evidencia**: `SecurityConfig.java:54` → `/v3/api-docs/**`, `/swagger-ui/**` con `permitAll()`.
- **Escenario**: en producción, un atacante obtiene el mapa completo de endpoints, parámetros y esquemas, lo que reduce el esfuerzo de reconocimiento.
- **Recomendación**: deshabilitar en producción (`springdoc.api-docs.enabled=false` en un perfil `prod`) o protegerlo con autenticación o red interna.
- **Corrección aplicada (H3)**: backend O3 (commit `b83f373`). Swagger y `/v3/api-docs` están apagados salvo `SWAGGER_ENABLED=true` (solo lo activa `compose.dev.yaml`) y responden 404 al anónimo y con token. Prueba: `SwaggerExposureTest`. Evidencia: [evidencias/V-06-swagger.txt](evidencias/V-06-swagger.txt).

### V-07 · Base de datos expuesta al host — Media
- **OWASP**: A02:2025 · API8:2023.
- **Evidencia**: `compose.yaml:11` → `"5433:5432"` publica PostgreSQL en todas las interfaces del host, con credenciales triviales (V-01).
- **Escenario**: si el servidor tiene IP pública o está en la red del campus, cualquiera conecta con `psql -h <host> -p 5433 -U reservahub` y lee o borra todos los datos.
- **Recomendación**: no publicar el puerto en producción (la API ya llega a la BD por la red interna de Docker) o enlazarlo a `127.0.0.1:5433:5432`, y usar un usuario de BD con privilegios mínimos distinto del dueño del esquema.
- **Corrección aplicada (H3)**: backend O7 (commit `d5a6491`, merge `9a1ffb1`). `compose.yaml` ya no publica puertos de `app` ni de `db` (solo red interna `reservahub-backend`); `compose.dev.yaml` publica 8080 y 5433 únicamente en `127.0.0.1`; la API se conecta con el rol `reservahub_app` (solo DML, `docker/db-init/01-app-user.sh`) y Flyway con el dueño. Prueba: `DeploymentConfigTest`. Evidencia: [evidencias/V-07-puertos-bd.txt](evidencias/V-07-puertos-bd.txt).

### V-08 · Sin cabeceras de seguridad HTTP — Media
- **OWASP**: A02:2025 · API8:2023.
- **Evidencia**: `nginx.conf` no tiene ninguna directiva `add_header`.
- **Escenario**: sin `Content-Security-Policy`, un XSS puede cargar scripts externos y robar el JWT (V-09); sin `X-Frame-Options`/`frame-ancestors` es posible el clickjacking; sin HSTS es posible el downgrade a HTTP.
- **Recomendación**: añadir `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` y `Referrer-Policy: no-referrer`.
- **Corrección aplicada (H3)**: backend O3 (`SecurityConfig`: CSP `default-src 'none'`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, nosniff, `Cache-Control: no-store`, HSTS) y front O8 (`nginx.conf`: HSTS, CSP estricta para la SPA, nosniff, XFO, Referrer y Permissions-Policy con `always` en cada `location`, `server_tokens off`). Pruebas: `ApiSecurityHeadersTest`, `src/seguridad/nginx.test.ts`, e2e que falla ante violaciones de CSP. Evidencia: [evidencias/V-08-cabeceras.txt](evidencias/V-08-cabeceras.txt).

### V-09 · JWT en `sessionStorage` — Baja
- **OWASP**: A07:2025 · API2:2023.
- **Evidencia**: `src/auth/session.ts:47,65` (frontend) guarda la sesión con el token en `sessionStorage`.
- **Escenario**: cualquier XSS en la SPA puede leer el token y usarlo hasta que expire (1 h). El riesgo es bajo porque React escapa la salida por defecto y `sessionStorage` se borra al cerrar la pestaña.
- **Recomendación**: a mediano plazo, mover el token a una cookie `HttpOnly; Secure; SameSite=Strict` (y entonces reactivar CSRF), o acortar la expiración y añadir refresh tokens; mientras tanto, aplicar CSP (V-08).
- **Corrección aplicada (H3)**: front O8 + O9 (commits `e2720ad`, `10373f9`). Mitigación sin rediseño: CSP estricta que bloquea scripts inyectados y la sesión del front caduca sola con la expiración del JWT (30 min). **Riesgo residual aceptado**: el token sigue en `sessionStorage` (mover a cookie `HttpOnly` queda fuera de alcance). Pruebas: `src/auth/session.test.ts`, `src/seguridad/nginx.test.ts`. Evidencia: [evidencias/V-09-csp-sesion.txt](evidencias/V-09-csp-sesion.txt).

### V-10 · Contenedores como root e imágenes sin digest — Baja
- **OWASP**: A02:2025 · A03:2025 Fallas en la cadena de suministro.
- **Evidencia**: `Dockerfile` (backend) no declara `USER`; las imágenes base (`eclipse-temurin:25-jre`, `postgres:16-alpine`, `nginx:1.27-alpine`) se referencian por etiqueta móvil.
- **Recomendación**: crear y usar un usuario sin privilegios (`USER app`), fijar las imágenes por `@sha256:` y escanearlas (Trivy, Docker Scout).
- **Corrección aplicada (H3)**: backend O7 (`Dockerfile` con bases `@sha256:` y `USER app` uid 10001; `postgres:16-alpine@sha256:…`) y front O8/O9 (`nginxinc/nginx-unprivileged:1.27-alpine@sha256:…`, `node:22-alpine@sha256:…`). Pruebas: `DeploymentConfigTest`, `src/seguridad/contenedor.test.ts`. Evidencia: [evidencias/V-10-no-root-digest.txt](evidencias/V-10-no-root-digest.txt).

### V-11 · Sin análisis de dependencias (SCA) — Baja
- **OWASP**: A03:2025 · API10:2023.
- **Evidencia**: `.github/workflows/sonarcloud.yml` ejecuta SonarQube Cloud (análisis estático del código), pero no hay Dependabot, OWASP Dependency-Check ni `npm audit` en el CI.
- **Recomendación**: activar Dependabot en ambos repositorios y añadir `dependency-check-maven` u `osv-scanner` al pipeline, haciendo fallar el build con CVEs de severidad alta.
- **Nota positiva**: las GitHub Actions ya están fijadas por SHA (buena práctica contra ataques a la cadena de suministro).
- **Corrección aplicada (H3)**: backend O7/O10 y front O9. Dependabot (`maven`/`npm`, `docker`, `github-actions`) y workflow `seguridad.yml` (OSV-Scanner + Trivy, falla con HIGH/CRITICAL) en ambos repos; `pnpm audit` limpio (override de `undici`); OSV-Scanner local halló 18 vulnerabilidades en Tomcat y Jackson, corregidas fijando Tomcat 11.0.25 y Jackson 2.21.7/3.1.7 en `pom.xml` (commit `b7819e8`). El escaneo de imagen con Trivy corre solo en CI. Pruebas: `DeploymentConfigTest`, `src/seguridad/cadena.test.ts`. Evidencia: [evidencias/V-11-dependencias.txt](evidencias/V-11-dependencias.txt).

### V-12 · Auditoría sin centralización ni alertas — Baja
- **OWASP**: A09:2025 Fallas en registro y alertas.
- **Evidencia**: `AuditLogger` solo escribe en el log de la aplicación (`log.info("audit action=…")`); existen eventos `LOGIN_FAILED`, `LOGIN_LOCKED`, `SERVICE_CREATED`, etc., pero no se envían a un sistema central ni disparan alertas.
- **Recomendación**: enviar los logs a un sistema central (ELK, Grafana Loki, Azure Monitor) y crear alertas por picos de `LOGIN_FAILED`/`LOGIN_LOCKED` o de respuestas 403.
- **Corrección aplicada (H3)**: backend O6 (commit `5e770c4`, merge `d289fe7`) y O10 (commit `e062b80`, `traceId` duplicado en el JSON). Logs estructurados JSON (`LOG_FORMAT=logstash`) con acción, actor, IP, resultado y `traceId`; contadores Micrometer; `scripts/alerta-seguridad.sh` alerta por umbrales de logins fallidos, bloqueos, 429 y altas de proveedor (`docs/operacion/alertas.md`). **Mitigada**: sin ELK/Loki centralizado. Prueba: `AuditLoggerTest`. Evidencia: [evidencias/V-12-auditoria-alerta.txt](evidencias/V-12-auditoria-alerta.txt).

### V-13 · Auto-registro abierto de proveedores — Baja
- **OWASP**: A06:2025 Diseño inseguro · API6:2023.
- **Evidencia**: `SecurityConfig.java:56` → `POST /api/v1/providers` con `permitAll()`; cualquier persona obtiene el rol `PROVIDER` (incluido el acceso a `users/lookup`, V-05).
- **Recomendación**: verificación del correo y/o aprobación por un `ADMIN` antes de activar el proveedor (estado `PENDING`).
- **Corrección aplicada (H3)**: backend O4 + O6. Alta de proveedor limitada a 3 por hora por IP y auditada (`PROVIDER_REGISTERED`), incluida en la alerta por umbral. **Mitigada**: sin verificación de correo ni aprobación por `ADMIN` (ADR-016). Pruebas: `RateLimitCoverageTest`, `AuditLoggerTest`. Evidencia: [evidencias/V-13-alta-proveedores.txt](evidencias/V-13-alta-proveedores.txt).

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

> **Ejecutado** en el hito H3 (sesiones O1–O11, ramas `fix/owasp` de backend y frontend). El estado de cada V-xx está en la tabla de la sección 2, el detalle en cada «Corrección aplicada» y el antes/después en [`correcciones-owasp.md`](correcciones-owasp.md). Diferencias con este plan: sin paginación (rangos acotados a 31 días), bloqueo por correo + IP con 401 genérico en vez de CAPTCHA, y V-09/V-13 mitigadas sin cookie HttpOnly ni aprobación de proveedores.

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
