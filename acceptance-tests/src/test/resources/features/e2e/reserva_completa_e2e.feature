# language: es
@e2e @e2e_reserva_completa
Característica: Reserva completa entre proveedor y cliente
  Como plataforma de reservas
  Quiero que lo que publica un proveedor lo pueda reservar un cliente y que ambos vean los cambios
  Para que la agenda del negocio y las citas del cliente siempre coincidan

  Escenario: El proveedor publica un servicio, el cliente lo reserva y el proveedor lo cancela con motivo
    Cuando un proveedor nuevo registra su negocio
    Y el proveedor crea el servicio "Masaje" desde la sección Servicios
    Y el proveedor agrega una franja el "Lunes" de "06:00" a "22:00"
    Y el proveedor agrega una franja el "Miércoles" de "06:00" a "22:00"
    Y el proveedor agrega una franja el "Viernes" de "06:00" a "22:00"
    Y el proveedor agrega una franja el "Domingo" de "06:00" a "22:00"
    Y un cliente nuevo se registra con sus datos
    Y el cliente busca en el catálogo el servicio "Masaje"
    Entonces el catálogo debería mostrar el servicio "Masaje"
    Cuando el cliente reserva el primer horario libre del servicio "Masaje"
    Entonces el cliente debería ver la confirmación de la reserva del servicio "Masaje"
    Cuando el proveedor inicia sesión
    Y el proveedor abre su agenda
    Entonces el proveedor debería ver en su agenda la reserva del cliente con el estado "Confirmada"
    Cuando el proveedor cancela la reserva del cliente con el motivo "Cierre por mantenimiento"
    Entonces el proveedor debería ver en su agenda la reserva del cliente con el estado "Cancelada por el proveedor"
    Cuando el cliente inicia sesión
    Y el cliente revisa sus reservas
    Entonces el cliente debería ver la reserva del servicio "Masaje" con el estado "Cancelada por el proveedor"
    Cuando el cliente revisa su historial
    Entonces el cliente debería ver en su historial el servicio "Masaje" como "Cancelada"
