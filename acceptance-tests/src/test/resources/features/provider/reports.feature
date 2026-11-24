# language: en
@provider @reports
Feature: Business reports
  As a service provider
  I want to see the occupancy, demand and cancellations of my agenda
  So that I can make decisions about my schedules and services

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule

  @HU-19 @CA-HU19-01
  Scenario: The occupancy report shows the service
    Given the client has a reservation for the service "Corte"
    And the provider is logged in
    When the provider views the "Ocupación" report
    Then the provider should see the service "Corte" in the card "Por servicio"

  @HU-20 @CA-HU20-01
  Scenario: The demand report counts the confirmed reservations
    Given the client has a reservation for the service "Corte"
    And another client has a reservation for the service "Corte"
    And the provider is logged in
    When the provider views the "Demanda" report
    Then the provider should see the statistic "Reservas confirmadas" with the value "2"
    And the provider should see for the service "Corte" the value "2" in the card "Reservas por servicio"

  @HU-21 @CA-HU21-01
  Scenario: The cancellations report separates the client's and the provider's cancellations
    Given the client has a reservation for the service "Corte"
    And the client cancelled that reservation
    And another client has a reservation for the service "Corte"
    And the provider cancelled the other client's reservation
    And the provider is logged in
    When the provider views the "Cancelaciones" report
    Then the provider should see the statistic "Total cancelaciones" with the value "2"
    And the provider should see the statistic "Por el cliente" with the value "1"
    And the provider should see the statistic "Por el proveedor" with the value "1"
    And the provider should see for the service "Corte" the value "1 / 1" in the card "Cancelaciones por servicio"

  @HU-21 @CA-HU21-02
  Scenario: Cancellations can be filtered by service
    Given the provider offers the service "Tinte" with an all-week open schedule
    And the client has a reservation for the service "Corte"
    And the client cancelled that reservation
    And another client has a reservation for the service "Tinte"
    And the provider cancelled the other client's reservation
    And the provider is logged in
    When the provider views the "Cancelaciones" report for the service "Corte" only
    Then the provider should see the statistic "Total cancelaciones" with the value "1"
    And the provider should see the statistic "Por el cliente" with the value "1"
    And the provider should see the statistic "Por el proveedor" with the value "0"
    And the provider should not see the service "Tinte" in the card "Cancelaciones por servicio"

  @HU-19 @HU-20 @HU-21 @CA-HU19-07 @CA-HU20-05 @CA-HU21-05
  Scenario: The range of the reports cannot exceed 31 days
    Given the provider is logged in
    When the provider views the reports for the next 40 days
    Then they should see the validation error "El rango no puede superar 31 días."
