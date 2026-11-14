# OWASP Top 10:2025 — ReservaHub

Análisis del monolito Spring Boot contra [OWASP Top 10:2025](https://owasp.org/www-project-top-ten/).
Cada control cita una clase o un test que existe en este repositorio. Lo que no está en el código queda como pendiente.

Alcance: API `/api/v1` desplegada en Render (`reservahub-api`) con PostgreSQL 16. No hay perfil `prod` ni `application-prod.properties`: la misma `SecurityConfig` corre en local y en Render.

Responsable de las acciones: equipo ReservaHub.

## A01:2025 Broken Access Control

La aplicación no restringe lo que un usuario autenticado puede hacer sobre datos de otro. En 2025 esta categoría también incluye CSRF y SSRF.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Un cliente podría cancelar la reserva de otro, o un proveedor reservar un servicio que no es suyo, si el control de dueño fallara. |
| Controles | `SecurityConfig.securityFilterChain` exige rol por ruta. `EndpointSecurityMatrixTest` falla si aparece un endpoint de `/api/v1` sin fila. `ReservationAccessPolicy.requireOwner` y `clientFor` rechazan reservas ajenas. `CatalogServiceService` lanza `ForbiddenException` ("You are not allowed to modify this service"). No hay cliente HTTP (`RestTemplate`, `WebClient` ni `RestClient`) en `src/main`, así que no hay superficie SSRF. CSRF está apagado a propósito: la sesión es `STATELESS` y el token va en `Authorization`, no en cookie (`SecurityConfig`, comentario S4502). |
| Brechas | El rol se lee del claim del JWT en `JwtAuthenticationFilter` y no se vuelve a cargar de la base. Un cambio de rol o una baja de la cuenta siguen valiendo hasta que el token expire o se revoque. |
| Severidad | Media |
| Acción | Al autenticar, comprobar que el usuario sigue `ACTIVE` y que el rol de la base coincide con el claim. |
| Sprint | 3 |

## A02:2025 Security Misconfiguration

Opciones inseguras por defecto, consolas abiertas o cabeceras y endpoints de diagnóstico de más.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | En producción cualquiera puede leer el contrato completo y probar la API. Un Actuator amplio filtraría estado de la base o del entorno. |
| Controles | Actuator solo publica `health` y oculta el detalle: `management.endpoints.web.exposure.include=health` y `management.endpoint.health.show-details=never` en `application.properties`. `SecurityConfig` deja público únicamente `/actuator/health`. `spring.jpa.hibernate.ddl-auto=validate`: el esquema lo posee Flyway. CORS usa una lista explícita (`CorsConfigurationSource`); `CorsTest` comprueba el origen de Vite y no un comodín. |
| Brechas | Swagger y OpenAPI están en `permitAll` (`/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`) sin perfil que los cierre en Render. Quedan en `https://reservahub-api.onrender.com/swagger-ui.html`. El secreto JWT de desarrollo está escrito en `application.properties` (`change-me-to-a-long-random-secret-...`) y en `compose.yaml`. En Render el secreto lo genera el blueprint; si alguien arranca el jar sin `JWT_SECRET`, usa el valor del repositorio. |
| Severidad | Alta para Swagger abierto. Media para el secreto por defecto. Baja para Actuator: el health público no muestra componentes. |
| Acción | Servir Swagger solo fuera de producción, o exigirle JWT. Fallar el arranque si `JWT_SECRET` sigue siendo el valor de ejemplo. |
| Sprint | 3. Actuator, en su forma actual, se mantiene: Render lo usa como health check. |

## A03:2025 Software Supply Chain Failures

Dependencias, el build y la imagen pueden introducir código que el equipo no escribió.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Una versión vulnerable de Spring, JJWT o de la imagen Temurin entraría al servicio de Render en el siguiente build. |
| Controles | `pom.xml` fija el parent Spring Boot `4.1.1`, JJWT `0.12.6` y JaCoCo `0.8.15`. `.mvn/wrapper/maven-wrapper.properties` fija Maven `3.9.16`. El workflow `.github/workflows/sonarcloud.yml` analiza el código con SonarCloud en cada push a `main`. |
| Brechas | No hay `dependabot.yml` ni un escáner de CVEs (dependency-check u OWASP Dependency-Track). El `Dockerfile` usa `eclipse-temurin:25-jdk` y `eclipse-temurin:25-jre` por tag, sin digest. El workflow actual no corre en pull requests. |
| Severidad | Media |
| Acción | Dependabot semanal para Maven y GitHub Actions, y correr `./mvnw verify` más Sonar también en pull requests. Fijar el digest de Temurin cuando se actualice la imagen. |
| Sprint | 2 para Dependabot y el disparador de pull request (entregable de CI). El digest queda para el sprint 3. |

## A04:2025 Cryptographic Failures

Datos sensibles protegidos con criptografía débil, ausente o mal usada.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Una contraseña en claro en la base, o un JWT firmado con una clave corta, permite suplantar usuarios. |
| Controles | `SecurityConfig.passwordEncoder` es `BCryptPasswordEncoder`. `UserService` guarda `passwordHash`, no la clave. `Sprint1AcceptanceTest` espera que el JSON de registro no tenga `password`. `ProviderReadTest` comprueba que el lookup no devuelve `passwordHash`. `JwtService` rechaza un secreto de menos de 32 bytes y firma con HMAC-SHA256 (`Keys.hmacShaKeyFor`). |
| Brechas | No hay cifrado de columnas en la aplicación. No hay rotación del secreto JWT: cambiarlo invalida todas las sesiones, y no está automatizado. El valor por defecto del secreto (A02) debilita este control si se despliega sin variable de entorno. |
| Severidad | Media |
| Acción | Quitar el secreto por defecto. Documentar la rotación de `JWT_SECRET` en Render. |
| Sprint | 3 |

## A05:2025 Injection

Datos no confiables que el intérprete ejecuta: SQL, JPQL o comandos.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Un filtro de búsqueda armado con el email o el id del usuario podría alterar la consulta. |
| Controles | El acceso a datos es Spring Data JPA. Los `@Query` existentes son JPQL con parámetros (`ProviderRepository.findByIdForUpdate`, repositorios de reservas, catálogo y `RevokedTokenRepository`). No hay `@Query` nativo ni `createNativeQuery`. Los DTO validan con Bean Validation (`CreateUserRequestDTO`: email, tamaño y patrón de la clave). `GlobalExceptionHandler` responde 400 ante cuerpo mal formado, parámetro ausente o tipo inválido. `InvalidParamsTest` cubre esos 400. |
| Brechas | No hay una prueba que intente una cadena SQL en el lookup por email. El riesgo residual es bajo porque el parámetro viaja como argumento de Spring Data, no concatenado. |
| Severidad | Baja |
| Acción | Mantener la regla de no introducir SQL nativo sin parámetros. Si aparece un `@Query` nativo, añadir un test de inyección. |
| Sprint | 3, solo si se agrega SQL nativo. |

## A06:2025 Insecure Design

Controles que faltan en el diseño, no solo en una línea de código.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Dos clientes podrían quedar con el mismo cupo, o un atacante podría probar claves sin límite de diseño. |
| Controles | ADR-015 y `ProviderRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`) serializan las reservas de un proveedor. `Reservation` tiene `@Version`. `ConcurrentReservationTest` y `OptimisticLockHandlerTest` cubren el doble intento y el 409. El bloqueo de login está diseñado (5 intentos, 15 minutos) y no es solo un chequeo suelto: `LockoutProperties` y `AuthService.registerFailure`. |
| Brechas | No hay modelo de amenazas en `docs/`. No hay límite de peticiones por IP. No hay segundo factor en el diseño de la autenticación (ver A07). `AuditLogger` declara que la auditoría persistente queda para después. |
| Severidad | Media |
| Acción | Añadir un ADR de autenticación que deje MFA y el rate limit como decisión explícita, aceptada o pendiente. |
| Sprint | 3 |

