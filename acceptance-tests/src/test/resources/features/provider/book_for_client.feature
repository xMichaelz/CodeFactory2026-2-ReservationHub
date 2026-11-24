# language: en
@provider @book_for_client
Feature: Book on behalf of a client
  As a service provider
  I want to create reservations for clients who call me or visit me
  So that they also appear in the business agenda

  Background:
    Given the provider offers the service "Corte" with an all-week open schedule

  @HU-13 @CA-HU13-03
  Scenario: Looking up an email that does not belong to any client
    Given the provider is logged in
    When the provider looks up the client with the email "nadie-registrado@screenplay.test"
    Then they should see the error message "No se encontró un cliente activo con ese correo."

  @HU-13 @CA-HU13-01
  Scenario: Looking up a registered client shows their name
    Given the client is registered
    And the provider is logged in
    When the provider looks up the client by their email
    Then the provider should see that the client was found by their full name

  @HU-13 @CA-HU13-01
  Scenario: The reservation made for the client ends up in the agenda
    Given the client is registered
    And the provider is logged in
    When the provider books "Corte" on behalf of the client in a slot of the following week
    Then they should see the notification "Reserva creada para"
    And the provider should see the reservation created in the client's name
    When the provider goes to their agenda from the confirmation
    Then the provider should see the client's reservation in their agenda with the status "Confirmada"
