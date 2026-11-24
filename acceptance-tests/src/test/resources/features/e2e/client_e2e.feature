# language: en
@e2e @e2e_client
Feature: Complete client journey
  As a person who wants to book a service
  I want to register, find the service, book it and manage my reservation
  So that I can organize my appointments without calling the business

  @HU-01 @HU-11 @HU-12 @HU-14 @HU-16 @HU-17 @CA-HU01-01 @CA-HU12-01 @CA-HU16-01 @CA-HU14-01 @CA-HU17-03
  Scenario: Register, book, reschedule, cancel and review the history
    Given the provider offers the service "Corte" with an all-week open schedule
    When a new client registers with their details
    And the client searches the catalog for the service "Corte"
    Then the catalog should show the service "Corte"
    When the client books the first free slot of the service "Corte"
    Then the client should see the booking confirmation for the service "Corte"
    When the client reviews their reservations
    Then the client should see the reservation for the service "Corte" with the status "Confirmada"
    When the client reschedules the reservation for the service "Corte"
    Then they should see the notification "Reserva reprogramada."
    And the client should see the reservation for the service "Corte" at a different time than before
    When the client cancels the reservation for the service "Corte" with the reason "Me surgió un viaje"
    Then the client should see the reservation for the service "Corte" with the status "Cancelada por el cliente"
    When the client reviews their history
    Then the client should see the service "Corte" in their history as "Cancelada"
