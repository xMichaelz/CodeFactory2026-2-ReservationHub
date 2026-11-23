package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/services: tabla de servicios, formulario (modal), asociación de recursos y desactivación. */
public class ServicesUI {
    private static final String ROW = "//table[@data-testid='services-table']//tr[.//strong[normalize-space()='{0}']]";

    public static final Target NEW_SERVICE_BUTTON = Target.the("botón nuevo servicio")
            .locatedBy("//button[normalize-space()='+ Nuevo servicio']");

    // Formulario del modal "Nuevo servicio" / "Editar servicio"
    public static final Target NAME = Target.the("nombre del servicio").locatedBy("css:[role='dialog'] [name='name']");
    public static final Target DESCRIPTION = Target.the("descripción del servicio")
            .locatedBy("css:[role='dialog'] [name='description']");
    public static final Target DURATION = Target.the("duración del servicio")
            .locatedBy("css:[role='dialog'] [name='durationMinutes']");
    public static final Target PRICE = Target.the("precio del servicio").locatedBy("css:[role='dialog'] [name='price']");

    // Fila de un servicio
    public static final Target SERVICE_ROW = Target.the("fila del servicio {0}").locatedBy(ROW);
    public static final Target SERVICE_PRICE = Target.the("precio del servicio {0}").locatedBy(ROW + "/td[3]");
    public static final Target SERVICE_RESOURCES = Target.the("recursos del servicio {0}").locatedBy(ROW + "/td[4]");
    public static final Target SERVICE_STATUS = Target.the("estado del servicio {0}")
            .locatedBy(ROW + "/td[5]//span[contains(@class,'badge')]");
    public static final Target SERVICE_ACTION = Target.the("botón {1} del servicio {0}")
            .locatedBy(ROW + "//button[normalize-space()='{1}']");

    // Modal "Recursos de ..."
    public static final Target RESOURCE_CHECKBOX = Target.the("casilla del recurso {0}")
            .locatedBy("//div[@role='dialog']//label[.//strong[normalize-space()='{0}']]//input[@type='checkbox']");
    public static final Target LINK_BUTTON = Target.the("botón asociar")
            .locatedBy("//div[@role='dialog']//button[starts-with(normalize-space(),'Asociar')]");

    private ServicesUI() { }
}
