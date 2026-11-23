package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** Elementos presentes en varias páginas: encabezados, alertas, notificaciones, menú y modales. */
public class CommonUI {
    // Encabezados
    public static final Target PAGE_TITLE = Target.the("título de la página").locatedBy("css:.page-header__title");
    public static final Target AUTH_TITLE = Target.the("título del formulario").locatedBy("css:.auth-card__title");
    public static final Target EMPTY_STATE_TITLE = Target.the("título del estado vacío")
            .locatedBy("css:.empty-state__title");

    // Mensajes
    public static final Target ERROR_MESSAGE = Target.the("mensaje de error").locatedBy("css:[data-testid='alert-error']");
    public static final Target SUCCESS_MESSAGE = Target.the("mensaje de éxito")
            .locatedBy("css:[data-testid='alert-success']");
    public static final Target WARNING_MESSAGE = Target.the("aviso").locatedBy("css:[data-testid='alert-warning']");
    public static final Target SUCCESS_TOAST = Target.the("notificación de éxito")
            .locatedBy("css:[data-testid='toast-success']");
    public static final Target FIELD_ERROR = Target.the("error de validación").locatedBy("css:.field__error");

    // Barra superior y menú lateral
    public static final Target USER_CHIP = Target.the("usuario en sesión").locatedBy("css:[data-testid='user-chip']");
    public static final Target USER_ROLE = Target.the("rol del usuario").locatedBy("css:.user-chip__role");
    public static final Target LOGOUT_BUTTON = Target.the("botón cerrar sesión")
            .locatedBy("//button[normalize-space()='Cerrar sesión']");
    public static final Target MENU_LINK = Target.the("opción del menú {0}")
            .locatedBy("//nav[@aria-label='Principal']//a[normalize-space(text())='{0}']");

    // Genéricos por texto o atributo name
    public static final Target BUTTON = Target.the("botón {0}").locatedBy("//button[normalize-space()='{0}']");
    public static final Target LINK = Target.the("enlace {0}").locatedBy("//a[normalize-space()='{0}']");
    public static final Target FIELD = Target.the("campo {0}").locatedBy("css:[name='{0}']");

    // Modales
    public static final Target DIALOG = Target.the("ventana modal").locatedBy("css:[role='dialog']");
    public static final Target DIALOG_TITLE = Target.the("título de la ventana modal")
            .locatedBy("css:[role='dialog'] .modal__title");
    public static final Target DIALOG_FIELD = Target.the("campo {0} de la ventana modal")
            .locatedBy("css:[role='dialog'] [name='{0}']");
    public static final Target DIALOG_BUTTON = Target.the("botón {0} de la ventana modal")
            .locatedBy("//div[@role='dialog']//button[normalize-space()='{0}']");
    public static final Target DIALOG_ERROR = Target.the("error de la ventana modal")
            .locatedBy("css:[role='dialog'] [data-testid='alert-error']");

    // Selector de horarios libres (reserva, reprogramación, reserva para cliente)
    public static final Target FREE_SLOTS = Target.the("horarios libres").locatedBy("css:[data-testid='slot']");
    public static final Target SELECTED_SLOT = Target.the("horario seleccionado")
            .locatedBy("css:[data-testid='slot'][aria-pressed='true']");
    public static final Target BOOKING_CONFIRMATION = Target.the("confirmación de la reserva")
            .locatedBy("css:[data-testid='booking-confirmation']");

    // Filtro de fechas (agenda, historial, reportes)
    public static final Target RANGE_FROM = Target.the("fecha desde").locatedBy("css:input[name='from']");
    public static final Target RANGE_TO = Target.the("fecha hasta").locatedBy("css:input[name='to']");
    public static final Target RANGE_PRESET = Target.the("atajo de fechas {0}")
            .locatedBy("//button[contains(@class,'chip') and normalize-space()='{0}']");

    private CommonUI() { }
}
