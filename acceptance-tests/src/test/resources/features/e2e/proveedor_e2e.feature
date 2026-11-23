# language: es
@e2e @e2e_proveedor
Característica: Recorrido completo del proveedor
  Como dueño de un negocio de servicios
  Quiero registrar mi negocio, configurarlo y atender reservas
  Para recibir clientes y medir cómo va mi agenda

  Escenario: Registrar el negocio, configurarlo, reservar a un cliente y revisar los reportes
    Cuando un proveedor nuevo registra su negocio
    Y el proveedor registra el recurso "Camilla"
    Y el proveedor crea el servicio "Terapia" desde la sección Servicios
    Y el proveedor asocia el recurso "Camilla" al servicio "Terapia"
    Entonces el proveedor debería ver el recurso "Camilla" en la fila del servicio "Terapia"
    Cuando el proveedor agrega una franja el "Lunes" de "08:00" a "18:00"
    Y el proveedor agrega una franja el "Miércoles" de "08:00" a "18:00"
    Y el proveedor agrega una franja el "Viernes" de "08:00" a "18:00"
    Y el proveedor bloquea la franja del "Lunes" de "12:00" a "13:00" por "Almuerzo"
    Entonces el proveedor debería ver el bloqueo "Almuerzo" en la franja del "Lunes"
    Dado que el cliente está registrado
    Cuando el proveedor reserva "Terapia" a nombre del cliente en un horario de la semana siguiente
    Entonces el proveedor debería ver la reserva creada a nombre del cliente
    Cuando el proveedor va a su agenda desde la confirmación
    Entonces el proveedor debería ver en su agenda la reserva del cliente con el estado "Confirmada"
    Cuando el proveedor consulta el reporte de "Demanda"
    Entonces el proveedor debería ver la estadística "Reservas confirmadas" con el valor "1"
    Y el proveedor debería ver para el servicio "Terapia" el valor "1" en la tarjeta "Reservas por servicio"
    Cuando el proveedor consulta el reporte de "Ocupación"
    Entonces el proveedor debería ver el servicio "Terapia" en la tarjeta "Por servicio"
