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
| 500 | `INTERNAL_ERROR` |

El header de respuesta `X-Trace-Id` coincide con `traceId` cuando el filtro de observabilidad está activo.
