# language: en
@access @login
Feature: Login and logout
  As a registered ReservaHub user
  I want to log in with my email and password
  So that I can manage my reservations or my business

  @HU-03 @CA-HU03-01
  Scenario: The client logs in and lands on their home page
    Given the client is registered
    When the client logs in
    Then they should be on the page "/client"
    And they should see the role "Cliente" in the top bar

  @HU-03 @CA-HU03-02
  Scenario: The provider logs in and lands on the business dashboard
    Given the provider is registered
    When the provider logs in
    Then they should be on the page "/provider"
    And they should see the title "Panel del negocio"
    And they should see the role "Proveedor" in the top bar

  @HU-03 @CA-HU03-03
  Scenario: Login is rejected with a wrong password
    Given the client is registered
    When the client tries to log in with the password "Incorrecta999"
    Then they should see the error message "Correo o contraseña incorrectos."

  @HU-03 @CA-HU03-03
  Scenario: Login is rejected with an unregistered email
    When someone tries to log in with the email "nadie@screenplay.test"
    Then they should see the error message "Correo o contraseña incorrectos."

  @HU-03 @CA-HU03-04 @CA-HU03-05
  Scenario: The account is locked after 5 failed attempts
    Given the client is registered
    When the client fails to log in 5 times
    And the client tries to log in with the password "Secreta123"
    Then they should see an error message starting with "Cuenta bloqueada por intentos fallidos"

  @HU-04 @CA-HU04-01 @CA-HU04-02
  Scenario: Logging out returns to the login page and protects private pages
    Given the client is logged in
    When they log out
    Then they should be on the page "/login"
    When they open the page "/client/reservations" directly
    Then they should be on the page "/login"

  @HU-03 @CA-HU03-07
  Scenario: A successful login resets the failed attempts counter
    Given the client is registered
    When the client fails to log in 4 times
    And the client logs in
    Then they should be on the page "/client"
    When they log out
    And the client tries to log in with the password "Incorrecta999"
    Then they should see the error message "Correo o contraseña incorrectos."
