# language: en
@e2e @e2e_full_booking
Feature: Complete booking between provider and client
  As a provider and a client
  I want what a provider publishes to be bookable by a client and both to see the changes
  So that the business agenda and the client's appointments always match

  @HU-01 @HU-02 @HU-05 @HU-08 @HU-11 @HU-12 @HU-15 @HU-17 @HU-18 @CA-HU12-01 @CA-HU15-01 @CA-HU17-03
  Scenario: The provider publishes a service, the client books it and the provider cancels it with a reason
    When a new provider registers their business
    And the provider creates the service "Masaje" from the Services section
    And the provider adds a window on "Lunes" from "06:00" to "22:00"
    And the provider adds a window on "Miércoles" from "06:00" to "22:00"
    And the provider adds a window on "Viernes" from "06:00" to "22:00"
    And the provider adds a window on "Domingo" from "06:00" to "22:00"
    And a new client registers with their details
    And the client searches the catalog for the service "Masaje"
    Then the catalog should show the service "Masaje"
    When the client books the first free slot of the service "Masaje"
    Then the client should see the booking confirmation for the service "Masaje"
    When the provider logs in
    And the provider opens their agenda
    Then the provider should see the client's reservation in their agenda with the status "Confirmada"
    When the provider cancels the client's reservation with the reason "Cierre por mantenimiento"
    Then the provider should see the client's reservation in their agenda with the status "Cancelada por el proveedor"
    When the client logs in
    And the client reviews their reservations
    Then the client should see the reservation for the service "Masaje" with the status "Cancelada por el proveedor"
    When the client reviews their history
    Then the client should see the service "Masaje" in their history as "Cancelada"
