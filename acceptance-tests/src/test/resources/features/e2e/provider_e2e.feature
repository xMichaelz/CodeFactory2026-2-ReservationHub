# language: en
@e2e @e2e_provider
Feature: Complete provider journey
  As the owner of a service business
  I want to register my business, set it up and serve reservations
  So that I can receive clients and measure how my agenda is doing

  @HU-02 @HU-05 @HU-08 @HU-09 @HU-10 @HU-13 @HU-19 @HU-20 @CA-HU02-01 @CA-HU05-01 @CA-HU08-01 @CA-HU09-01 @CA-HU10-05 @CA-HU13-01 @CA-HU19-01 @CA-HU20-01
  Scenario: Register the business, set it up, book for a client and review the reports
    When a new provider registers their business
    And the provider registers the resource "Camilla"
    And the provider creates the service "Terapia" from the Services section
    And the provider links the resource "Camilla" to the service "Terapia"
    Then the provider should see the resource "Camilla" in the row of the service "Terapia"
    When the provider adds a window on "Lunes" from "08:00" to "18:00"
    And the provider adds a window on "Miércoles" from "08:00" to "18:00"
    And the provider adds a window on "Viernes" from "08:00" to "18:00"
    And the provider blocks the window on "Lunes" from "12:00" to "13:00" for "Almuerzo"
    Then the provider should see the block "Almuerzo" in the window on "Lunes"
    Given the client is registered
    When the provider books "Terapia" on behalf of the client in a slot of the following week
    Then the provider should see the reservation created in the client's name
    When the provider goes to their agenda from the confirmation
    Then the provider should see the client's reservation in their agenda with the status "Confirmada"
    When the provider views the "Demanda" report
    Then the provider should see the statistic "Reservas confirmadas" with the value "1"
    And the provider should see for the service "Terapia" the value "1" in the card "Reservas por servicio"
    When the provider views the "Ocupación" report
    Then the provider should see the service "Terapia" in the card "Por servicio"
