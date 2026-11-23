# language: es
@cliente @reservar
Característica: Reservar un servicio desde el catálogo
  Como cliente de ReservaHub
  Quiero ver los horarios libres de un servicio y reservar uno
  Para asegurar mi cita sin llamar al negocio

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el cliente ha iniciado sesión

  Escenario: El cliente reserva el primer horario libre y ve la confirmación
    Cuando el cliente reserva el primer horario libre del servicio "Corte"
    Entonces el cliente debería ver la confirmación de la reserva del servicio "Corte"
    Y debería ver la notificación "¡Reserva confirmada!"

  Escenario: La reserva hecha aparece confirmada en Mis reservas
    Cuando el cliente reserva el primer horario libre del servicio "Corte"
    Y el cliente revisa sus reservas
    Entonces el cliente debería ver la reserva del servicio "Corte" con el estado "Confirmada"

  Escenario: El cliente navega a la semana siguiente de la disponibilidad
    Cuando el cliente abre la disponibilidad del servicio "Corte"
    Y el cliente pasa a la semana siguiente de la disponibilidad
    Entonces el cliente debería ver la disponibilidad a partir de dentro de 7 días
