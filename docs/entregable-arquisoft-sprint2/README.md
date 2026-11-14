# Entregable Arquitectura de Software — Sprint 2 (ReservaHub)

Insumos para el informe. Todo se obtuvo del código real de los repos backend (`feature/frontend-inicial`) y frontend.

| Archivo | Punto del entregable | Uso |
|---|---|---|
| `diagrama-despliegue.drawio` | Diagrama de despliegue | Editable: abrir en https://app.diagrams.net (Archivo → Abrir) o en la extensión draw.io de VS Code. |
| `diagrama-despliegue.png` / `.pdf` | Diagrama de despliegue | Para pegar directamente en el informe. |
| `priorizacion-apis.md` | 3. Priorizar las APIs | Método (criterios y pesos), tabla de 23 endpoints puntuados, justificación y conclusiones. |
| `priorizacion-apis.csv` | 3. Priorizar las APIs | Misma tabla para Excel / Google Sheets. |
| `vulnerabilidades-owasp.md` | 4. Vulnerabilidades con OWASP | 13 hallazgos mapeados a OWASP Top 10:2025 y API Security Top 10:2023, evidencia (archivo:línea), escenario, recomendación, fortalezas y plan de remediación. |
| `vulnerabilidades-owasp.csv` | 4. Vulnerabilidades con OWASP | Tabla resumen para Excel. |

## Resumen del despliegue (para el texto del informe)
- **Dispositivo del usuario**: navegador que ejecuta la SPA React 19 (Vite); guarda el JWT en `sessionStorage`.
- **Servidor Docker host** con Docker Compose y tres contenedores en la red `reservahub_default`:
  - `reservahub-web` — Nginx 1.27: sirve el build estático y hace de reverse proxy de `/api/` hacia la API (puerto 8081 → 80).
  - `reservahub-app` — Spring Boot 4.1 sobre Java 25 (Temurin JRE): monolito modular (security, user, provider, servicecatalog, schedule, resource, reservation, audit, observability); expone REST `/api/v1/**`, Swagger y Actuator (8080).
  - `reservahub-db` — PostgreSQL 16 con migraciones Flyway V1–V5 y volumen persistente `reservahub_pgdata` (5433 → 5432).
- **QA**: Postman / Swagger UI contra la API directa (8080, CORS).
- **CI**: GitHub → GitHub Actions (`mvn verify`, JDK 25) → SonarQube Cloud.

Íconos: logos oficiales de [Simple Icons](https://simpleicons.org) (CC0) y la librería AWS de draw.io para personas.
