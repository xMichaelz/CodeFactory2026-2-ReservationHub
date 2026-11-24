# language: en
@access @permissions
Feature: Pages protected by session and by role
  As the platform owner
  I want each page to be visible only to those who have permission
  So that a client cannot manage businesses and a visitor cannot see other people's reservations

  @HU-03 @HU-04
  Scenario: Without a session, a client page redirects to login
    When someone without a session opens the page "/client/reservations"
    Then they should be on the page "/login"

  @HU-03 @HU-04
  Scenario: Without a session, a provider page redirects to login
    When someone without a session opens the page "/provider/services"
    Then they should be on the page "/login"

  @HU-03
  Scenario: A client cannot open the provider pages
    Given the client is logged in
    When they open the page "/provider/services" directly
    Then they should be on the page "/client"

  @HU-03
  Scenario: A provider cannot open the client pages
    Given the provider is logged in
    When they open the page "/client/catalog" directly
    Then they should be on the page "/provider"

  @HU-17 @CA-HU17-06
  Scenario: Without a session, the client history page redirects to login
    When someone without a session opens the page "/client/history"
    Then they should be on the page "/login"

  @HU-17 @CA-HU17-06
  Scenario: A provider cannot open the client history
    Given the provider is logged in
    When they open the page "/client/history" directly
    Then they should be on the page "/provider"

  @HU-18 @CA-HU18-05
  Scenario: A client cannot open the business history
    Given the client is logged in
    When they open the page "/provider/history" directly
    Then they should be on the page "/client"

  @HU-19 @HU-20 @HU-21 @CA-HU19-09 @CA-HU20-06 @CA-HU21-06
  Scenario: A client cannot open the provider reports
    Given the client is logged in
    When they open the page "/provider/reports" directly
    Then they should be on the page "/client"
