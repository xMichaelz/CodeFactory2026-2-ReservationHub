# language: en
@provider @resources
Feature: Provider resources
  As a service provider
  I want to register the equipment, rooms or professionals of my business
  So that I can link them to the services that need them

  @HU-10 @CA-HU10-01
  Scenario: The provider registers a resource and it appears in their list
    Given the provider is logged in
    When the provider registers the resource "Sala"
    Then they should see the notification "registrado."
    And the provider should see the resource "Sala" in My resources

  @HU-10
  Scenario: A resource with an already existing name cannot be registered
    Given the provider has the resource "Silla"
    And the provider is logged in
    When the provider tries to register another resource with the same name as "Silla"
    Then they should see the error message "Ya tienes un recurso con ese nombre."

  @HU-10
  Scenario: The browser does not allow registering a resource with quantity 0
    Given the provider is logged in
    When the provider tries to register the resource "Sala" with quantity "0"
    Then the browser should not allow submitting a quantity lower than 1
    And they should see the empty-state notice "Aún no tienes recursos"
