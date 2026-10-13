# Lecturas para el frontend

Endpoints de solo lectura que alimentan las pantallas del cliente y la agenda del proveedor. Requieren Bearer
con rol `CLIENT` o `PROVIDER` (sin token → 401; `ADMIN` → 403).

## `GET /api/v1/catalog/services`

Servicios **activos** de proveedores **activos**, ordenados por proveedor y nombre.

```json
[
  {
    "id": "…", "name": "Corte de cabello", "description": "…",
    "durationMinutes": 60, "price": 25000,
    "providerId": "…", "providerName": "Barbería Central"
  }
]
```

## `GET /api/v1/reservations/mine?from=YYYY-MM-DD&to=YYYY-MM-DD`

- **Cliente**: sus reservas.
- **Proveedor**: su agenda (las reservas de todos sus servicios).

Incluye todos los estados (`CONFIRMED`, `CANCELLED_BY_CLIENT`, `CANCELLED_BY_PROVIDER`) en orden de fecha y hora.
Cada elemento es un `ReservationResponseDTO` (igual que la respuesta de crear reserva).

| Caso | Respuesta |
|------|-----------|
| `from` > `to` | 400 `BAD_REQUEST` |
| Rango de más de 31 días (inclusive) | 400 `BAD_REQUEST` |
| Sin token | 401 |

Pruebas: `ReadEndpointsTest` y filas en `EndpointSecurityMatrixTest`. Postman: carpeta «Lecturas» de
`postman/ReservaHub-Sprint2.postman_collection.json` (al final del recorrido).
