# Diagramas de arquitectura (C4 ligero / UML textual)

Los diagramas reflejan **solo** componentes implementados en Sprint 1.

## Diagrama de contexto

```mermaid
C4Context
title ReservaHub — Contexto
Person(client, "Cliente", "Usuario final")
Person(provider, "Proveedor", "Dueño del negocio")
System(reservahub, "ReservaHub", "API REST monolito modular")
SystemDb(pg, "PostgreSQL", "Datos de usuarios, proveedores, servicios, agendas, recursos")
Rel(client, reservahub, "Registro / login")
Rel(provider, reservahub, "Registro, servicios, horarios, recursos")
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
├── reservation   (placeholder)
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

```mermaid
flowchart TB
  subgraph host [Máquina local / Docker Compose]
    APP[reservahub-app :8080]
    DB[reservahub-db :5432 interno / :5433 host]
  end
  DEV[Desarrollador] --> APP
  APP --> DB
```
