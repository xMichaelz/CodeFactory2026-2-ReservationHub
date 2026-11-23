package co.edu.udea.calidad.reservahub.models;

import co.edu.udea.calidad.reservahub.utils.TestData;

/** Datos de registro de un proveedor y su negocio (HU-02). */
public class ProviderAccount {
    public final String firstName;
    public final String lastName;
    public final String email;
    public final String password;
    public final String businessName;
    public final String serviceType;
    public final String address;

    private ProviderAccount(String firstName, String lastName, String email, String password,
            String businessName, String serviceType, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.businessName = businessName;
        this.serviceType = serviceType;
        this.address = address;
    }

    public static ProviderAccount random() {
        return new ProviderAccount("Paula", "Proveedora", TestData.emailFor("proveedor"), TestData.PASSWORD,
                "Barbería " + TestData.unique(), "Barbería", "Calle 10 # 20-30");
    }

    public ProviderAccount withBusinessName(String newBusinessName) {
        return new ProviderAccount(firstName, lastName, TestData.emailFor("proveedor"), password, newBusinessName,
                serviceType, address);
    }

    public ProviderAccount withPassword(String newPassword) {
        return new ProviderAccount(firstName, lastName, email, newPassword, businessName, serviceType, address);
    }
}
