package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/resources: formulario "Nuevo recurso" y tabla "Mis recursos". */
public class ResourcesUI {
    public static final Target NAME = Target.the("nombre del recurso").locatedBy("css:input[name='name']");
    public static final Target TYPE = Target.the("tipo de recurso").locatedBy("css:input[name='resourceType']");
    public static final Target QUANTITY = Target.the("cantidad del recurso").locatedBy("css:input[name='quantity']");
    public static final Target DESCRIPTION = Target.the("descripción del recurso")
            .locatedBy("css:textarea[name='description']");
    public static final Target REGISTER_BUTTON = Target.the("botón registrar recurso")
            .locatedBy("//button[@type='submit' and normalize-space()='Registrar recurso']");

    public static final Target RESOURCE_ROW = Target.the("recurso {0} en Mis recursos")
            .locatedBy("//section[.//h2[normalize-space()='Mis recursos']]"
                    + "//table[@data-testid='resources-table']//tr[.//strong[normalize-space()='{0}']]");

    private ResourcesUI() { }
}
