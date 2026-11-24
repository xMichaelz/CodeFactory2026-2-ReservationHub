# language: en
@provider @schedules
Feature: Opening hours and blocks
  As a service provider
  I want to define my weekly windows and block specific periods
  So that clients only book when I can serve them

  @HU-08 @CA-HU08-01
  Scenario: The provider adds a window and sees it on its day
    Given the provider is logged in
    When the provider adds a window on "Martes" from "08:00" to "12:00"
    Then they should see the notification "Franja del martes creada."
    And the provider should see the window from "08:00" to "12:00" on "Martes"

  @HU-08 @CA-HU08-03
  Scenario: A window that overlaps another one on the same day cannot be added
    Given the provider has a window on "Lunes" from "08:00" to "12:00"
    And the provider is logged in
    When the provider adds a window on "Lunes" from "10:00" to "14:00"
    Then they should see the error message "El horario se cruza con otra franja del mismo día."

  @HU-08 @CA-HU08-02
  Scenario: The window start time must be before the end time
    Given the provider is logged in
    When the provider adds a window on "Miércoles" from "14:00" to "10:00"
    Then they should see the error message "La hora de inicio debe ser anterior a la hora de fin."

  @HU-09 @CA-HU09-01
  Scenario: The provider blocks part of a window with a reason
    Given the provider has a window on "Lunes" from "08:00" to "12:00"
    And the provider is logged in
    When the provider blocks the window on "Lunes" from "09:00" to "10:00" for "Capacitación"
    Then they should see the notification "Bloqueo registrado."
    And the provider should see the block "Capacitación" in the window on "Lunes"

  @HU-09 @CA-HU09-03
  Scenario: The block must fall within the window's hours
    Given the provider has a window on "Lunes" from "08:00" to "12:00"
    And the provider is logged in
    When the provider blocks the window on "Lunes" from "13:00" to "14:00" for "Festivo"
    Then they should see the error message "El bloqueo debe estar dentro del horario de la franja." in the dialog
