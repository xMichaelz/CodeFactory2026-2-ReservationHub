# language: es
@cliente @mis_reservas
Característica: Mis reservas
  Como cliente de ReservaHub
  Quiero consultar, cancelar y reprogramar mis reservas
  Para ajustar mis citas cuando cambian mis planes

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el cliente ha iniciado sesión
    Y que el cliente tiene una reserva del servicio "Corte"

  Escenario: El cliente ve su reserva confirmada
    Cuando el cliente revisa sus reservas
    Entonces debería ver el título "Mis reservas"
    Y el cliente debería ver la reserva del servicio "Corte" con el estado "Confirmada"

  Escenario: El cliente cancela su reserva indicando un motivo
    Cuando el cliente revisa sus reservas
    Y el cliente cancela la reserva del servicio "Corte" con el motivo "Me surgió un viaje"
    Entonces debería ver la notificación "Reserva cancelada."
    Y el cliente debería ver la reserva del servicio "Corte" con el estado "Cancelada por el cliente"

  Escenario: El cliente cancela su reserva sin indicar motivo
    Cuando el cliente revisa sus reservas
    Y el cliente cancela la reserva del servicio "Corte"
    Entonces debería ver la notificación "Reserva cancelada."
    Y el cliente debería ver la reserva del servicio "Corte" con el estado "Cancelada por el cliente"

  Escenario: El cliente reprograma su reserva a otro horario libre
    Cuando el cliente revisa sus reservas
    Y el cliente reprograma la reserva del servicio "Corte"
    Entonces debería ver la notificación "Reserva reprogramada."
    Y el cliente debería ver la reserva del servicio "Corte" en un horario distinto al anterior
    Y el cliente debería ver la reserva del servicio "Corte" con el estado "Confirmada"

  Escenario: Un rango de fechas sin reservas muestra el aviso vacío
    Cuando el cliente revisa sus reservas
    Y el cliente consulta sus reservas desde dentro de 200 días hasta dentro de 210 días
    Entonces debería ver el aviso vacío "No hay reservas en este rango"

  Escenario: Un rango de más de 31 días se rechaza antes de consultar
    Cuando el cliente revisa sus reservas
    Y el cliente consulta sus reservas desde dentro de 0 días hasta dentro de 40 días
    Entonces debería ver el error de validación "El rango no puede superar 31 días."
