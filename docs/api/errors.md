# Contrato de errores API

Formato uniforme:

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Resource not found",
  "details": [],
  "traceId": "uuid"
}
```

| HTTP | errorCode típico |
|------|------------------|
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST` |
| 401 | `UNAUTHORIZED` |
| 403 | `FORBIDDEN` |
| 404 | `RESOURCE_NOT_FOUND` |
| 409 | `CONFLICT` |
| 429 | `RATE_LIMITED` |
| 500 | `INTERNAL_ERROR` |

El header de respuesta `X-Trace-Id` coincide con `traceId` cuando el filtro de observabilidad está activo.

## Login: bloqueo por email + IP con 401 genérico (V-04)

`POST /api/v1/auth/login` cuenta los fallos de cada **pareja email + IP del cliente** (la IP real, resuelta igual que en el límite de peticiones).

- Al llegar a `app.security.lockout.max-attempts` fallos (5 por defecto) la pareja queda frenada `app.security.lockout.duration-minutes` minutos (15 por defecto). La misma cuenta sigue entrando desde otra IP, así que un atacante no puede bloquear a la víctima.
- Email inexistente, cuenta inactiva, clave errónea y pareja frenada responden **lo mismo**: **401** `UNAUTHORIZED`, mensaje `Invalid credentials`, sin `Retry-After`. Siempre se hace una comparación BCrypt (contra un hash ficticio si no hay cuenta o la pareja está frenada), para que el tiempo de respuesta no delate si el email existe.
- La racha se olvida al vencer el bloqueo o tras `duration-minutes` sin fallos; un login correcto la pone en 0. El contador vive en memoria (Caffeine, máx. 100 000 parejas) y se pierde al reiniciar.
- **Ya no existe el 423 `ACCOUNT_LOCKED`** (el front debe dejar de esperarlo, sesión O9).
- Auditoría: `LOGIN_FAILED` y `LOGIN_LOCKED` con `ip=` (y `userId=none` si el email no existe), `LOGIN_SUCCESS` con IP.

## Registro duplicado (V-05)

`POST /api/v1/users` (email repetido) y `POST /api/v1/providers` (email o nombre de negocio repetido) responden **409** `CONFLICT` con el mismo mensaje genérico: `No se pudo completar el registro con los datos enviados`. Queda el evento `REGISTRATION_REJECTED` con rol, campo e IP, sin el email en claro. El 409 sigue indicando que algo ya existe: riesgo residual aceptado en [ADR-016](../adr/ADR-016.md).

## Búsqueda de clientes (`GET /api/v1/users/lookup`)

Cada búsqueda deja `CLIENT_LOOKUP` con `actor` (id del proveedor), `found=true|false` y `emailHash` (SHA-256 hex del email normalizado en minúsculas), nunca el email en claro. El límite por usuario lo pone el rate limit (`lookup-per-minute`).

## Límite de peticiones (`RATE_LIMITED`)

Los endpoints públicos se limitan por IP del cliente y los de lectura costosa por usuario autenticado (V-03). Al pasarse del cupo la API responde **429** con `errorCode: RATE_LIMITED`, el header `Retry-After` (segundos hasta que se recarga el cupo) y no ejecuta la petición:

```json
{
  "errorCode": "RATE_LIMITED",
  "message": "Too many requests, try again later",
  "details": [],
  "traceId": "uuid"
}
```

| Ruta | Se cuenta por | Límite por defecto | Propiedad / variable |
|------|---------------|--------------------|----------------------|
| `POST /api/v1/auth/login` | IP | 10 por minuto | `app.rate-limit.login-per-minute` / `RATE_LIMIT_LOGIN_PER_MINUTE` |
| `POST /api/v1/users` | IP | 20 por hora | `app.rate-limit.registration-per-hour` / `RATE_LIMIT_REGISTRATION_PER_HOUR` |
| `POST /api/v1/providers` | IP | 3 por hora | `app.rate-limit.provider-registration-per-hour` / `RATE_LIMIT_PROVIDER_REGISTRATION_PER_HOUR` |
| `GET /api/v1/services/{id}/availability`, `GET /api/v1/catalog/services`, `GET /api/v1/reservations/mine` (cupo compartido) | usuario | 60 por minuto | `app.rate-limit.authenticated-per-minute` / `RATE_LIMIT_AUTHENTICATED_PER_MINUTE` |
| `GET /api/v1/users/lookup` | usuario | 10 por minuto | `app.rate-limit.lookup-per-minute` / `RATE_LIMIT_LOOKUP_PER_MINUTE` |

- El cupo se recarga entero al terminar cada periodo (minuto u hora). Los cubos viven en memoria de cada instancia y caducan si la clave deja de usarse.
- `app.rate-limit.enabled=false` (`RATE_LIMIT_ENABLED`) apaga el límite.
- Un anónimo que llama a un endpoint autenticado no gasta cupo: recibe 401 como siempre.
- **IP real detrás de Nginx**: el `X-Forwarded-For` solo se cree si la petición llega de un proxy de confianza, `app.rate-limit.trusted-proxies` (`RATE_LIMIT_TRUSTED_PROXIES`, regex; por defecto loopback, `172.16.0.0/12` y `192.168.0.0/16`, las redes de Docker). Tomcat usa el mismo valor (`server.forward-headers-strategy=native`, `server.tomcat.remoteip.internal-proxies`). Desde cualquier otra IP la cabecera se ignora, así que inventarla no evade el límite.
- Cada 429 queda en la auditoría como `RATE_LIMITED` con regla, método, ruta, IP y, si aplica, el id de usuario (nunca el token).

## Parámetros de consulta o de ruta inválidos

Un parámetro obligatorio ausente (p. ej. `GET /api/v1/reservations/mine` sin `from`) o uno que no se puede convertir (fecha que no es ISO `yyyy-MM-dd`, id que no es UUID) responde **400** `VALIDATION_ERROR` con el parámetro en `details`:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": ["from: invalid value, expected LocalDate"],
  "traceId": "uuid"
}
```

Si falta: `"details": ["from: is required"]`.
