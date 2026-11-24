# language: en
@client @client_history
Feature: Client reservation history
  As a ReservaHub client
  I want to see my past and upcoming reservations with their lifecycle
  So that I can keep track of what I booked and cancelled

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule
    And the provider offers the service "Barba" with an all-week open schedule
    And the client is logged in
    And the client has a reservation for the service "Corte"
    And the client has a reservation for the service "Barba"
    And the client cancelled that reservation

  @HU-17 @CA-HU17-01 @CA-HU17-03
  Scenario: The history shows the pending and the cancelled reservation
    When the client reviews their history
    Then they should see the title "Historial de reservas"
    And the client should see the service "Corte" in their history as "Pendiente"
    And the client should see the service "Barba" in their history as "Cancelada"

  @HU-17 @CA-HU17-03
  Scenario: The "Cancelada" tab shows only cancelled reservations
    When the client reviews their history
    And the client filters their history by "Cancelada"
    Then the client's history should only show "Cancelada" reservations
    And the client should see the service "Barba" in their history as "Cancelada"
    And the client's history should not include the service "Corte"

  @HU-17 @CA-HU17-01
  Scenario: The "Pendiente" tab shows only upcoming reservations
    When the client reviews their history
    And the client filters their history by "Pendiente"
    Then the client's history should only show "Pendiente" reservations
    And the client should see the service "Corte" in their history as "Pendiente"
    And the client's history should not include the service "Barba"
