# language: es
@acceso @permisos
Característica: Páginas protegidas por sesión y por rol
  Como responsable de la plataforma
  Quiero que cada página solo la vea quien tiene permiso
  Para que un cliente no administre negocios ni un visitante vea reservas ajenas

  Escenario: Sin sesión, una página de cliente redirige al inicio de sesión
    Cuando alguien sin sesión abre la página "/client/reservations"
    Entonces debería estar en la página "/login"

  Escenario: Sin sesión, una página de proveedor redirige al inicio de sesión
    Cuando alguien sin sesión abre la página "/provider/services"
    Entonces debería estar en la página "/login"

  Escenario: Un cliente no puede abrir las páginas del proveedor
    Dado que el cliente ha iniciado sesión
    Cuando abre directamente la página "/provider/services"
    Entonces debería estar en la página "/client"

  Escenario: Un proveedor no puede abrir las páginas del cliente
    Dado que el proveedor ha iniciado sesión
    Cuando abre directamente la página "/client/catalog"
    Entonces debería estar en la página "/provider"
