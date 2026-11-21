# Reportes — Sprint 3

Prefijo `/api/v1/reports`. Todos exigen `Authorization: Bearer`. `from` y `to` son fechas `YYYY-MM-DD`, inclusive, máximo 31 días. Si falta un parámetro, `from` es posterior a `to` o el rango se pasa, la respuesta es 400.

| Método | Ruta | Rol | HU |
|--------|------|-----|----|
| GET | `/api/v1/reports/my-reservations` | CLIENT | HU-17 |
| GET | `/api/v1/reports/business-reservations` | PROVIDER | HU-18 |
| GET | `/api/v1/reports/occupancy` | PROVIDER | HU-19 |
| GET | `/api/v1/reports/demand` | PROVIDER | HU-20 |
| GET | `/api/v1/reports/cancellations` | PROVIDER | HU-21 |

`occupancy` y `cancellations` aceptan `serviceId`. Si el servicio no es del proveedor, responden 404.

## Ciclo de vida

`status` es lo que está guardado. `lifecycle` es lo que se muestra en el historial. Se calcula en `America/Bogota` (`ReservationLifecycle`) y en SQL (`reservation_lifecycle`).

| lifecycle | En la historia | Cuándo |
|-----------|----------------|--------|
| `PENDING` | pendiente | `CONFIRMED` y todavía no empieza |
| `CONFIRMED` | confirmada, en curso | `CONFIRMED` y la hora cae dentro del turno |
| `COMPLETED` | completada | `CONFIRMED` y el turno ya terminó |
| `CANCELLED` | cancelada | `CANCELLED_BY_CLIENT` o `CANCELLED_BY_PROVIDER` |

HU-17 y HU-18 devuelven servicio, proveedor, cliente, fecha, hora, `status` y `lifecycle`. El nombre del cliente es nombre y apellido. El del proveedor es el nombre del negocio. Entran reservas futuras, pasadas, canceladas y completadas.

## Ocupación, demanda y cancelaciones

HU-19: `occupancyRate` = `occupiedSlots / offeredSlots` (4 decimales). `offeredSlots` son los huecos de la agenda vigente, sin contar bloqueos. `occupiedSlots` son los que una reserva confirmada ya llenó. Si ese día no hay agenda, la tasa es 0. `byService` y `byDate` parten el mismo cálculo.

HU-20: `deliveredCount` cuenta reservas `CONFIRMED` del periodo. Una cancelación no suma.

HU-21: `cancelledByClient` y `cancelledByProvider` salen del `status` guardado.

## Ejemplo

```http
GET /api/v1/reports/occupancy?from=2026-11-22&to=2026-11-22&serviceId=<uuid>
Authorization: Bearer <token del proveedor>
```
