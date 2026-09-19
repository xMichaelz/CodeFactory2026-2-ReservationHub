# Supuestos de dominio — Sprint 1

Decisiones mínimas tomadas ante ambigüedad del enunciado:

1. **User vs Provider:** un proveedor es un `User` con rol `PROVIDER` más un perfil `Provider` 1:1 (`providers.user_id` único).
2. **Un proveedor puede tener múltiples servicios, horarios y recursos.**
3. **Nombre de servicio único por proveedor** (no global).
4. **Nombre de recurso único por proveedor.**
5. **Nombre comercial de proveedor único** a nivel sistema.
6. **Desactivación de servicio es lógica** (`ACTIVE`/`INACTIVE`); no hay borrado físico.
7. **Horarios semanales** (`dayOfWeek` + `startTime`/`endTime`); los bloqueos son para una **fecha concreta** ligada a un horario, y deben caer dentro de la ventana y el mismo día de la semana.
8. **Solapamientos** se previenen entre ventanas del mismo proveedor y mismo día, y entre bloqueos del mismo horario/fecha.
9. **JWT + revocación por JTI** para logout coherente en modelo stateless.
10. **ADMIN** existe en el modelo de roles pero sin endpoints administrativos en Sprint 1.
11. **Reserva/report** no implementan casos de uso aún; solo se reservan paquetes.
12. Contraseña mínima: 8 caracteres, al menos una letra y un dígito (BCrypt, max 72).
