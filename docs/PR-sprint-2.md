# PR: `feature/sprint-2` → `develop` — Sprint 2 (EP-03 Gestión de Reservas)

Nota: lo abre y empuja el usuario.

## Historias de usuario
- **HU-11** Disponibilidad: `GET /api/v1/services/{id}/availability?from&to` (máx. 31 días; solo días con huecos).
- **HU-12** Reservar como cliente: `POST /api/v1/reservations`; solo huecos ofrecidos; concurrencia con `SELECT … FOR UPDATE` sobre el proveedor (ADR-015).
- **HU-13** Reservar como proveedor a nombre de un cliente (403 servicio/cliente ajeno, 404 cliente inexistente, 400 si no es CLIENT activo).
- **HU-14 / HU-15** Cancelar: `PATCH /api/v1/reservations/{id}/cancel` (cliente; proveedor con motivo obligatorio); el hueco se libera y queda en el historial.
- **HU-16** Reprogramar: `PATCH /api/v1/reservations/{id}/reschedule`; si el nuevo horario no está libre la reserva queda igual; conflicto de versión → 409.

## Bugs del Sprint 1
- **#201, #203, #204, #205** corregidos en S1 (seguridad / validaciones); **#202** bloqueo de cuenta 15 min tras 5 claves erróneas (423 `ACCOUNT_LOCKED`) en S2.

## Base de datos y modelo
- `V5__reservations.sql`: tablas de reservas e historial. Recursos por cantidad (cada reserva ocupa 1 unidad).

## Pruebas
- `./mvnw verify`: 92/92 en verde; cobertura de instrucciones JaCoCo ≈ 90,6 %.
- `Sprint2AcceptanceTest` (recorrido de punta a punta) y colección `postman/ReservaHub-Sprint2.postman_collection.json` (23/23 aserciones con newman contra `docker compose up --build`).

## Docs
`overview.md`, `assumptions.md` (zona horaria, regla de recursos, cuadrícula), `diagrams.md`, `ADR-015`.
