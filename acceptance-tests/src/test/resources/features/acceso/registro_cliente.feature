# language: es
@acceso @registro @registro_cliente
Característica: Registro de clientes
  Como persona que quiere reservar servicios
  Quiero crear mi cuenta de cliente
  Para poder reservar en el catálogo de ReservaHub

  Escenario: Registro exitoso lleva al catálogo con la sesión iniciada
    Cuando un cliente nuevo se registra con sus datos
    Entonces debería estar en la página "/client/catalog"
    Y debería ver la notificación "Cuenta creada"
    Y debería ver el rol "Cliente" en la barra superior

  Escenario: Registro rechazado con una contraseña corta
    Cuando un cliente nuevo se registra con la contraseña "abc12"
    Entonces debería ver el mensaje de error "La contraseña debe tener al menos 8 caracteres."

  Escenario: Registro rechazado con una contraseña sin números
    Cuando un cliente nuevo se registra con la contraseña "SoloLetras"
    Entonces debería ver el mensaje de error "La contraseña debe contener al menos una letra y un número."

  Escenario: Registro rechazado si las contraseñas no coinciden
    Cuando un cliente nuevo se registra con una confirmación de contraseña distinta
    Entonces debería ver el mensaje de error "Las contraseñas no coinciden."

  Escenario: Registro rechazado con un correo ya registrado
    Dado que el cliente está registrado
    Cuando otra persona intenta registrarse con el mismo correo del cliente
    Entonces debería ver el mensaje de error "Ese correo ya está registrado."
