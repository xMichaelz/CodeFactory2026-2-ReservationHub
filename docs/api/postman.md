# Guía Postman — ReservaHub Sprint 1

## Importar

1. Abrir Postman
2. Import → `postman/ReservaHub-Sprint1.postman_collection.json`
3. Variable `baseUrl` = `http://localhost:8080`

## Orden sugerido

1. **USERS → Register client**
2. **PROVIDERS → Register provider** (usa el mismo `suffix` de colección)
3. **AUTH → Login provider** (guarda `accessToken`)
4. **SERVICES / SCHEDULE / RESOURCES** (requieren Bearer)
5. **AUTH → Logout** (revoca el token)

## Casos negativos incluidos

- email inválido
- cliente duplicado
- credenciales incorrectas
- servicio sin token
- horario inválido / solapado
- recurso duplicado

Detalle de contrato de errores en `docs/api/errors.md`.
