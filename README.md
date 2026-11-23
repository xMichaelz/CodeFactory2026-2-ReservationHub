# ReservaHub

[![CI](https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub/actions/workflows/sonarcloud.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=alert_status)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=coverage)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=bugs)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=security_rating)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=arge2004_reservationhubanalysis&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=arge2004_reservationhubanalysis)

Plataforma de reservas de servicios — **Caso 14 CodeF@ctory UdeA** (Sprint 3).

## 🚀 Inicio rápido (todo con Docker)

Con Docker Desktop corriendo, desde la raíz del repo:

```bash
docker compose up -d --build
```

Eso levanta PostgreSQL, la API (Spring Boot) y el frontend (React + nginx). La primera vez tarda unos minutos (compila Maven y Vite). Cuando termine:

| Qué | URL |
|-----|-----|
| 🖥️ **Frontend (aplicación web)** | http://localhost:3001 |
| 🧪 Swagger UI (vía frontend) | http://localhost:3001/swagger-ui.html |
| 🧪 Swagger UI (API directa) | http://localhost:8080/swagger-ui.html |
| 📄 OpenAPI JSON | http://localhost:8080/v3/api-docs |
| ❤️ Health de la API | http://localhost:8080/actuator/health |
| 🔌 API REST (base) | http://localhost:8080/api/v1 |
| 🐘 PostgreSQL | `localhost:5433` (usuario/clave/db: `reservahub`) |
| 📊 Grafana (perfil `observability`) | http://localhost:3000 (admin / admin) |
| 📈 Prometheus (perfil `observability`) | http://localhost:9090 |

Para detener: `docker compose down` (agrega `-v` para borrar también los datos de PostgreSQL).

**Primer uso:** en http://localhost:3001 crea una cuenta de **proveedor** (“Soy proveedor”), registra un servicio y sus horarios de atención; luego, en otra ventana (o en incógnito), crea una cuenta de **cliente** y reserva desde el catálogo.

Arquitectura: **monolito modular** (una sola aplicación Spring Boot), organizada por dominios de negocio con capas `controller / DTO / entity / mapper / repository / service`.

## Tecnologías

| Tecnología | Versión / uso |
|------------|---------------|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven | Wrapper (`mvnw`) |
| PostgreSQL | 16 (Docker) |
| Flyway | migraciones versionadas |
| Spring Security + JWT | autenticación/autorización |
| SpringDoc OpenAPI | Swagger UI |
| Actuator | `/actuator/health` |
| Testcontainers | pruebas de integración |
| React 18 + TypeScript + Vite | frontend (`frontend/`) |
| React Router | navegación por páginas y rutas protegidas por rol |
| nginx | sirve el frontend y hace de proxy hacia la API en Docker |
| Playwright | pruebas E2E rápidas del frontend |
| Serenity BDD + Screenplay + Cucumber | pruebas de aceptación (`acceptance-tests/`) |

Dependencias extra justificadas: **JJWT** (tokens), **springdoc-openapi** (documentación API exigida), **Testcontainers** (integración con PostgreSQL real).

## Requisitos

- JDK 25+
- Docker (recomendado para PostgreSQL y tests)
- Maven Wrapper incluido (`./mvnw`)

## Configuración

```bash
cp .env.example .env
```

Variables:

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | JDBC URL. Si no se define, se arma con `DB_HOST`, `DB_PORT` y `DB_NAME` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales DB |
| `JWT_SECRET` | Secreto HS256 (≥ 32 bytes) |
| `JWT_EXPIRATION_MS` | Expiración del token |
| `SERVER_PORT` | Puerto HTTP local (default 8080). En Render manda `PORT` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma |

## Ejecución local

### 1) PostgreSQL con Docker

```bash
docker compose up -d db
```

PostgreSQL queda en `localhost:5433` (usuario/clave/db: `reservahub`).

Exportar variables (o usar un `.env` cargado por tu shell):

```bash
export DB_URL=jdbc:postgresql://localhost:5433/reservahub
export DB_USERNAME=reservahub
export DB_PASSWORD=reservahub
export JWT_SECRET='change-me-to-a-long-random-secret-at-least-256-bits-long!!'
```

### 2) Arrancar la API

```bash
./mvnw spring-boot:run
```

Flyway aplicará `V1__initial_schema.sql` al iniciar.

### Todo con Docker Compose

```bash
docker compose up --build
```

