# language: es
@proveedor @reservar_para_cliente
Característica: Reservar a nombre de un cliente
  Como proveedor de servicios
  Quiero crear reservas para clientes que me llaman o me visitan
  Para que también queden en la agenda del negocio

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana

  Escenario: Buscar un correo que no es de ningún cliente
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor busca al cliente con el correo "nadie-registrado@screenplay.test"
    Entonces debería ver el mensaje de error "No se encontró un cliente activo con ese correo."

  Escenario: Buscar a un cliente registrado muestra su nombre
    Dado que el cliente está registrado
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor busca al cliente por su correo
    Entonces el proveedor debería ver que encontró al cliente por su nombre completo

  Escenario: La reserva hecha para el cliente queda en la agenda
    Dado que el cliente está registrado
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor reserva "Corte" a nombre del cliente en un horario de la semana siguiente
    Entonces debería ver la notificación "Reserva creada para"
    Y el proveedor debería ver la reserva creada a nombre del cliente
    Cuando el proveedor va a su agenda desde la confirmación
    Entonces el proveedor debería ver en su agenda la reserva del cliente con el estado "Confirmada"