## A07:2025 Authentication Failures

Identidad, sesión y recuperación de acceso.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Robo de contraseña, reutilización de un JWT ya cerrado, o adivinar si un email existe. |
| Controles | Login en `AuthService`: BCrypt, cuenta inactiva y email inexistente responden el mismo `Invalid credentials`. A los 5 fallos, `AccountLockedException` responde 423 con `Retry-After` (`GlobalExceptionHandler.handleAccountLocked`). `AuthServiceTest` cubre fallo, bloqueo y éxito. El logout guarda el `jti` en `TokenRevocationService` (tabla `RevokedToken`) y `JwtAuthenticationFilter` rechaza los tokens revocados. La sesión HTTP no se crea (`STATELESS`). La clave de registro exige 8 a 72 caracteres, una letra y un dígito (`CreateUserRequestDTO`). |
| Brechas | No hay MFA. No existe clase, endpoint ni dependencia TOTP, WebAuthn u OTP en el repositorio. No hay límite por IP, solo por cuenta. No hay refresh token: el access token dura `JWT_EXPIRATION_MS` (1 hora por defecto). |
| Severidad | Alta por la ausencia de MFA. El resto del login está controlado. |
| Acción | Diseñar MFA para el proveedor (dueño de la agenda) en un sprint posterior. Mientras tanto, no afirmar que la autenticación tiene segundo factor. |
| Sprint | Pendiente. No está en el alcance implementado del Sprint 2. |

