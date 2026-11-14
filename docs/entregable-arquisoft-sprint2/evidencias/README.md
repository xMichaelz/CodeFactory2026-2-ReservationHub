# Evidencias de las correcciones OWASP (H3)

Cada archivo guarda la salida real de los comandos, tomada el 2026-11-14 en el Mac contra el stack Docker (`compose.yaml` del backend + `compose.yaml` del front, sin `compose.dev.yaml`), por `https://localhost:8443`. Los comandos van en cada archivo, precedidos por `$`.

| V | Estado | Evidencia |
|---|---|---|
| V-01 | Corregida | [V-01-secretos.txt](V-01-secretos.txt) |
| V-02 | Corregida | [V-02-tls.txt](V-02-tls.txt) |
| V-03 | Corregida | [V-03-rate-limit.txt](V-03-rate-limit.txt) |
| V-04 | Corregida | [V-04-login-bloqueo.txt](V-04-login-bloqueo.txt) |
| V-05 | Corregida con riesgo residual (409, ADR-016) | [V-05-registro-enumeracion.txt](V-05-registro-enumeracion.txt) |
| V-06 | Corregida | [V-06-swagger.txt](V-06-swagger.txt) |
| V-07 | Corregida | [V-07-puertos-bd.txt](V-07-puertos-bd.txt) |
| V-08 | Corregida | [V-08-cabeceras.txt](V-08-cabeceras.txt) |
| V-09 | Mitigada (CSP + sesión que caduca) | [V-09-csp-sesion.txt](V-09-csp-sesion.txt) |
| V-10 | Corregida | [V-10-no-root-digest.txt](V-10-no-root-digest.txt) |
| V-11 | Corregida (escaneo de imagen con Trivy solo en CI) | [V-11-dependencias.txt](V-11-dependencias.txt) |
| V-12 | Mitigada (logs JSON + alerta por umbral) | [V-12-auditoria-alerta.txt](V-12-auditoria-alerta.txt) |
| V-13 | Mitigada (3 altas/hora por IP + auditoría) | [V-13-alta-proveedores.txt](V-13-alta-proveedores.txt) |

Complementos: [e2e-https.txt](e2e-https.txt) (9 e2e de Playwright contra el contenedor por HTTPS).

Notas
- Las pruebas automáticas que mantienen estas garantías viven en `src/test/java/.../security/owasp/` (backend) y `src/seguridad/` (front).
- Quedan para CI: el escaneo de la imagen con Trivy y la ejecución del workflow `seguridad.yml` (no instalados/ejecutables en local).
- Para los e2e se apagó el límite de la API con un override (`RATE_LIMIT_ENABLED=false`) y se volvió a encender; Nginx mantuvo su `limit_req`.
- OSV-Scanner halló 18 vulnerabilidades conocidas en el backend (Tomcat 11.0.24, Jackson 2.21.5 y 3.1.5) que se corrigieron fijando versiones parcheadas en `pom.xml`.
