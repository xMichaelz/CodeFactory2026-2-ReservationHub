# language: en
@access @registration @client_registration
Feature: Client registration
  As a person who wants to book services
  I want to create my client account
  So that I can book from the ReservaHub catalog

  @HU-01 @CA-HU01-01
  Scenario: Successful registration lands on the catalog with the session started
    When a new client registers with their details
    Then they should be on the page "/client/catalog"
    And they should see the notification "Cuenta creada"
    And they should see the role "Cliente" in the top bar

  @HU-01 @CA-HU01-03
  Scenario: Registration is rejected with a short password
    When a new client registers with the password "abc12"
    Then they should see the error message "La contraseña debe tener al menos 8 caracteres."

  @HU-01 @CA-HU01-03
  Scenario: Registration is rejected with a password without numbers
    When a new client registers with the password "SoloLetras"
    Then they should see the error message "La contraseña debe contener al menos una letra y un número."

  @HU-01
  Scenario: Registration is rejected if the passwords do not match
    When a new client registers with a different password confirmation
    Then they should see the error message "Las contraseñas no coinciden."

  @HU-01 @CA-HU01-02
  Scenario: Registration is rejected with an already registered email
    Given the client is registered
    When another person tries to register with the client's email
    Then they should see the error message "Ese correo ya está registrado."
