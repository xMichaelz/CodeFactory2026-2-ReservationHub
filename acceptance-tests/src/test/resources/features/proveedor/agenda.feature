# language: es
@proveedor @agenda
Característica: Agenda del proveedor
  Como proveedor de servicios
  Quiero ver las reservas de mi negocio y poder cancelarlas o moverlas
  Para organizar mi trabajo cuando surgen imprevistos

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana

  Escenario: La agenda muestra la reserva con el nombre del cliente
    Dado que el cliente tiene una reserva del servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor abre su agenda
    Entonces el proveedor debería ver en su agenda la reserva del cliente con el estado "Confirmada"

  Escenario: El navegador no deja cancelar sin escribir el motivo
    Dado que el proveedor tiene una cita del cliente mañana para "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor intenta cancelar la reserva del cliente sin escribir un motivo
    Entonces el navegador no debería dejar enviar la cancelación sin motivo

  Escenario: Un motivo con solo espacios no sirve para cancelar
    Dado que el proveedor tiene una cita del cliente mañana para "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor cancela la reserva del cliente con el motivo "   "
    Entonces debería ver en la ventana el mensaje de error "Como proveedor debes indicar el motivo de la cancelación."

  Escenario: El proveedor cancela una reserva indicando el motivo
    Dado que el proveedor tiene una cita del cliente mañana para "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor cancela la reserva del cliente con el motivo "El profesional está incapacitado"
    Entonces debería ver la notificación "Reserva cancelada."
    Y el proveedor debería ver en su agenda la reserva del cliente con el estado "Cancelada por el proveedor"

  Escenario: El proveedor reprograma una reserva a otro horario
    Dado que el proveedor tiene una cita del cliente mañana para "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor reprograma la reserva del cliente a un horario de la semana siguiente
    Entonces debería ver la notificación "Reserva reprogramada."
    Y el proveedor debería ver la reserva del cliente en otro horario
