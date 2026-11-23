# language: es
@cliente @historial_cliente
Característica: Historial de reservas del cliente (HU-17)
  Como cliente de ReservaHub
  Quiero ver mis reservas pasadas y futuras con su ciclo de vida
  Para llevar el control de lo que reservé y cancelé

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el proveedor ofrece el servicio "Barba" con agenda abierta toda la semana
    Y que el cliente ha iniciado sesión
    Y que el cliente tiene una reserva del servicio "Corte"
    Y que el cliente tiene una reserva del servicio "Barba"
    Y que el cliente canceló esa reserva

  Escenario: El historial muestra la reserva pendiente y la cancelada
    Cuando el cliente revisa su historial
    Entonces debería ver el título "Historial de reservas"
    Y el cliente debería ver en su historial el servicio "Corte" como "Pendiente"
    Y el cliente debería ver en su historial el servicio "Barba" como "Cancelada"

  Escenario: La pestaña "Cancelada" muestra solo las reservas canceladas
    Cuando el cliente revisa su historial
    Y el cliente filtra su historial por "Cancelada"
    Entonces el historial del cliente solo debería mostrar reservas "Cancelada"
    Y el cliente debería ver en su historial el servicio "Barba" como "Cancelada"
    Y el historial del cliente no debería incluir el servicio "Corte"

  Escenario: La pestaña "Pendiente" muestra solo las reservas futuras
    Cuando el cliente revisa su historial
    Y el cliente filtra su historial por "Pendiente"
    Entonces el historial del cliente solo debería mostrar reservas "Pendiente"
    Y el cliente debería ver en su historial el servicio "Corte" como "Pendiente"
    Y el historial del cliente no debería incluir el servicio "Barba"
