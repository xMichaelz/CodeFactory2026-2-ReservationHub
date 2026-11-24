# language: en
@client @my_reservations
Feature: My reservations
  As a ReservaHub client
  I want to view, cancel and reschedule my reservations
  So that I can adjust my appointments when my plans change

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule
    And the client is logged in
    And the client has a reservation for the service "Corte"

  @HU-17
  Scenario: The client sees their confirmed reservation
    When the client reviews their reservations
    Then they should see the title "Mis reservas"
    And the client should see the reservation for the service "Corte" with the status "Confirmada"

  @HU-14 @CA-HU14-01
  Scenario: The client cancels their reservation giving a reason
    When the client reviews their reservations
    And the client cancels the reservation for the service "Corte" with the reason "Me surgió un viaje"
    Then they should see the notification "Reserva cancelada."
    And the client should see the reservation for the service "Corte" with the status "Cancelada por el cliente"

  @HU-14 @CA-HU14-01 @CA-HU14-06
  Scenario: The client cancels their reservation without giving a reason
    When the client reviews their reservations
    And the client cancels the reservation for the service "Corte"
    Then they should see the notification "Reserva cancelada."
    And the client should see the reservation for the service "Corte" with the status "Cancelada por el cliente"

  @HU-16 @CA-HU16-01
  Scenario: The client reschedules their reservation to another free slot
    When the client reviews their reservations
    And the client reschedules the reservation for the service "Corte"
    Then they should see the notification "Reserva reprogramada."
    And the client should see the reservation for the service "Corte" at a different time than before
    And the client should see the reservation for the service "Corte" with the status "Confirmada"

  @HU-17 @CA-HU17-05
  Scenario: A date range with no reservations shows the empty notice
    When the client reviews their reservations
    And the client views their reservations from 200 days from now to 210 days from now
    Then they should see the empty-state notice "No hay reservas en este rango"

  @HU-17 @CA-HU17-07
  Scenario: A range longer than 31 days is rejected before querying
    When the client reviews their reservations
    And the client views their reservations from 0 days from now to 40 days from now
    Then they should see the validation error "El rango no puede superar 31 días."
