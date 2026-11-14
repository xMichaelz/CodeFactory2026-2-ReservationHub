# Priorización de APIs — ReservaHub (Sprint 2)

## 1. Alcance
Se inventariaron **21 endpoints de negocio** (`/api/v1/**`) y **2 de operación** (health y OpenAPI) a partir del código del backend (`*Controller.java` y `SecurityConfig.java`), rama `feature/frontend-inicial`.

## 2. Método
Puntuación ponderada (1 = bajo, 5 = alto) por cuatro criterios:

| Criterio | Peso | Qué mide |
|---|---|---|
| Valor de negocio | 35 % | Cuánto aporta al objetivo del sistema (reservar servicios). |
| Criticidad operativa | 25 % | Impacto si el endpoint falla o se degrada. |
| Riesgo de seguridad / exposición | 20 % | Público o no, datos sensibles, superficie de ataque (ver `vulnerabilidades-owasp.md`). |
| Dependencias | 20 % | Cuántos otros flujos lo necesitan antes de poder ejecutarse. |

**Puntaje = 0,35·V + 0,25·C + 0,20·R + 0,20·D**

| Rango | Prioridad | MoSCoW | Implicación |
|---|---|---|---|
| ≥ 4,0 | **P1 Crítica** | Must | Primero en pruebas, monitoreo, endurecimiento de seguridad y SLA. |
| 3,0 – 3,99 | **P2 Alta** | Should | Pruebas de contrato y de autorización obligatorias. |
| 2,5 – 2,99 | **P3 Media** | Could | Pruebas funcionales estándar. |
| < 2,5 | **P4 Baja** | Won't (por ahora) | Lecturas de apoyo / operación; esfuerzo mínimo. |

## 3. Resultado

| # | Método | Endpoint | Rol | V | C | R | D | Puntaje | Prioridad |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `POST` | `/api/v1/auth/login` | Iniciar sesión y obtener JWT | 5 | 5 | 5 | 5 | **5.00** | P1 Crítica |
| 2 | `POST` | `/api/v1/reservations` | Crear reserva (cliente o proveedor en nombre de un cliente) | 5 | 5 | 4 | 4 | **4.60** | P1 Crítica |
| 3 | `GET` | `/api/v1/services/{serviceId}/availability` | Franjas libres de un servicio (máx. 31 días) | 5 | 4 | 3 | 5 | **4.35** | P1 Crítica |
| 4 | `POST` | `/api/v1/users` | Registrar cliente | 4 | 4 | 4 | 5 | **4.20** | P1 Crítica |
| 5 | `POST` | `/api/v1/providers` | Registrar proveedor | 4 | 4 | 4 | 5 | **4.20** | P1 Crítica |
| 6 | `POST` | `/api/v1/services` | Crear servicio | 4 | 4 | 3 | 5 | **4.00** | P1 Crítica |
| 7 | `POST` | `/api/v1/schedules` | Definir horario de atención | 4 | 4 | 3 | 5 | **4.00** | P1 Crítica |
| 8 | `GET` | `/api/v1/catalog/services` | Catálogo de servicios activos | 5 | 4 | 2 | 4 | **3.95** | P2 Alta |
| 9 | `PATCH` | `/api/v1/reservations/{id}/cancel` | Cancelar reserva (motivo obligatorio si cancela el proveedor) | 4 | 4 | 4 | 2 | **3.60** | P2 Alta |
| 10 | `PATCH` | `/api/v1/reservations/{id}/reschedule` | Reprogramar reserva | 4 | 4 | 4 | 1 | **3.40** | P2 Alta |
| 11 | `GET` | `/api/v1/reservations/mine` | Mis reservas (filtros) | 4 | 3 | 4 | 2 | **3.35** | P2 Alta |
| 12 | `POST` | `/api/v1/auth/logout` | Cerrar sesión (revoca el JTI) | 3 | 3 | 5 | 1 | **3.00** | P2 Alta |
| 13 | `GET` | `/api/v1/users/lookup` | Buscar cliente por email | 3 | 3 | 5 | 1 | **3.00** | P2 Alta |
| 14 | `POST` | `/api/v1/schedules/{id}/blocks` | Bloquear franja (vacaciones, ausencias) | 3 | 4 | 2 | 2 | **2.85** | P3 Media |
| 15 | `PATCH` | `/api/v1/services/{id}/deactivate` | Desactivar servicio | 3 | 3 | 3 | 2 | **2.80** | P3 Media |
| 16 | `POST` | `/api/v1/resources` | Registrar recurso (sala, equipo, persona) | 3 | 3 | 2 | 3 | **2.80** | P3 Media |
| 17 | `PUT` | `/api/v1/services/{id}` | Editar servicio | 3 | 3 | 3 | 1 | **2.60** | P3 Media |
| 18 | `POST` | `/api/v1/services/{id}/resources` | Asociar recursos a un servicio | 3 | 3 | 2 | 2 | **2.60** | P3 Media |
| 19 | `GET` | `/api/v1/services/mine` | Mis servicios | 3 | 2 | 2 | 2 | **2.35** | P4 Baja |
| 20 | `GET` | `/api/v1/schedules/mine` | Mis horarios | 3 | 2 | 2 | 2 | **2.35** | P4 Baja |
| 21 | `GET` | `/actuator/health` | Salud del servicio | 2 | 4 | 1 | 1 | **2.10** | P4 Baja |
| 22 | `GET` | `/api/v1/resources/mine` | Mis recursos | 2 | 2 | 2 | 2 | **2.00** | P4 Baja |
| 23 | `GET` | `/swagger-ui.html, /v3/api-docs` | Documentación interactiva | 2 | 1 | 4 | 1 | **1.95** | P4 Baja |

