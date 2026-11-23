# language: es
@proveedor @historial_negocio
Característica: Historial de reservas del negocio
  Como proveedor de servicios
  Quiero ver todas las reservas de mi negocio, también las canceladas
  Para conocer qué pasó con cada cita

  Escenario: El historial lista reservas confirmadas y canceladas con su cliente
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el proveedor tiene una cita del cliente mañana para "Corte"
    Y que otro cliente tiene una reserva del servicio "Corte"
    Y que el proveedor canceló la reserva de otro cliente
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor abre el historial del negocio
    Entonces el proveedor debería ver en el historial la reserva de "el cliente" con el ciclo "Pendiente" y el estado "Confirmada"
    Y el proveedor debería ver en el historial la reserva de "otro cliente" con el ciclo "Cancelada" y el estado "Cancelada por el proveedor"
