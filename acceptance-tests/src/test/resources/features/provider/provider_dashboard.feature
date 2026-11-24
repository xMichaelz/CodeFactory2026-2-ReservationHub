# language: en
@provider @provider_dashboard
Feature: Business dashboard
  As a service provider
  I want to see the setup status of my business
  So that I know what is missing before receiving reservations

  @HU-05 @HU-08
  Scenario: A new provider sees the pending setup checklist
    Given the provider is logged in
    Then the provider should see the card "Completa tu configuración"
    And the provider should see the step "Crea al menos un servicio activo" as pending
    And the provider should see the step "Define tus horarios de atención" as pending
    And the provider should see the statistic "Servicios activos" with the value "0"

  @HU-05 @HU-08
  Scenario: With a service and schedules the dashboard counts one active service
    Given the provider offers the service "Corte" with an all-week open schedule
    And the provider is logged in
    Then the provider should see the statistic "Servicios activos" with the value "1"
    And the provider should see the statistic "Franjas de atención" with the value "7"
