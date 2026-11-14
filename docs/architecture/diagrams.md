# Diagramas de arquitectura (C4 ligero / UML textual)

Los diagramas reflejan los componentes implementados hasta el Sprint 2, reservas incluidas. El módulo `report` sigue como placeholder.

## Diagrama de contexto

```mermaid
C4Context
title ReservaHub — Contexto
Person(client, "Cliente", "Usuario final")
Person(provider, "Proveedor", "Dueño del negocio")
System(reservahub, "ReservaHub", "API REST monolito modular")
SystemDb(pg, "PostgreSQL", "Usuarios, proveedores, servicios, agendas, recursos y reservas")
Rel(client, reservahub, "Registro, login y reservas")
Rel(provider, reservahub, "Oferta, agenda y reservas a nombre de un cliente")
Rel(reservahub, pg, "JDBC / JPA")
```

## Diagrama de contenedores

```mermaid
flowchart LR
  UI[Postman / Swagger UI] --> APP[ReservaHub JAR<br/>Spring Boot 4.1.1]
  APP --> DB[(PostgreSQL 16)]
```

## Diagrama de paquetes

```text
co.udea.codefactory.reservahub
├── user
├── provider
├── servicecatalog
├── schedule
├── resource
├── reservation   (activo desde Sprint 2)
├── report        (placeholder)
├── security
├── audit
├── observability
└── shared
```

## Diagrama de componentes (request autenticada de proveedor)

```mermaid
sequenceDiagram
  participant C as Client
  participant F as JwtFilter
  participant Ctrl as Controller
  participant Svc as Service
  participant Repo as Repository
  participant DB as PostgreSQL
  C->>F: Authorization Bearer JWT
  F->>Ctrl: AuthenticatedUser
  Ctrl->>Svc: DTO validado
  Svc->>Repo: Entity
  Repo->>DB: SQL
  DB-->>Repo: row
  Repo-->>Svc: Entity
  Svc-->>Ctrl: Response DTO
  Ctrl-->>C: JSON 2xx
```

## Diagrama de despliegue

Producción del Sprint 2. El servicio ya está en Render. GitHub Actions (`.github/workflows/sonarcloud.yml`) compila, corre las pruebas con JaCoCo y publica el análisis en SonarCloud. El diagrama incluye también los pull requests hacia `develop` y `main`: ese disparador entra con el cambio de CI.

```mermaid
flowchart TB
  DEV[Desarrollador] --> GH[GitHub<br/>CodeFactory2026-2-ReservationHub]
  GH -->|push a main y pull request| GHA[GitHub Actions<br/>verify con JaCoCo]
  GHA --> SONAR[SonarCloud<br/>arge2004_reservationhubanalysis]
  GH -->|deploy de main| API[Render Web Service<br/>reservahub-api<br/>Docker, Java 25]
  API --> PG[(Render PostgreSQL 16<br/>reservahub-db)]
  USER[Cliente o proveedor] -->|HTTPS| API
```

En local, `docker compose` sigue levantando la API y PostgreSQL 16 en la máquina del desarrollador. Ese camino no reemplaza el diagrama de arriba.
