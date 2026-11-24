# language: en
@client @booking
Feature: Book a service from the catalog
  As a ReservaHub client
  I want to see the free slots of a service and book one
  So that I can secure my appointment without calling the business

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule
    And the client is logged in

  @HU-12 @CA-HU12-01
  Scenario: The client books the first free slot and sees the confirmation
    When the client books the first free slot of the service "Corte"
    Then the client should see the booking confirmation for the service "Corte"
    And they should see the notification "¡Reserva confirmada!"

  @HU-12 @CA-HU12-01
  Scenario: The booking appears as confirmed in My reservations
    When the client books the first free slot of the service "Corte"
    And the client reviews their reservations
    Then the client should see the reservation for the service "Corte" with the status "Confirmada"

  @HU-11 @CA-HU11-01
  Scenario: The client navigates to the next week of availability
    When the client opens the availability of the service "Corte"
    And the client moves to the next week of availability
    Then the client should see availability starting 7 days from now
