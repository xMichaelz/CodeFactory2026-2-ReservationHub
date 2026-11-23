package co.edu.udea.calidad.reservahub.models;

import co.edu.udea.calidad.reservahub.utils.TestData;

/** Datos de registro de un cliente (HU-01). */
public class ClientAccount {
    public final String firstName;
    public final String lastName;
    public final String email;
    public final String phone;
    public final String password;

    private ClientAccount(String firstName, String lastName, String email, String phone, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public static ClientAccount random() {
        return new ClientAccount("Carlos", "Cliente", TestData.emailFor("cliente"), "3001234567", TestData.PASSWORD);
    }

    public static ClientAccount named(String firstName, String lastName) {
        return new ClientAccount(firstName, lastName, TestData.emailFor("cliente"), "3001234567", TestData.PASSWORD);
    }

    public ClientAccount withPassword(String newPassword) {
        return new ClientAccount(firstName, lastName, email, phone, newPassword);
    }

    public ClientAccount withEmail(String newEmail) {
        return new ClientAccount(firstName, lastName, newEmail, phone, password);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
