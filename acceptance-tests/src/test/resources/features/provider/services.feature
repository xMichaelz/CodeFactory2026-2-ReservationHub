# language: en
@provider @services
Feature: Provider service catalog
  As a service provider
  I want to create, edit and deactivate my services and link resources to them
  So that I control what clients can book

  @HU-05 @CA-HU05-01
  Scenario: The provider creates a service and it is active
    Given the provider is logged in
    When the provider creates the service "Manicure" from the Services section
    Then they should see the notification "creado."
    And the provider should see the service "Manicure" with the status "Activo"

  @HU-06 @CA-HU06-01
  Scenario: The provider changes the price of a service
    Given the provider has the service "Corte"
    And the provider is logged in
    When the provider changes the price of the service "Corte" to 40000
    Then they should see the notification "actualizado."
    And the provider should see the service "Corte" with a price of "40.000"

  @HU-05
  Scenario: A service with an already existing name cannot be created
    Given the provider has the service "Corte"
    And the provider is logged in
    When the provider tries to create another service with the same name as "Corte"
    Then they should see the error message "Ya tienes un servicio con ese nombre." in the dialog

  @HU-07 @CA-HU07-01
  Scenario: The provider deactivates a service
    Given the provider has the service "Corte"
    And the provider is logged in
    When the provider deactivates the service "Corte"
    Then they should see the notification "quedó inactivo."
    And the provider should see the service "Corte" with the status "Inactivo"

  @HU-10 @CA-HU10-05
  Scenario: The provider links a resource to a service
    Given the provider has the service "Corte"
    And the provider has the resource "Silla"
    And the provider is logged in
    When the provider links the resource "Silla" to the service "Corte"
    Then they should see the notification "Recursos asociados."
    And the provider should see the resource "Silla" in the row of the service "Corte"
