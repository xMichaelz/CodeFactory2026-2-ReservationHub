# language: es
@proveedor @horarios
Característica: Horarios de atención y bloqueos
  Como proveedor de servicios
  Quiero definir mis franjas semanales y bloquear ratos concretos
  Para que los clientes solo reserven cuando puedo atenderlos

  Escenario: El proveedor agrega una franja y la ve en su día
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor agrega una franja el "Martes" de "08:00" a "12:00"
    Entonces debería ver la notificación "Franja del martes creada."
    Y el proveedor debería ver la franja de "08:00" a "12:00" el "Martes"

  Escenario: No se puede agregar una franja que se cruza con otra del mismo día
    Dado que el proveedor tiene una franja el "Lunes" de "08:00" a "12:00"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor agrega una franja el "Lunes" de "10:00" a "14:00"
    Entonces debería ver el mensaje de error "El horario se cruza con otra franja del mismo día."

  Escenario: La hora de inicio de la franja debe ser anterior a la de fin
    Dado que el proveedor ha iniciado sesión
    Cuando el proveedor agrega una franja el "Miércoles" de "14:00" a "10:00"
    Entonces debería ver el mensaje de error "La hora de inicio debe ser anterior a la hora de fin."

  Escenario: El proveedor bloquea parte de una franja con un motivo
    Dado que el proveedor tiene una franja el "Lunes" de "08:00" a "12:00"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor bloquea la franja del "Lunes" de "09:00" a "10:00" por "Capacitación"
    Entonces debería ver la notificación "Bloqueo registrado."
    Y el proveedor debería ver el bloqueo "Capacitación" en la franja del "Lunes"

  Escenario: El bloqueo debe quedar dentro del horario de la franja
    Dado que el proveedor tiene una franja el "Lunes" de "08:00" a "12:00"
    Y que el proveedor ha iniciado sesión
    Cuando el proveedor bloquea la franja del "Lunes" de "13:00" a "14:00" por "Festivo"
    Entonces debería ver en la ventana el mensaje de error "El bloqueo debe estar dentro del horario de la franja."
