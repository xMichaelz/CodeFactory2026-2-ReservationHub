# fix/owasp: corrección de las 13 vulnerabilidades del informe OWASP

Informe de partida: `docs/entregable-arquisoft-sprint2/vulnerabilidades-owasp.md`. Evidencias: `docs/entregable-arquisoft-sprint2/evidencias/` (cada V-xx enlaza a su archivo). Backend: 147 tests + 1 nuevo, sin omitidos; front: 217 tests, build limpio; e2e 9/9 por HTTPS contra Docker.

## Qué se hizo por vulnerabilidad

| V | Qué se hizo | Evidencia |
|---|---|---|
| V-01 | Sin secretos por defecto (`${JWT_SECRET}`, `${DB_*}`); `compose.yaml` con `${VAR:?}`; `.env.example`; la API no arranca sin JWT_SECRET | V-01-secretos.txt |
| V-02 | Nginx con TLS 1.2/1.3 (certificado autofirmado por `scripts/generar-certificado.sh`), 8081 redirige 301 a 8443, HSTS | V-02-tls.txt |
| V-03 | Bucket4j en la API (login por IP, registros, lookup, lecturas) con 429 + `Retry-After`; `limit_req` en Nginx como 2.ª capa | V-03-rate-limit.txt |
| V-04 | Bloqueo por pareja email+IP con 401 genérico, BCrypt contra hash ficticio; desaparece el 423 `ACCOUNT_LOCKED` | V-04-login-bloqueo.txt |
| V-05 | 409 con mensaje genérico, evento `REGISTRATION_REJECTED` y `CLIENT_LOOKUP`; riesgo residual aceptado (ADR-016) | V-05-registro-enumeracion.txt |
| V-06 | Swagger apagado salvo `SWAGGER_ENABLED=true` (404 al anónimo) | V-06-swagger.txt |
| V-07 | Compose de despliegue sin puertos publicados; BD con usuario `reservahub_app` sin DDL; `compose.dev.yaml` publica 127.0.0.1:8080/5433 | V-07-puertos-bd.txt |
| V-08 | CSP, HSTS, X-Frame-Options, nosniff, Referrer-Policy, Permissions-Policy, no-store en SPA y API | V-08-cabeceras.txt |
| V-09 | Mitigada: CSP estricta y sesión que caduca sola; el token sigue en `sessionStorage` | V-09-csp-sesion.txt |
| V-10 | Contenedores sin root (API uid 10001, Nginx `nginx-unprivileged`), imágenes por digest | V-10-no-root-digest.txt |
| V-11 | Dependabot + workflow `seguridad.yml` (OSV-Scanner + Trivy) en ambos repos; `pnpm audit` limpio; se fijaron Tomcat 11.0.25 y Jackson 2.21.7/3.1.7 (OSV-Scanner halló 18 vulnerabilidades) | V-11-dependencias.txt |
| V-12 | Auditoría en JSON (logstash) con contadores Micrometer y `scripts/alerta-seguridad.sh`; arreglado el `traceId` duplicado que tiraba las líneas | V-12-auditoria-alerta.txt |
| V-13 | Mitigada: 3 altas de proveedor por hora y por IP + evento `PROVIDER_REGISTERED` | V-13-alta-proveedores.txt |

## Cambios que rompen

- **`.env` obligatorio**: `DB_PASSWORD` (dueño, Flyway), `DB_APP_PASSWORD` (usuario de la API), `JWT_SECRET` (>= 32 bytes). Sin ellos `docker compose up` falla.
- **`docker compose down -v` la primera vez**: el volumen de la BD cambió (usuario de aplicación sin DDL); lo ejecuta el usuario, perdiendo los datos de desarrollo.
- **Puertos**: la API y la BD ya no se publican. El front sirve `8081` (redirige) y `8443` (HTTPS). Para Postman/DBeaver: `docker compose -f compose.yaml -f compose.dev.yaml up`.
- **Orden de arranque**: primero el backend (crea la red `reservahub-backend`), luego el front (`scripts/generar-certificado.sh` antes de la primera vez).
- **423 `ACCOUNT_LOCKED` eliminado**: el login siempre responde 401 genérico; aparece el 429 `RATE_LIMITED`.
- Rutas sin recurso responden 404; Swagger apagado por defecto.
- Hay que reenviar `RATE_LIMIT_*` a la API si se quieren cambiar los límites desde el compose (hoy `compose.yaml` no las reenvía; para e2e se usa un override con `RATE_LIMIT_ENABLED=false`).

## Orden de fusión

1. Backend `fix/owasp` -> `develop`.
2. Front `fix/owasp` -> `main` (necesita la red y el servicio `app` del backend ya fusionado).

## Diagrama de despliegue (pendiente de regenerar)

`diagrama-despliegue.drawio/.png/.pdf` sigue mostrando el estado anterior. Debe reflejar: Nginx con `8443` TLS y `8081` redirigiendo (ya no `8081 -> 80`); imagen `nginx-unprivileged`; API (`expose 8080`) y BD sin puertos al host; red `reservahub-backend`; nota de `compose.dev.yaml` (127.0.0.1:8080/5433). Se regenera con `fuentes-diagrama/generar_diagrama.py` (sesión O11).

## Pendiente conocido

- Trivy y el workflow `seguridad.yml` solo se ejecutan en CI.
- V-05 (409 distingue correos existentes) y V-09 (token en `sessionStorage`) quedan como riesgo residual.
