# language: es
@proveedor @reportes
Característica: Reportes del negocio
  Como proveedor de servicios
  Quiero ver la ocupación, la demanda y las cancelaciones de mi agenda
  Para tomar decisiones sobre mis horarios y servicios

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana

  Escenario: El reporte de ocupación muestra el servicio
    Dado que el cliente tiene una reserva del servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor consulta el reporte de "Ocupación"
    Entonces el proveedor debería ver el servicio "Corte" en la tarjeta "Por servicio"

  Escenario: El reporte de demanda cuenta las reservas confirmadas
    Dado que el cliente tiene una reserva del servicio "Corte"
    Y que otro cliente tiene una reserva del servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor consulta el reporte de "Demanda"
    Entonces el proveedor debería ver la estadística "Reservas confirmadas" con el valor "2"
    Y el proveedor debería ver para el servicio "Corte" el valor "2" en la tarjeta "Reservas por servicio"

  Escenario: El reporte de cancelaciones separa las del cliente y las del proveedor
    Dado que el cliente tiene una reserva del servicio "Corte"
    Y que el cliente canceló esa reserva
    Y que otro cliente tiene una reserva del servicio "Corte"
    Y que el proveedor canceló la reserva de otro cliente
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor consulta el reporte de "Cancelaciones"
    Entonces el proveedor debería ver la estadística "Total cancelaciones" con el valor "2"
    Y el proveedor debería ver la estadística "Por el cliente" con el valor "1"
    Y el proveedor debería ver la estadística "Por el proveedor" con el valor "1"
    Y el proveedor debería ver para el servicio "Corte" el valor "1 / 1" en la tarjeta "Cancelaciones por servicio"

  Escenario: Las cancelaciones se pueden filtrar por servicio
    Dado que el proveedor ofrece el servicio "Tinte" con agenda abierta toda la semana
    Y que el cliente tiene una reserva del servicio "Corte"
    Y que el cliente canceló esa reserva
    Y que otro cliente tiene una reserva del servicio "Tinte"
    Y que el proveedor canceló la reserva de otro cliente
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor consulta el reporte de "Cancelaciones" solo del servicio "Corte"
    Entonces el proveedor debería ver la estadística "Total cancelaciones" con el valor "1"
    Y el proveedor debería ver la estadística "Por el cliente" con el valor "1"
    Y el proveedor debería ver la estadística "Por el proveedor" con el valor "0"
    Y el proveedor no debería ver el servicio "Tinte" en la tarjeta "Cancelaciones por servicio"

  Escenario: El rango de los reportes no puede superar 31 días
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor consulta los reportes de los próximos 40 días
    Entonces debería ver el error de validación "El rango no puede superar 31 días."
