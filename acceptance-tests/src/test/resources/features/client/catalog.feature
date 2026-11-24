# language: en
@client @catalog
Feature: Service catalog
  As a ReservaHub client
  I want to browse the active services of the providers
  So that I can find the one I want to book

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule
    And the client is logged in

  @HU-05
  Scenario: The catalog shows the provider's active service
    When the client opens the catalog
    Then they should see the title "Catálogo de servicios"
    And the catalog should show the service "Corte"

  @HU-05
  Scenario: Searching by service name finds it
    When the client searches the catalog for the service "Corte"
    Then the catalog should show the service "Corte"

  @HU-05
  Scenario: A search with no matches shows the empty notice
    When the client searches the catalog for the text "servicio-que-no-existe-zzz"
    Then they should see the empty-state notice "Sin resultados"

  @HU-07 @CA-HU07-01
  Scenario: A deactivated service does not appear in the catalog
    Given the provider has the service "Tinte"
    And the provider deactivated the service "Tinte"
    When the client searches the catalog for the provider's business
    Then the catalog should show the service "Corte"
    And the catalog should not show the service "Tinte"

  @HU-05
  Scenario: Filtering by provider shows only that business's services
    Given the provider has the service "Barba"
    When the client filters the catalog by the provider's business
    Then the catalog should only show services from the provider's business
    And the catalog should show the service "Corte"
    And the catalog should show the service "Barba"
