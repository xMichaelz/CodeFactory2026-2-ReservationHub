# language: en
@provider @agenda
Feature: Provider agenda
  As a service provider
  I want to see my business reservations and be able to cancel or move them
  So that I can organize my work when unexpected events come up

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule

  @HU-18 @CA-HU18-01
  Scenario: The agenda shows the reservation with the client's name
    Given the client has a reservation for the service "Corte"
    And the provider is logged in
    When the provider opens their agenda
    Then the provider should see the client's reservation in their agenda with the status "Confirmada"

  @HU-15 @CA-HU15-02
  Scenario: The provider cannot cancel a reservation without writing a reason
    Given the provider has an appointment with the client tomorrow for "Corte"
    And the provider is logged in
    When the provider tries to cancel the client's reservation without writing a reason
    Then the provider should not be able to submit the cancellation without a reason

  @HU-15 @CA-HU15-02
  Scenario: A reason made only of spaces is not valid for cancelling
    Given the provider has an appointment with the client tomorrow for "Corte"
    And the provider is logged in
    When the provider cancels the client's reservation with the reason "   "
    Then they should see the error message "Como proveedor debes indicar el motivo de la cancelación." in the dialog

  @HU-15 @CA-HU15-01
  Scenario: The provider cancels a reservation giving the reason
    Given the provider has an appointment with the client tomorrow for "Corte"
    And the provider is logged in
    When the provider cancels the client's reservation with the reason "El profesional está incapacitado"
    Then they should see the notification "Reserva cancelada."
    And the provider should see the client's reservation in their agenda with the status "Cancelada por el proveedor"

  @HU-16 @CA-HU16-02
  Scenario: The provider reschedules a reservation to another slot
    Given the provider has an appointment with the client tomorrow for "Corte"
    And the provider is logged in
    When the provider reschedules the client's reservation to a slot in the following week
    Then they should see the notification "Reserva reprogramada."
    And the provider should see the client's reservation at a different time
