package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** Login y registro de clientes y proveedores. */
public class AccessUI {
    // Login
    public static final Target EMAIL = Target.the("campo correo").locatedBy("css:input[name='email']");
    public static final Target PASSWORD = Target.the("campo contraseña").locatedBy("css:input[name='password']");
    public static final Target LOGIN_BUTTON = Target.the("botón entrar")
            .locatedBy("//button[@type='submit' and normalize-space()='Entrar']");

    // Registro (cliente y proveedor)
    public static final Target FIRST_NAME = Target.the("nombre").locatedBy("css:input[name='firstName']");
    public static final Target LAST_NAME = Target.the("apellido").locatedBy("css:input[name='lastName']");
    public static final Target PHONE = Target.the("teléfono").locatedBy("css:input[name='phone']");
    public static final Target CONFIRM_PASSWORD = Target.the("confirmar contraseña").locatedBy("css:input[name='confirm']");
    public static final Target CREATE_ACCOUNT_BUTTON = Target.the("botón crear cuenta")
            .locatedBy("//button[@type='submit' and normalize-space()='Crear cuenta']");

    // Registro de proveedor
    public static final Target BUSINESS_NAME = Target.the("nombre del negocio").locatedBy("css:input[name='businessName']");
    public static final Target SERVICE_TYPE = Target.the("tipo de servicio").locatedBy("css:input[name='serviceType']");
    public static final Target ADDRESS = Target.the("dirección").locatedBy("css:input[name='address']");
    public static final Target REGISTER_BUSINESS_BUTTON = Target.the("botón registrar negocio")
            .locatedBy("//button[@type='submit' and normalize-space()='Registrar negocio']");

    private AccessUI() { }
}
