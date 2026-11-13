# APIs priorizadas — ReservaHub

Inventario de los endpoints declarados en los controllers (`@RequestMapping` y métodos HTTP).
Los roles salen de `SecurityConfig` y de la matriz `EndpointSecurityMatrixTest`.
La prioridad MoSCoW dice qué tan necesario es el endpoint para reservar un servicio en el Sprint 2.

Prefijo vigente: `/api/v1`.

| Método | Ruta | HU | Rol | MoSCoW | Justificación |
|--------|------|----|-----|--------|----------------|
| POST | `/api/v1/users` | HU-01 | Público | Must | Sin cliente no hay quien reserve. `UserController`. |
| POST | `/api/v1/providers` | HU-02 | Público | Must | Sin proveedor no hay agenda ni servicios. `ProviderController`. |
| POST | `/api/v1/auth/login` | HU-03 | Público | Must | El resto de la API exige JWT. `AuthController`. |
| POST | `/api/v1/auth/logout` | HU-04 | Autenticado | Should | Revoca el `jti` del token. El acceso también vence solo a la hora. |
| POST | `/api/v1/services` | HU-05 | PROVIDER | Must | La reserva apunta a un servicio. Sin crearlo, HU-11 y HU-12 no tienen objeto. |
| PUT | `/api/v1/services/{id}` | HU-06 | PROVIDER | Should | Corrige precio, duración o nombre. No hace falta para la primera reserva. |
| PATCH | `/api/v1/services/{id}/deactivate` | HU-07 | PROVIDER | Should | Saca un servicio de la oferta sin borrar historial. |
| POST | `/api/v1/services/{id}/resources` | #205 | PROVIDER | Should | Asocia recursos (cupos) al servicio. La reserva ocupa una unidad. |
| GET | `/api/v1/services/mine` | Lectura F2 | PROVIDER | Could | Lista la oferta propia para el frontend. Se puede operar con el id ya conocido. |
| GET | `/api/v1/services/{serviceId}/availability` | HU-11 | CLIENT, PROVIDER | Must | El cliente solo puede reservar un hueco que esta consulta ofrece. |
| GET | `/api/v1/catalog/services` | Lectura F1 | CLIENT, PROVIDER | Should | Muestra servicios activos para elegir cuál reservar. |
| POST | `/api/v1/schedules` | HU-08 | PROVIDER | Must | La disponibilidad sale de la ventana de atención. Sin horario no hay huecos. |
| POST | `/api/v1/schedules/{id}/blocks` | HU-09 | PROVIDER | Should | Bloquea un tramo. La reserva funciona sin bloqueos. |
| GET | `/api/v1/schedules/mine` | Lectura F2 | PROVIDER | Could | Consulta de apoyo para la pantalla del proveedor. |
| POST | `/api/v1/resources` | HU-10 | PROVIDER | Should | Crea el recurso que luego se asocia al servicio. |
| GET | `/api/v1/resources/mine` | Lectura F2 | PROVIDER | Could | Consulta de apoyo. No participa en el cálculo de la reserva. |
| GET | `/api/v1/users/lookup` | Apoyo HU-13 | PROVIDER | Should | El proveedor busca un cliente activo por email antes de reservar a su nombre. |
| POST | `/api/v1/reservations` | HU-12, HU-13 | CLIENT, PROVIDER | Must | Crea la reserva. El cliente reserva para sí (HU-12). El proveedor indica `clientId` (HU-13). |
| GET | `/api/v1/reservations/mine` | Lectura F1 | CLIENT, PROVIDER | Should | Historial del cliente y agenda del proveedor. Exige `from` y `to`. |
| PATCH | `/api/v1/reservations/{id}/cancel` | HU-14, HU-15 | CLIENT, PROVIDER | Must | Libera el cupo. El proveedor debe enviar motivo (HU-15). |
| PATCH | `/api/v1/reservations/{id}/reschedule` | HU-16 | CLIENT, PROVIDER | Should | Mueve la reserva a otro hueco libre. Si no está libre, la reserva no cambia. |

Actuator (`/actuator/health`) y Swagger (`/swagger-ui.html`, `/v3/api-docs`) no están en esta tabla: no los declara un controller de negocio.

## Versionado y retiro

La API publicada es `/api/v1`. El contrato de error vive en `docs/api/errors.md`.

- Un cambio que solo agrega campos o endpoints se queda en `/api/v1`.
- Un cambio que rompe un cliente (ruta, método, significado de un campo o código de error) se publica en `/api/v2`. `/api/v1` sigue respondiendo durante el sprint siguiente.
- Antes de retirar una versión se avisa en el README y en la descripción de Swagger. Hoy no se envía el header `Sunset`: queda pendiente.
- No hay otra versión mayor en el código. Retirar `/api/v1` sin `/api/v2` dejaría al frontend sin contrato.
