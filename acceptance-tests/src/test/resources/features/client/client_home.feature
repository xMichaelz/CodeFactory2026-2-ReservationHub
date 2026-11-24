# language: en
@client @client_home
Feature: Client home page
  As a ReservaHub client
  I want to see a summary of my upcoming appointments when I log in
  So that I know what I have booked without searching for it

  @HU-17
  Scenario: The home page shows the greeting and the next booked appointment
    Given the provider offers the service "Corte" with an all-week open schedule
    And the client is logged in
    And the client has a reservation for the service "Corte"
    When the client opens their home page
    Then the client should see their welcome greeting
    And the client should see the service "Corte" in their upcoming appointments
