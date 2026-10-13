# PR: `feature/frontend-inicial` → `develop` — Lecturas y CORS para el frontend (F1 + F2 + F2b)

Nota: lo abre y empuja el usuario. **Se fusiona después del PR `feature/sprint-2` → `develop`** (esta rama sale de `feature/sprint-2`; si se abre antes, el diff arrastra todo el Sprint 2). El frontend (repo `CodeFactory2026-2-ReservationHub-frontend`, `main`) depende de estos endpoints.

## Endpoints nuevos (todos `GET`, con JWT)
- **F1 · Catálogo** `GET /api/v1/catalog/services` (CLIENT, PROVIDER): servicios activos de proveedores activos (`CatalogEntryDTO`: id, name, description, durationMinutes, price, providerId, providerName), ordenados por proveedor y nombre.
- **F1 · Mis reservas / agenda** `GET /api/v1/reservations/mine?from&to` (CLIENT, PROVIDER): el cliente ve las suyas, el proveedor su agenda; todos los estados, en orden cronológico; rango máx. 31 días inclusive; proveedor sin perfil → 404.
- **F2 · Lo del proveedor** (PROVIDER):
  - `GET /api/v1/services/mine` → `ProviderServiceDTO` (servicio + `resources[]`, activos e inactivos).
  - `GET /api/v1/resources/mine` → `ResourceResponseDTO[]`.
  - `GET /api/v1/schedules/mine` → `ProviderScheduleDTO` (+ `blocks[]`).
- **F2 · Buscar cliente** `GET /api/v1/users/lookup?email=` (solo PROVIDER): `{id, name}` de un CLIENT activo; cualquier otro caso 404 (no revela si el email existe).

## CORS
- Activo en `/api/**` para los orígenes de `app.cors.allowed-origins`, variable de entorno `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:5173`, el Vite del front). En despliegue hay que fijarla al dominio del frontend.

## F2b · Parámetros inválidos → 400
- Antes, un parámetro de consulta ausente (`/reservations/mine` sin `from`) o mal escrito (`from=abc`, id que no es UUID) respondía **500**. Ahora `GlobalExceptionHandler` maneja `MissingServletRequestParameterException` y `MethodArgumentTypeMismatchException` → **400 `VALIDATION_ERROR`** con el parámetro en `details`. Documentado en `docs/api/errors.md`.

## Pruebas
- `./mvnw test`: 110/110 en verde (Sprint 2 cerró con 92).
- Nuevas: `ReadEndpointsTest` (F1), `ProviderReadTest` y `CorsTest` (F2), `InvalidParamsTest` (F2b); la matriz de permisos por rol cubre los endpoints nuevos.

## Docs
`docs/api/lecturas.md` (contrato de las lecturas), `docs/api/errors.md`, carpeta «Lecturas» en la colección Postman.
