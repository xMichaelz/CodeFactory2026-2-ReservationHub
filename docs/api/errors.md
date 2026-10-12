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
| 500 | `INTERNAL_ERROR` |

El header de respuesta `X-Trace-Id` coincide con `traceId` cuando el filtro de observabilidad está activo.

## Bloqueo por intentos fallidos (`ACCOUNT_LOCKED`)

`POST /api/v1/auth/login` cuenta los fallos de contraseña de cada cuenta existente y activa.

- Al llegar a `app.security.lockout.max-attempts` fallos (5 por defecto), la cuenta queda bloqueada durante `app.security.lockout.duration-minutes` minutos (15 por defecto).
- Mientras dure el bloqueo, el login responde **423** con `errorCode: ACCOUNT_LOCKED`, aunque la contraseña sea correcta. El header `Retry-After` trae los segundos que faltan para el desbloqueo. Los intentos hechos durante el bloqueo no se cuentan.
- Cuando el bloqueo vence, el contador vuelve a 0. Un login correcto también lo pone en 0.
- Un email que no existe, o una cuenta inactiva, siempre responde **401** `UNAUTHORIZED` con el mensaje genérico `Invalid credentials` y no se bloquea nunca.
- En la auditoría quedan las acciones `LOGIN_FAILED` y `LOGIN_LOCKED`.
