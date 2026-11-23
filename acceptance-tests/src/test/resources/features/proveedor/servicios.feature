# language: es
@proveedor @servicios
Característica: Catálogo de servicios del proveedor
  Como proveedor de servicios
  Quiero crear, editar y desactivar mis servicios y asociarles recursos
  Para controlar lo que los clientes pueden reservar

  Escenario: El proveedor crea un servicio y queda activo
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor crea el servicio "Manicure" desde la sección Servicios
    Entonces debería ver la notificación "creado."
    Y el proveedor debería ver el servicio "Manicure" con el estado "Activo"

  Escenario: El proveedor cambia el precio de un servicio
    Dado que el proveedor tiene el servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor cambia el precio del servicio "Corte" a 40000
    Entonces debería ver la notificación "actualizado."
    Y el proveedor debería ver el servicio "Corte" con un precio de "40.000"

  Escenario: No se puede crear un servicio con un nombre que ya existe
    Dado que el proveedor tiene el servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor intenta crear otro servicio con el mismo nombre de "Corte"
    Entonces debería ver en la ventana el mensaje de error "Ya tienes un servicio con ese nombre."

  Escenario: El proveedor desactiva un servicio
    Dado que el proveedor tiene el servicio "Corte"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor desactiva el servicio "Corte"
    Entonces debería ver la notificación "quedó inactivo."
    Y el proveedor debería ver el servicio "Corte" con el estado "Inactivo"

  Escenario: El proveedor asocia un recurso a un servicio
    Dado que el proveedor tiene el servicio "Corte"
    Y que el proveedor tiene el recurso "Silla"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor asocia el recurso "Silla" al servicio "Corte"
    Entonces debería ver la notificación "Recursos asociados."
    Y el proveedor debería ver el recurso "Silla" en la fila del servicio "Corte"