## 4. Justificación por endpoint


### P1 Crítica

- **`POST /api/v1/auth/login`** (HU-03, Público) — Iniciar sesión y obtener JWT. Puerta de entrada: sin login ningún otro endpoint protegido funciona; blanco principal de fuerza bruta.
- **`POST /api/v1/reservations`** (HU-12, HU-13, CLIENT, PROVIDER) — Crear reserva (cliente o proveedor en nombre de un cliente). Es el flujo central del negocio; debe garantizar que no haya dobles reservas (bloqueo SELECT … FOR UPDATE, ADR-015).
- **`GET /api/v1/services/{serviceId}/availability`** (HU-11, CLIENT, PROVIDER) — Franjas libres de un servicio (máx. 31 días). Paso previo obligatorio a reservar y reprogramar; consulta costosa (calcula horarios − bloqueos − reservas).
- **`POST /api/v1/users`** (HU-01, Público) — Registrar cliente. Sin clientes no hay reservas; endpoint público expuesto a registros masivos y enumeración.
- **`POST /api/v1/providers`** (HU-02, Público) — Registrar proveedor. Sin proveedores no hay oferta; habilita todo el módulo de catálogo.
- **`POST /api/v1/services`** (HU-05, PROVIDER) — Crear servicio. Sin servicios no hay catálogo ni disponibilidad.
- **`POST /api/v1/schedules`** (HU-08, PROVIDER) — Definir horario de atención. La disponibilidad se calcula a partir de los horarios.

### P2 Alta

- **`GET /api/v1/catalog/services`** (Frontend F1, CLIENT, PROVIDER) — Catálogo de servicios activos. Primera pantalla del cliente; alto tráfico de lectura.
- **`PATCH /api/v1/reservations/{id}/cancel`** (HU-14, HU-15, CLIENT, PROVIDER) — Cancelar reserva (motivo obligatorio si cancela el proveedor). Libera franjas; riesgo de BOLA si no se valida el dueño (hoy sí se valida: requireOwner).
- **`PATCH /api/v1/reservations/{id}/reschedule`** (HU-16, CLIENT, PROVIDER) — Reprogramar reserva. Combina cancelar + reservar; misma concurrencia que crear.
- **`GET /api/v1/reservations/mine`** (Frontend F1/F2, CLIENT, PROVIDER) — Mis reservas (filtros). Expone datos personales de reservas; debe filtrar estrictamente por el usuario autenticado.
- **`POST /api/v1/auth/logout`** (HU-04, Autenticado) — Cerrar sesión (revoca el JTI). Control de seguridad: invalida el token antes de expirar.
- **`GET /api/v1/users/lookup`** (HU-13, PROVIDER) — Buscar cliente por email. Permite al proveedor reservar por un cliente; riesgo de enumeración de emails.

### P3 Media

- **`POST /api/v1/schedules/{id}/blocks`** (HU-09, PROVIDER) — Bloquear franja (vacaciones, ausencias). Afecta directamente la disponibilidad ofrecida.
- **`PATCH /api/v1/services/{id}/deactivate`** (HU-07, PROVIDER) — Desactivar servicio. Retira el servicio del catálogo; verifica dueño.
- **`POST /api/v1/resources`** (HU-10, PROVIDER) — Registrar recurso (sala, equipo, persona). Requisito para asociar recursos a servicios.
- **`PUT /api/v1/services/{id}`** (HU-06, PROVIDER) — Editar servicio. Mantenimiento del catálogo.
- **`POST /api/v1/services/{id}/resources`** (Sprint 2, PROVIDER) — Asociar recursos a un servicio. Valida que el recurso sea del mismo proveedor.

### P4 Baja

- **`GET /api/v1/services/mine`** (Frontend F2, PROVIDER) — Mis servicios. Lectura del panel del proveedor.
- **`GET /api/v1/schedules/mine`** (Frontend F2, PROVIDER) — Mis horarios. Lectura del panel del proveedor.
- **`GET /actuator/health`** (Operación, Público) — Salud del servicio. Necesario para monitoreo y healthchecks; no expone detalles.
- **`GET /api/v1/resources/mine`** (Frontend F2, PROVIDER) — Mis recursos. Lectura del panel del proveedor.
- **`GET /swagger-ui.html, /v3/api-docs`** (Operación, Público) — Documentación interactiva. Útil en desarrollo; en producción expone el inventario completo de la API.

## 5. Conclusiones
- **Ruta crítica del negocio**: `login → catálogo → disponibilidad → crear reserva`. Estos endpoints concentran la prioridad P1 y deben tener prioridad en pruebas de carga, monitoreo y controles de seguridad (limitación de tasa, alertas).
- **Endpoints públicos** (`/auth/login`, `/users`, `/providers`) son P1 tanto por valor como por exposición: son los primeros candidatos para rate limiting y CAPTCHA (ver V-03 y V-05 en el análisis OWASP).
- **Escrituras sobre reservas** (`cancel`, `reschedule`) son P2: tienen menos dependientes, pero manipulan recursos de terceros, por lo que necesitan pruebas de autorización por objeto (BOLA).
- **Lecturas del panel del proveedor** (`/mine`) son P3/P4: bajo riesgo porque filtran por el usuario del token.
- **Swagger/OpenAPI** queda en P4 para el negocio, pero debe deshabilitarse o protegerse en producción.
