# Criterios de HU-17 a HU-21.
# Los ejecuta Sprint3ReportTest (JUnit + MockMvc + Testcontainers). No hay runner de Cucumber.
Feature: Consultas del sprint 3

  Scenario: El cliente ve sus reservas pasadas y futuras
    Given un cliente con una reserva de ayer ya terminada y otra de mañana
    When consulta GET /api/v1/reports/my-reservations
    Then ve la de ayer como COMPLETED y la de mañana como PENDING
    And cada una trae servicio, proveedor, fecha y hora

  Scenario: El proveedor ve las reservas de su negocio
    Given esas mismas reservas
    When consulta GET /api/v1/reports/business-reservations
    Then ve el nombre del cliente, el estado guardado y el ciclo de vida
    And también aparecen las canceladas y las completadas

  Scenario: La ocupación es huecos ocupados frente a los de la agenda
    Given una agenda de 09:00 a 12:00 con turnos de 60 minutos y una reserva a las 09:00
    When el proveedor consulta GET /api/v1/reports/occupancy filtrando por ese servicio
    Then offeredSlots es 3, occupiedSlots es 1 y occupancyRate es 0.3333
    And un serviceId de otro proveedor responde 404

  Scenario: La demanda no cuenta cancelaciones
    Given tres reservas y dos de ellas canceladas, una por el cliente y otra por el proveedor
    When el proveedor consulta GET /api/v1/reports/demand
    Then totalDelivered es 1
    And GET /api/v1/reports/cancellations separa 1 del cliente y 1 del proveedor

  Scenario: Los reportes exigen token y rol
    Given un request sin Authorization
    Then la respuesta es 401
    And un cliente que pide un reporte del proveedor recibe 403
    And un proveedor que pide el historial del cliente recibe 403
    And un rango invertido o de más de 31 días recibe 400

  Scenario: Prometheus queda detrás del token
    Given GET /actuator/prometheus sin JWT
    Then la respuesta es 401
    And con un JWT válido la respuesta es 200 y trae métricas
