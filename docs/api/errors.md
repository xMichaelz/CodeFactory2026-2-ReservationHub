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
| 423 | `ACCOUNT_LOCKED` |
| 429 | `RATE_LIMITED` |
| 500 | `INTERNAL_ERROR` |

El header de respuesta `X-Trace-Id` coincide con `traceId` cuando el filtro de observabilidad está activo.

## Bloqueo por intentos fallidos (`ACCOUNT_LOCKED`)

`POST /api/v1/auth/login` cuenta los fallos de contraseña de cada cuenta existente y activa.

- Al llegar a `app.security.lockout.max-attempts` fallos (5 por defecto), la cuenta queda bloqueada durante `app.security.lockout.duration-minutes` minutos (15 por defecto).
- Mientras dure el bloqueo, el login responde **423** con `errorCode: ACCOUNT_LOCKED`, aunque la contraseña sea correcta. El header `Retry-After` trae los segundos que faltan para el desbloqueo. Los intentos hechos durante el bloqueo no se cuentan.
- Cuando el bloqueo vence, el contador vuelve a 0. Un login correcto también lo pone en 0.
- Un email que no existe, o una cuenta inactiva, siempre responde **401** `UNAUTHORIZED` con el mensaje genérico `Invalid credentials` y no se bloquea nunca.
- En la auditoría quedan las acciones `LOGIN_FAILED` y `LOGIN_LOCKED`.

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