Servicios: `db` (PostgreSQL), `app` (API en `:8080`) y `frontend` (nginx en `:3001`). Ver la tabla de URLs en [Inicio rápido](#-inicio-rápido-todo-con-docker).

Prometheus y Grafana van en un perfil aparte. Hace falta abrir las métricas solo en esa máquina:

```bash
PROMETHEUS_PUBLIC=true docker compose --profile observability up --build
```

Grafana: `http://localhost:3000` (admin / admin). Prometheus: `http://localhost:9090`. En Render `PROMETHEUS_PUBLIC` queda en false y `/actuator/prometheus` pide JWT.

## Despliegue en Render

`render.yaml` crea la API (Docker, Java 25) y PostgreSQL 16 en la región `oregon`. Flyway aplica las migraciones al arrancar, igual que en local. Render genera `JWT_SECRET` y conecta la base por la red privada.

1. El blueprint tiene que estar en `main`.
2. Abre [este enlace de Blueprint](https://render.com/deploy?repo=https://github.com/xMichaelz/CodeFactory2026-2-ReservationHub) e inicia sesión en Render.
3. Confirma el plan gratuito. Render crea `reservahub-api` y `reservahub-db`.

Cuando termine el deploy:

- Health: `https://<servicio>.onrender.com/actuator/health`
- Swagger: `https://<servicio>.onrender.com/swagger-ui.html`
- OpenAPI: `https://<servicio>.onrender.com/v3/api-docs`

Si el frontend no corre en `http://localhost:5173`, cambia `CORS_ALLOWED_ORIGINS` en el dashboard (orígenes separados por coma) y vuelve a desplegar.

En el plan gratuito la API se suspende tras un rato sin tráfico: el primer request después de eso tarda más. La base gratuita de Render caduca a los 30 días.

## Frontend (React)

Código en `frontend/`. SPA en React 18 + TypeScript + Vite, sin librerías de UI: componentes propios y CSS con tokens (modo claro/oscuro automático).

### Qué se puede hacer desde la UI

Todo lo que expone la API está disponible en el frontend:

| Rol | Página | Endpoints / HU |
|-----|--------|----------------|
| Público | Registro de cliente `/register` | `POST /users` (HU-01) |
| Público | Registro de proveedor `/register/provider` | `POST /providers` (HU-02) |
| Público | Login `/login` · botón “Cerrar sesión” | `POST /auth/login`, `POST /auth/logout` (HU-03, HU-04) |
| Cliente | Inicio `/client` | `GET /reservations/mine` |
| Cliente | Catálogo `/client/catalog` | `GET /catalog/services` |
| Cliente | Reservar `/client/book/:id` | `GET /services/{id}/availability`, `POST /reservations` (HU-11, HU-12) |
| Cliente | Mis reservas `/client/reservations` | `GET /reservations/mine`, `PATCH /reservations/{id}/cancel`, `PATCH /reservations/{id}/reschedule` (HU-14, HU-16) |
| Cliente | Historial `/client/history` | `GET /reports/my-reservations` (HU-17) |
| Proveedor | Panel `/provider` | `GET /services/mine`, `/resources/mine`, `/schedules/mine`, `/reservations/mine` |
| Proveedor | Servicios `/provider/services` | `POST /services`, `PUT /services/{id}`, `PATCH /services/{id}/deactivate`, `POST /services/{id}/resources` (HU-05, HU-06, HU-07) |
| Proveedor | Recursos `/provider/resources` | `GET /resources/mine`, `POST /resources` (HU-10) |
| Proveedor | Horarios `/provider/schedules` | `POST /schedules`, `POST /schedules/{id}/blocks` (HU-08, HU-09) |
| Proveedor | Agenda `/provider/agenda` | `GET /reservations/mine`, cancelar con motivo y reprogramar (HU-15, HU-16) |
| Proveedor | Reservar a cliente `/provider/book` | `GET /users/lookup`, `POST /reservations` con `clientId` (HU-13) |
| Proveedor | Historial `/provider/history` | `GET /reports/business-reservations` (HU-18) |
| Proveedor | Reportes `/provider/reports` | `GET /reports/occupancy`, `/reports/demand`, `/reports/cancellations` (HU-19, HU-20, HU-21) |

### Estructura

```text
frontend/
├── src/
│   ├── api/            # cliente HTTP (JWT, errores) + un módulo por dominio + tipos de los DTO
│   ├── context/        # AuthContext (sesión JWT) y ToastContext (notificaciones)
│   ├── hooks/          # useAsync / useSubmit
│   ├── components/
│   │   ├── layout/     # AppLayout, PublicLayout, ProtectedRoute (guardas por rol)
│   │   ├── ui/         # Button, Modal, Card, campos, badges, filtros de fechas, gráficos…
│   │   ├── reservations/ services/ reports/   # componentes de dominio
│   ├── pages/          # auth/, client/, provider/, shared/
│   ├── utils/          # fechas (zona America/Bogota), formatos, etiquetas
│   └── styles/global.css
├── e2e/                # pruebas Playwright de punta a punta
├── Dockerfile          # build con Node + runtime nginx
└── nginx.conf          # SPA + proxy de /api, /swagger-ui, /v3/api-docs y /actuator a la API
```

En Docker, el navegador solo habla con `http://localhost:3001`; nginx reenvía `/api/**` a la API, así que no hace falta CORS. Con `VITE_API_URL` (build arg) se puede apuntar a otra API, por ejemplo la de Render.

### Desarrollo local del frontend

Con la API corriendo en `:8080` (por ejemplo `docker compose up -d db app`):

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (Vite redirige /api a :8080)
npm run build      # typecheck + build de producción
```

Si la API está en otro host: `VITE_API_PROXY_TARGET=http://host:puerto npm run dev`.

### Pruebas E2E

Recorren por la UI todas las historias (registro, login/logout, servicios, recursos, horarios, bloqueos, reserva, reprogramación, cancelaciones, reserva a nombre de un cliente, historiales y reportes). Usan el Chrome instalado en la máquina:

```bash
docker compose up -d --build
cd frontend
npm install
npm run test:e2e                                     # contra http://localhost:3001
E2E_BASE_URL=http://localhost:5173 npm run test:e2e  # contra npm run dev
```

## Pruebas de aceptación (Serenity BDD + Screenplay)

En `acceptance-tests/` hay una suite de pruebas de aceptación con **Serenity BDD**, el patrón **Screenplay** y **Cucumber** en español. Las features están organizadas por área: `acceso/`, `cliente/`, `proveedor/` y `e2e/`. Las precondiciones se preparan por API y lo que se prueba ocurre en el navegador. Detalles en [`acceptance-tests/README.md`](acceptance-tests/README.md).

```bash
docker compose up -d --build        # la app en http://localhost:3001
cd acceptance-tests
./gradlew clean test                # todas (gradlew.bat en Windows)
./gradlew clean test -Dcucumber.filter.tags="@cliente" -Dheadless.mode=true
```

Reporte HTML de Serenity: `acceptance-tests/target/site/serenity/index.html`.

## Tests

```bash
./mvnw clean test
./mvnw clean package
```

## Documentación interactiva

- Frontend: http://localhost:3001
- Swagger UI: http://localhost:8080/swagger-ui.html (también en http://localhost:3001/swagger-ui.html)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Endpoints principales (HU-01 .. HU-16)

| Método | Ruta | HU |
|--------|------|----|
| POST | `/api/v1/users` | HU-01 |
| POST | `/api/v1/providers` | HU-02 |
| POST | `/api/v1/auth/login` | HU-03 |
| POST | `/api/v1/auth/logout` | HU-04 |
| POST | `/api/v1/services` | HU-05 |
| PUT | `/api/v1/services/{id}` | HU-06 |
| PATCH | `/api/v1/services/{id}/deactivate` | HU-07 |
| POST | `/api/v1/schedules` | HU-08 |
| POST | `/api/v1/schedules/{id}/blocks` | HU-09 |
| POST | `/api/v1/resources` | HU-10 |
| GET | `/api/v1/services/{id}/availability` | HU-11 |
| POST | `/api/v1/reservations` | HU-12, HU-13 |
| PATCH | `/api/v1/reservations/{id}/cancel` | HU-14, HU-15 |
| PATCH | `/api/v1/reservations/{id}/reschedule` | HU-16 |
| GET | `/api/v1/reports/my-reservations` | HU-17 |
| GET | `/api/v1/reports/business-reservations` | HU-18 |
| GET | `/api/v1/reports/occupancy` | HU-19 |
| GET | `/api/v1/reports/demand` | HU-20 |
| GET | `/api/v1/reports/cancellations` | HU-21 |

HU-12 es la reserva del cliente para sí mismo. HU-13 es el mismo `POST`: el proveedor envía el `clientId`. En la cancelación, el proveedor debe indicar motivo (HU-15).

Los reportes exigen `from` y `to` (máximo 31 días) y un JWT. HU-17 es solo del cliente. HU-18 a HU-21 son solo del proveedor. El ciclo de vida del historial (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`) se calcula al leer; la reserva guardada sigue en `CONFIRMED`, `CANCELLED_BY_CLIENT` o `CANCELLED_BY_PROVIDER`. Contrato: `docs/api/reportes.md`.

Priorización, roles y política de `/api/v1`: `docs/api/apis-priorizadas.md`.

Colecciones Postman: `postman/ReservaHub-Sprint1.postman_collection.json` y `postman/ReservaHub-Sprint2.postman_collection.json`.

## Estructura del proyecto

```text
reservahub/
├── src/main/java/co/udea/codefactory/reservahub/
│   ├── user/ provider/ servicecatalog/ schedule/ resource/
│   ├── reservation/                  # Sprint 2
│   ├── report/                       # Sprint 3: HU-17 .. HU-21
│   ├── security/ audit/ observability/ shared/
├── frontend/                         # SPA React (ver sección Frontend)
├── acceptance-tests/                 # Serenity BDD + Screenplay + Cucumber
├── src/main/resources/db/migration/
├── docs/adr/ docs/architecture/ docs/api/
├── postman/
├── Dockerfile
├── compose.yaml
└── pom.xml
```

## Documentación adicional

- Arquitectura: `docs/architecture/overview.md`
- Diagramas: `docs/architecture/diagrams.md`
- Supuestos: `docs/architecture/assumptions.md`
- ADRs: `docs/adr/`
