# Entregable Arquitectura de Software — Sprint 2 (ReservaHub)

Insumos para el informe. El análisis se hizo sobre el código real de los repos backend (`feature/frontend-inicial`) y frontend; las correcciones OWASP (hito H3) están en las ramas `fix/owasp` de ambos repos, y el diagrama refleja ya ese despliegue corregido.

| Archivo | Punto del entregable | Uso |
|---|---|---|
| `diagrama-despliegue.drawio` | Diagrama de despliegue | Editable: abrir en https://app.diagrams.net (Archivo → Abrir) o en la extensión draw.io de VS Code. |
| `diagrama-despliegue.png` / `.pdf` | Diagrama de despliegue | Para pegar directamente en el informe. |
| `priorizacion-apis.md` | 3. Priorizar las APIs | Método (criterios y pesos), tabla de 23 endpoints puntuados, justificación y conclusiones. |
| `priorizacion-apis.csv` | 3. Priorizar las APIs | Misma tabla para Excel / Google Sheets. |
| `vulnerabilidades-owasp.md` | 4. Vulnerabilidades con OWASP | 13 hallazgos mapeados a OWASP Top 10:2025 y API Security Top 10:2023, evidencia (archivo:línea), escenario, recomendación, **estado** y **corrección aplicada** de cada uno, fortalezas y plan de remediación. |
| `vulnerabilidades-owasp.csv` | 4. Vulnerabilidades con OWASP | Tabla resumen para Excel, con estado, corrección, pruebas y evidencia. |
| `correcciones-owasp.md` | 4. Vulnerabilidades con OWASP | Informe final de correcciones: antes/después por V-xx, riesgos residuales (V-05, V-09), pruebas que lo garantizan y cambios operativos. |
| `evidencias/` | 4. Vulnerabilidades con OWASP | Salida real de comandos contra el stack Docker, un archivo por V-xx (índice en `evidencias/README.md`). |
| `fuentes-diagrama/` | Diagrama de despliegue | Script que genera el `.drawio` e íconos; instrucciones de exportación. |

## Resumen del despliegue (para el texto del informe)
- **Dispositivo del usuario**: navegador que ejecuta la SPA React 19 (Vite); guarda el JWT en `sessionStorage` y la sesión caduca sola a los 30 min.
- **Servidor Docker host** con Docker Compose (proyectos `reservahub` y `reservahub-front`) y tres contenedores en la red `reservahub-backend`, todos sin root y con imágenes fijadas por digest:
  - `reservahub-web` — Nginx 1.27 sin privilegios: sirve el build estático y hace de reverse proxy de `/api/` hacia `app:8080`. Único punto publicado: **8443 → 8443 (HTTPS, TLS 1.2/1.3, HSTS, CSP)** y **8081 → 8080 (redirige 301 a https)**; `limit_req` frente a ráfagas.
  - `reservahub-app` — Spring Boot 4.1 sobre Java 25 (Temurin JRE, `USER app`): monolito modular (security con rate limiting, user, provider, servicecatalog, schedule, resource, reservation, audit, observability); REST `/api/v1/**` y Actuator. **Sin puerto en el host** (solo `expose 8080`); Swagger apagado salvo `SWAGGER_ENABLED=true`.
  - `reservahub-db` — PostgreSQL 16 con migraciones Flyway V1–V5 (dueño `reservahub`) y volumen `reservahub_pgdata`; **sin puerto en el host**; la API entra con `reservahub_app` (solo DML).
- **QA / desarrollo**: `compose.dev.yaml` publica la API (127.0.0.1:8080, con Swagger) y la BD (127.0.0.1:5433) solo en local para Postman.
- **CI**: GitHub → GitHub Actions (`mvn verify`, JDK 25; `seguridad.yml` con OSV-Scanner + Trivy) → SonarQube Cloud; Dependabot en ambos repos.

Íconos: logos oficiales de [Simple Icons](https://simpleicons.org) (CC0) y la librería AWS de draw.io para personas.
