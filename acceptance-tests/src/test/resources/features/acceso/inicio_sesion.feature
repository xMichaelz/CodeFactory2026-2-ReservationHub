# language: es
@acceso @login
Característica: Inicio y cierre de sesión
  Como usuario registrado de ReservaHub
  Quiero iniciar sesión con mi correo y contraseña
  Para gestionar mis reservas o mi negocio

  Escenario: El cliente inicia sesión y llega a su inicio
    Dado que el cliente está registrado
    Cuando el cliente inicia sesión
    Entonces debería estar en la página "/client"
    Y debería ver el rol "Cliente" en la barra superior

  Escenario: El proveedor inicia sesión y llega al panel del negocio
    Dado que el proveedor está registrado
    Cuando el proveedor inicia sesión
    Entonces debería estar en la página "/provider"
    Y debería ver el título "Panel del negocio"
    Y debería ver el rol "Proveedor" en la barra superior

  Escenario: Inicio de sesión rechazado con una contraseña incorrecta
    Dado que el cliente está registrado
    Cuando el cliente intenta iniciar sesión con la contraseña "Incorrecta999"
    Entonces debería ver el mensaje de error "Correo o contraseña incorrectos."

  Escenario: Inicio de sesión rechazado con un correo que no está registrado
    Cuando alguien intenta iniciar sesión con el correo "nadie@screenplay.test"
    Entonces debería ver el mensaje de error "Correo o contraseña incorrectos."

  Escenario: La cuenta se bloquea después de 5 intentos fallidos
    Dado que el cliente está registrado
    Cuando el cliente falla el inicio de sesión 5 veces
    Y el cliente intenta iniciar sesión con la contraseña "Secreta123"
    Entonces debería ver un mensaje de error que empieza por "Cuenta bloqueada por intentos fallidos"

  Escenario: Cerrar sesión devuelve al inicio de sesión
    Dado que el cliente ha iniciado sesión
    Cuando cierra la sesión
    Entonces debería estar en la página "/login"
    Cuando abre directamente la página "/client/reservations"
    Entonces debería estar en la página "/login"
