# language: en
@provider @business_history
Feature: Business reservation history
  As a service provider
  I want to see all my business reservations, including the cancelled ones
  So that I know what happened with each appointment

  @HU-18 @CA-HU18-01 @CA-HU18-02
  Scenario: The history lists confirmed and cancelled reservations with their client
    Given the provider offers the service "Corte" with an all-week open schedule
    And the provider has an appointment with the client tomorrow for "Corte"
    And another client has a reservation for the service "Corte"
    And the provider cancelled the other client's reservation
    And the provider is logged in
    When the provider opens the business history
    Then the provider should see in the history the reservation of "the client" with the lifecycle "Pendiente" and the status "Confirmada"
    And the provider should see in the history the reservation of "another client" with the lifecycle "Cancelada" and the status "Cancelada por el proveedor"
