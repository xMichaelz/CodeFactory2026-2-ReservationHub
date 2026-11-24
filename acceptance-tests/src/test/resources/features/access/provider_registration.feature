# language: en
@access @registration @provider_registration
Feature: Provider registration
  As the owner of a service business
  I want to register my business in ReservaHub
  So that I can publish my services and receive reservations

  @HU-02 @CA-HU02-01
  Scenario: Successful registration lands on the business dashboard
    When a new provider registers their business
    Then they should be on the page "/provider"
    And they should see the title "Panel del negocio"
    And they should see the notification "Negocio registrado"

  @HU-02
  Scenario: Registration is rejected with an already registered business name
    Given the provider is registered
    When another provider tries to register a business with the same name
    Then they should see the error message "Ese nombre de negocio ya está registrado."
