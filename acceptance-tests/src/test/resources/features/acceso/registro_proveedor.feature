# language: es
@acceso @registro @registro_proveedor
Característica: Registro de proveedores
  Como dueño de un negocio de servicios
  Quiero registrar mi negocio en ReservaHub
  Para publicar mis servicios y recibir reservas

  Escenario: Registro exitoso lleva al panel del negocio
    Cuando un proveedor nuevo registra su negocio
    Entonces debería estar en la página "/provider"
    Y debería ver el título "Panel del negocio"
    Y debería ver la notificación "Negocio registrado"

  Escenario: Registro rechazado con un nombre de negocio ya registrado
    Dado que el proveedor está registrado
    Cuando otro proveedor intenta registrar un negocio con el mismo nombre
    Entonces debería ver el mensaje de error "Ese nombre de negocio ya está registrado."
