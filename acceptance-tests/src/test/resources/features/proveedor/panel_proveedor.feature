# language: es
@proveedor @panel_proveedor
Característica: Panel del negocio
  Como proveedor de servicios
  Quiero ver el estado de la configuración de mi negocio
  Para saber qué me falta antes de recibir reservas

  Escenario: Un proveedor nuevo ve la lista de configuración pendiente
    Dado que el proveedor ha iniciado sesión
    Entonces el proveedor debería ver la tarjeta "Completa tu configuración"
    Y el proveedor debería ver pendiente el paso "Crea al menos un servicio activo"
    Y el proveedor debería ver pendiente el paso "Define tus horarios de atención"
    Y el proveedor debería ver la estadística "Servicios activos" con el valor "0"

  Escenario: Con un servicio y horarios el panel cuenta un servicio activo
    Dado que el proveedor ofrece el servicio "Corte" con agenda abierta toda la semana
    Y que el proveedor ha iniciado sesión
    Entonces el proveedor debería ver la estadística "Servicios activos" con el valor "1"
    Y el proveedor debería ver la estadística "Franjas de atención" con el valor "7"
