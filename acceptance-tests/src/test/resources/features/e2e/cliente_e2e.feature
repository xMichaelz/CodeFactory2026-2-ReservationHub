# language: es
@e2e @e2e_cliente
Característica: Recorrido completo del cliente
  Como persona que quiere reservar un servicio
  Quiero registrarme, encontrar el servicio, reservarlo y gestionar mi reserva
  Para organizar mis citas sin llamar al negocio

  Escenario: Registrarse, reservar, reprogramar, cancelar y revisar el historial
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Cuando un cliente nuevo se registra con sus datos
    Y el cliente busca en el catálogo el servicio "Corte"
    Entonces el catálogo debería mostrar el servicio "Corte"
    Cuando el cliente reserva el primer horario libre del servicio "Corte"
    Entonces el cliente debería ver la confirmación de la reserva del servicio "Corte"
    Cuando el cliente revisa sus reservas
    Entonces el cliente debería ver la reserva del servicio "Corte" con el estado "Confirmada"
    Cuando el cliente reprograma la reserva del servicio "Corte"
    Entonces debería ver la notificación "Reserva reprogramada."
    Y el cliente debería ver la reserva del servicio "Corte" en un horario distinto al anterior
    Cuando el cliente cancela la reserva del servicio "Corte" con el motivo "Me surgió un viaje"
    Entonces el cliente debería ver la reserva del servicio "Corte" con el estado "Cancelada por el cliente"
    Cuando el cliente revisa su historial
    Entonces el cliente debería ver en su historial el servicio "Corte" como "Cancelada"
