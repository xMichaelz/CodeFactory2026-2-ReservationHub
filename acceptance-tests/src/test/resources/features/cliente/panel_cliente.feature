# language: es
@cliente @panel_cliente
Característica: Inicio del cliente
  Como cliente de ReservaHub
  Quiero ver un resumen de mis próximas citas al entrar
  Para saber qué tengo reservado sin buscarlo

  Escenario: El inicio muestra el saludo y la próxima cita reservada
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el cliente ha iniciado sesión
    Y que el cliente tiene una reserva del servicio "Corte"
    Cuando el cliente abre su inicio
    Entonces el cliente debería ver su saludo de bienvenida
    Y el cliente debería ver el servicio "Corte" en sus próximas citas