## A08:2025 Software or Data Integrity Failures

Confiar en código o datos sin comprobar que no cambiaron.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Una imagen Docker sustituida o una migración editada a mano dejarían el servicio y la base en estados distintos. |
| Controles | Flyway aplica `V1`–`V5` y Hibernate solo valida el esquema. El JWT se verifica con `JwtService.parseClaims` antes de crear la autenticación. `@Version` en `Reservation` impide pisar una fila que otro request ya cambió (`OptimisticLockHandlerTest`). |
| Brechas | La imagen de Render no está firmada (no hay cosign ni digest en el `Dockerfile`). No hay verificación de integridad del jar más allá del build de Maven. |
| Severidad | Media |
| Acción | Publicar la imagen con digest y, si el curso lo exige después, firmarla. |
| Sprint | 3 |

## A09:2025 Security Logging and Alerting Failures

Sin registro útil no se detecta un abuso. En 2025 el énfasis es que el log genere una alerta, no solo una línea.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Un ataque de claves o un acceso denegado repetido solo queda en la salida estándar del contenedor. Nadie recibe un aviso. |
| Controles | `AuditLogger` registra `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGIN_LOCKED`, `LOGOUT_SUCCESS` y las altas de usuario, proveedor, servicio, horario, recurso y reserva. No escribe la contraseña: el detalle es `userId` o `jti`. `TraceIdFilter` pone `X-Trace-Id` y `GlobalExceptionHandler` lo devuelve en el JSON. Los 500 se registran con el stack en el servidor y al cliente solo le llega "Unexpected server error". |
| Brechas | `AuditLogger` es un `log.info` de SLF4J. El comentario de la clase dice que la pista persistente se agregará después. No hay alerta, métrica ni destino distinto de la consola de Render. |
| Severidad | Media |
| Acción | Persistir los eventos de login fallido y bloqueo, y definir una alerta mínima (por ejemplo, un umbral de `LOGIN_LOCKED` en los logs de Render). |
| Sprint | 3 |

## A10:2025 Mishandling of Exceptional Conditions

Fallar abierto, dejar a medias una transacción o filtrar el error interno al cliente.

| Campo | Detalle |
|-------|---------|
| Riesgo en ReservaHub | Un choque de dos reservas podría responder 500 y dejar el cupo en un estado ambiguo, o el stack trace revelaría la base. |
| Controles | `GlobalExceptionHandler` traduce negocio, validación, credenciales, acceso denegado y cuerpo ilegible a 400, 401, 403, 404, 409 o 423. El genérico responde 500 sin la excepción. `ObjectOptimisticLockingFailureException` responde 409, no 500 (`OptimisticLockHandlerTest`). Un JWT inválido en `JwtAuthenticationFilter` limpia el contexto y la autorización posterior rechaza la ruta protegida: no abre la sesión. |
| Brechas | `SecurityConfig.writeError` traga la excepción si no puede escribir el JSON y solo deja el status. No hay prueba de un fallo a mitad de la transacción de reserva más allá del bloqueo optimista y del `SELECT … FOR UPDATE`. |
| Severidad | Baja |
| Acción | Mantener el manejador global como único formato de error. No devolver `ex.getMessage()` de excepciones no controladas. |
| Sprint | Se sostiene en el Sprint 2. Sin acción nueva salvo que aparezca un 500 con cuerpo interno. |

## Resumen de pendientes

| Pendiente | Categoría | Severidad |
|-----------|-----------|-----------|
| Swagger y `/v3/api-docs` públicos en Render | A02 | Alta |
| No hay MFA | A07 | Alta |
| Secreto JWT de ejemplo en el repositorio | A02, A04 | Media |
| Rol tomado solo del JWT, sin releer la cuenta | A01 | Media |
| Sin Dependabot ni digest de la imagen | A03, A08 | Media |
| Auditoría solo en logs, sin alerta | A09 | Media |
| Actuator limitado a health sin detalles | A02 | Control aceptado, no es brecha |
