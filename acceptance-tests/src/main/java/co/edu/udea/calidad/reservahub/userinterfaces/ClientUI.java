package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** Páginas del cliente: inicio, catálogo, reserva de un servicio, mis reservas e historial (HU-17). */
public class ClientUI {
    // Menú lateral: cada enlace lleva un ícono en un <span>, así que se compara solo su texto propio.
    public static final Target CLIENT_MENU_LINK = Target.the("opción del menú {0}")
            .locatedBy("//nav[@aria-label='Principal']//a[normalize-space(text())='{0}']");

    // Inicio (/client)
    public static final Target UPCOMING_APPOINTMENTS = Target.the("próximas citas")
            .locatedBy("//section[.//h2[normalize-space()='Próximas citas']]//li[contains(@class,'timeline__item')]");

    // Catálogo (/client/catalog)
    public static final Target CATALOG_SEARCH = Target.the("buscador del catálogo").locatedBy("css:input[name='search']");
    public static final Target CATALOG_PROVIDER_FILTER = Target.the("filtro por proveedor")
            .locatedBy("css:select[name='provider']");
    public static final Target CATALOG_PROVIDER_OPTION = Target.the("proveedor {0} en el filtro")
            .locatedBy("//select[@name='provider']/option[normalize-space()='{0}']");
    /** Una tarjeta del catálogo o el aviso vacío: lo que haya cuando el catálogo termina de cargar. */
    public static final Target CATALOG_RESULT = Target.the("resultado del catálogo")
            .locatedBy("//article[@data-testid='catalog-item'] | //h3[contains(@class,'empty-state__title')]");
    public static final Target CATALOG_SERVICE_NAMES = Target.the("nombres de los servicios del catálogo")
            .locatedBy("css:[data-testid='catalog-item'] .service-card__name");
    public static final Target CATALOG_PROVIDER_NAMES = Target.the("proveedores de los servicios del catálogo")
            .locatedBy("css:[data-testid='catalog-item'] .service-card__provider");
    public static final Target BOOK_SERVICE_LINK = Target.the("botón reservar del servicio {0}")
            .locatedBy("//article[@data-testid='catalog-item'][.//h3[normalize-space()='{0}']]"
                    + "//a[normalize-space()='Reservar']");

    // Reservar un servicio (/client/book/{id})
    public static final Target SLOT_PICKER = Target.the("selector de horarios").locatedBy("css:[data-testid='slot-picker']");
    public static final Target AVAILABILITY_RANGE = Target.the("semana de disponibilidad mostrada")
            .locatedBy("css:.slot-picker__range");
    public static final Target CONFIRMATION_DETAIL = Target.the("dato {0} de la confirmación")
            .locatedBy("//*[@data-testid='booking-confirmation']//dt[normalize-space()='{0}']/following-sibling::dd[1]");

    // Mis reservas (/client/reservations)
    public static final Target RESERVATIONS_TABLE = Target.the("tabla de mis reservas")
            .locatedBy("css:[data-testid='reservations-table']");
    public static final Target RESERVATION_TIME = Target.the("hora de la reserva de {0}")
            .locatedBy("//tr[@data-testid='reservation-row'][td[3][normalize-space()='{0}']]/td[2]");
    public static final Target RESERVATION_STATUS = Target.the("estado de la reserva de {0}")
            .locatedBy("//tr[@data-testid='reservation-row'][td[3][normalize-space()='{0}']]/td[5]");
    public static final Target RESERVATION_ACTION = Target.the("botón {1} de la reserva de {0}")
            .locatedBy("//tr[@data-testid='reservation-row'][td[3][normalize-space()='{0}']]"
                    + "//button[normalize-space()='{1}']");
    public static final Target DIALOG_SLOTS = Target.the("horarios libres de la ventana modal")
            .locatedBy("css:[role='dialog'] [data-testid='slot']");

    // Historial (/client/history)
    public static final Target HISTORY_TABLE = Target.the("tabla del historial").locatedBy("css:[data-testid='history-table']");
    public static final Target HISTORY_TAB = Target.the("pestaña {0} del historial")
            .locatedBy("//button[@role='tab' and normalize-space(text())='{0}']");
    public static final Target HISTORY_LIFECYCLE = Target.the("ciclo de vida de la reserva de {0}")
            .locatedBy("//tr[@data-testid='history-row'][td[3][normalize-space()='{0}']]/td[5]");
    public static final Target HISTORY_SERVICES = Target.the("servicios del historial")
            .locatedBy("//tr[@data-testid='history-row']/td[3]");
    public static final Target HISTORY_LIFECYCLES = Target.the("ciclos de vida del historial")
            .locatedBy("//tr[@data-testid='history-row']/td[5]");

    private ClientUI() { }
}
