# language: es
@cliente @catalogo
Característica: Catálogo de servicios
  Como cliente de ReservaHub
  Quiero explorar los servicios activos de los proveedores
  Para encontrar el que quiero reservar

  Antecedentes:
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el cliente ha iniciado sesión

  Escenario: El catálogo muestra el servicio activo del proveedor
    Cuando el cliente abre el catálogo
    Entonces debería ver el título "Catálogo de servicios"
    Y el catálogo debería mostrar el servicio "Corte"

  Escenario: Buscar por el nombre del servicio lo encuentra
    Cuando el cliente busca en el catálogo el servicio "Corte"
    Entonces el catálogo debería mostrar el servicio "Corte"

  Escenario: Una búsqueda sin coincidencias muestra el aviso vacío
    Cuando el cliente busca en el catálogo el texto "servicio-que-no-existe-zzz"
    Entonces debería ver el aviso vacío "Sin resultados"

  Escenario: Un servicio desactivado no aparece en el catálogo
    Dado que el proveedor tiene el servicio "Tinte"
    Y que el proveedor desactivó el servicio "Tinte"
    Cuando el cliente busca en el catálogo el negocio del proveedor
    Entonces el catálogo debería mostrar el servicio "Corte"
    Y el catálogo no debería mostrar el servicio "Tinte"

  Escenario: Filtrar por proveedor muestra solo los servicios de ese negocio
    Dado que el proveedor tiene el servicio "Barba"
    Cuando el cliente filtra el catálogo por el negocio del proveedor
    Entonces el catálogo solo debería mostrar servicios del negocio del proveedor
    Y el catálogo debería mostrar el servicio "Corte"
    Y el catálogo debería mostrar el servicio "Barba"
