# language: es
@proveedor @recursos
Característica: Recursos del proveedor
  Como proveedor de servicios
  Quiero registrar los equipos, salas o profesionales de mi negocio
  Para asociarlos a los servicios que los necesitan

  Escenario: El proveedor registra un recurso y aparece en su lista
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor registra el recurso "Sala"
    Entonces debería ver la notificación "registrado."
    Y el proveedor debería ver el recurso "Sala" en Mis recursos

  Escenario: No se puede registrar un recurso con un nombre que ya existe
    Dado que el proveedor tiene el recurso "Silla"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor intenta registrar otro recurso con el mismo nombre de "Silla"
    Entonces debería ver el mensaje de error "Ya tienes un recurso con ese nombre."

  Escenario: El navegador no deja registrar un recurso con cantidad 0
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor intenta registrar el recurso "Sala" con cantidad "0"
    Entonces el navegador no debería dejar enviar una cantidad menor que 1
    Y debería ver el aviso vacío "Aún no tienes recursos"
