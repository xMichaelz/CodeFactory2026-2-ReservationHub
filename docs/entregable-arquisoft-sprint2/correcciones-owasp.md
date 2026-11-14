# Informe final de correcciones OWASP — ReservaHub (H3)

Cierre del hito H3: las 13 vulnerabilidades de [`vulnerabilidades-owasp.md`](vulnerabilidades-owasp.md) quedan **corregidas (9)**, **corregida con riesgo residual (1)** o **mitigadas (3)**, cada una con una prueba automática que la vigila y una evidencia tomada contra el stack Docker real (carpeta [`evidencias/`](evidencias/README.md)).

- Ramas: `fix/owasp` en el backend (`CodeFactory2026-2-ReservationHub`) y en el frontend (`CodeFactory2026-2-ReservationHub-frontend`). Sin push ni PR todavía; el texto del PR está en `docs/PR-fix-owasp.md` del backend.
- Estado de las suites (2026-11-14): backend `./mvnw verify` 148 tests, 0 fallos, 0 omitidos; frontend `pnpm test` 217/217, build y typecheck limpios; e2e Playwright 9/9 por `https://localhost:8443` contra los contenedores.
- No queda ninguna prueba marcada «pendiente V-xx».

## 1. Antes y después por vulnerabilidad

| V | Sev. | Antes | Después | Estado | Sesión |
|---|---|---|---|---|---|
| V-01 | Alta | Secreto JWT y contraseña de BD por defecto en `application.properties` y `compose.yaml` | Sin valores por defecto; la API no arranca sin `JWT_SECRET` (≥ 32 bytes); compose exige `${VAR:?…}`; `.env.example`; token de 30 min | Corregida | O3 |
| V-02 | Alta | Todo por HTTP en 8081 | Nginx con TLS 1.2/1.3 + http2 en **8443** (certificado autofirmado generado por script); **8081 → 301** a https; HSTS | Corregida | O8 |
| V-03 | Media | Sin límite de peticiones | Bucket4j: login 10/min IP, registro 20/h IP, alta de proveedor 3/h IP, lecturas costosas 60/min usuario, lookup 10/min usuario → 429 `RATE_LIMITED` + `Retry-After`; `limit_req` 20 r/s en Nginx | Corregida | O4, O8 |
| V-04 | Media | 5 fallos bloqueaban la cuenta (423) para cualquiera y delataban cuentas | Siempre 401 genérico; bloqueo por pareja correo + IP; sin 423 ni `ACCOUNT_LOCKED` | Corregida | O5, O9 |
| V-05 | Media | 409 «correo ya registrado» y lookup sin control | 409 con mensaje genérico, límite por IP y auditoría; lookup limitado y auditado | Corregida con riesgo residual | O5 |
| V-06 | Media | Swagger y `/v3/api-docs` públicos | 404 salvo `SWAGGER_ENABLED=true` (solo `compose.dev.yaml`) | Corregida | O3 |
| V-07 | Media | BD publicada en 5433 y API en 8080 | `compose.yaml` sin `ports` en `app` ni `db`; red `reservahub-backend`; la API usa `reservahub_app` (solo DML) | Corregida | O7 |
| V-08 | Media | Sin CSP, HSTS, XFO… | Cabeceras completas en la API (`SecurityConfig`) y en Nginx (`always` en cada `location`) | Corregida | O3, O8 |
| V-09 | Baja | JWT en `sessionStorage`, sin CSP, sesión sin caducidad en el front | CSP estricta + la sesión del front caduca con el token | Mitigada (riesgo residual) | O8, O9 |
| V-10 | Baja | Contenedores como root, imágenes por etiqueta | Bases por `@sha256:`; `USER app` (10001) y `nginx-unprivileged` | Corregida | O7, O8, O9 |
| V-11 | Baja | Sin SCA | Dependabot + `seguridad.yml` (OSV-Scanner + Trivy) en ambos repos; Tomcat 11.0.25 y Jackson parcheados; `pnpm audit` limpio | Corregida (Trivy en CI) | O7, O9, O10 |
| V-12 | Baja | Logs de texto locales, sin alertas | Logs JSON (acción, actor, IP, resultado, `traceId`), métricas Micrometer y `scripts/alerta-seguridad.sh` por umbral | Mitigada (sin ELK) | O6, O10 |
| V-13 | Baja | Alta de proveedores abierta sin control | 3 altas/h por IP, auditoría `PROVIDER_REGISTERED` y alerta | Mitigada (sin aprobación) | O4, O6 |

