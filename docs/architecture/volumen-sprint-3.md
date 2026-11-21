# Volumen — Sprint 3

Los reportes leen `reservations` por proveedor o por cliente y por fecha. Esos accesos ya tienen índice: `idx_reservations_provider_date` y `idx_reservations_client_date` (V5). No se agregan tablas.

Una consulta no pasa de 31 días. Con turnos de 60 minutos y una ventana de 8 horas hay como máximo 8 huecos por servicio y día, unos 250 huecos por servicio en el rango. La ocupación recorre esa grilla en memoria a partir de la agenda vigente y de las reservas `CONFIRMED` del día.

Orden de magnitud para un proveedor con 5 servicios y la agenda llena: 5 × 8 × 31 = 1 240 reservas en el rango máximo, y la misma cantidad de filas en el historial. Cabe en una respuesta JSON sin paginar. Si un negocio supera eso, el límite de 31 días sigue acotando la lectura.

`reservation_lifecycle` no guarda filas: es una función. El trigger `trg_reservations_no_reopen` solo corre en `UPDATE` y rechaza el cambio de estado de una cancelada. No cambia el volumen de la tabla.