### Riesgos residuales aceptados
- **V-05** — el registro mantiene el código 409 porque el front y la HU-02 lo usan; el mensaje es genérico, hay límite por IP y queda auditado. Documentado en `docs/adr/ADR-016.md`.
- **V-09** — el token sigue en `sessionStorage`; pasar a cookie `HttpOnly; Secure; SameSite=Strict` exige rediseñar la autenticación y queda fuera del alcance.
- **V-12 / V-13** — sin centralización de logs (ELK/Loki) ni aprobación de proveedores por un `ADMIN`.

## 2. Pruebas que lo garantizan

| Repo | Prueba | Vigila |
|---|---|---|
| backend | `security/owasp/SecretsConfigTest` | V-01: sin secretos por defecto, arranque falla sin `JWT_SECRET` |
| backend | `security/owasp/RateLimitCoverageTest`, `RateLimitDisabledTest` | V-03, V-13: todo endpoint público o costoso responde 429 (descubre los endpoints solo) |
| backend | `security/owasp/AuthEnumerationTest`, `AuthServiceTest`, `UserServiceTest` | V-04, V-05: 401 genérico, bloqueo correo + IP, 409 genérico |
| backend | `security/owasp/SwaggerExposureTest` | V-06 |
| backend | `security/owasp/ApiSecurityHeadersTest` | V-08 (API) |
| backend | `security/owasp/DeploymentConfigTest` | V-07, V-10, V-11: compose sin puertos, digest, `USER`, Dependabot, workflow SCA |
| backend | `audit/AuditLoggerTest` | V-12, V-13: formato del evento y `traceId` sin duplicar |
| front | `src/seguridad/nginx.test.ts` | V-02, V-03, V-08, V-09: TLS, redirección, cabeceras, CSP, `limit_req` |
| front | `src/seguridad/contenedor.test.ts` | V-10: imagen sin root y fijada |
| front | `src/seguridad/cadena.test.ts` | V-11: Dependabot, workflow, auditoría de dependencias |
| front | `src/auth/session.test.ts` | V-09: la sesión caduca sola |
| front | e2e Playwright (`e2e/`) por HTTPS | flujo completo; falla ante cualquier violación de CSP |

## 3. Cambios operativos (cómo se levanta ahora)

1. **Variables obligatorias** en `.env` del backend (copiar `.env.example`): `JWT_SECRET`, `DB_PASSWORD` (dueño `reservahub`, lo usa Flyway) y `DB_APP_PASSWORD` (usuario `reservahub_app`, lo usa la API). Opcionales: `SWAGGER_ENABLED`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `RATE_LIMIT_*`.
2. **Orden de arranque**: primero `docker compose up -d` en el backend (proyecto `reservahub`, crea la red `reservahub-backend`), luego en el front (proyecto `reservahub-front`, se une a esa red como externa).
3. **Certificado**: en el front, `scripts/generar-certificado.sh` crea `certs/` (ignorado por git) antes del primer arranque.
4. **Puertos publicados**: solo `8443` (HTTPS) y `8081` (redirige a HTTPS). La API y la BD no tienen puerto en el host.
5. **Desarrollo y Postman**: `docker compose -f compose.yaml -f compose.dev.yaml up -d` publica `127.0.0.1:8080` (API) y `127.0.0.1:5433` (BD) y enciende Swagger.
6. **Primera vez tras el cambio**: `docker compose down -v` (el volumen cambió de nombre con el proyecto `reservahub`; los datos de desarrollo se recrean).
7. **Alertas**: `scripts/alerta-seguridad.sh` sobre `docker logs reservahub-app` (umbrales en `docs/operacion/alertas.md`).
8. **e2e contra contenedores**: apagar el límite de la API con un override (`RATE_LIMIT_ENABLED: "false"` en `app`), porque todo el tráfico del host llega con la misma IP.

## 4. Pendiente fuera del repositorio
- Push de `fix/owasp` (backend y frontend) y apertura de los PR (texto en `docs/PR-fix-owasp.md`).
- Primera ejecución en CI de `seguridad.yml` (OSV-Scanner + Trivy de la imagen); Trivy no se ejecutó en local.
- En producción, sustituir el certificado autofirmado por uno de una CA (Let's Encrypt) y guardar los secretos en un gestor (GitHub Secrets / Vault).
