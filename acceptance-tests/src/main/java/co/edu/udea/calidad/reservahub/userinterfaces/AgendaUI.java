package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/agenda: tabla de reservas del negocio y modales de cancelar y reprogramar. */
public class AgendaUI {
    /** La columna "Cliente" muestra el nombre completo cuando termina de cargar el historial del negocio. */
    private static final String ROW = "//tr[@data-testid='reservation-row'][td[4][normalize-space()='{0}']]";

    public static final Target RESERVATION_ROW = Target.the("reserva de {0} en la agenda").locatedBy(ROW);
    public static final Target RESERVATION_STATUS = Target.the("estado de la reserva de {0}")
            .locatedBy(ROW + "/td[5]//span[contains(@class,'badge')]");
    public static final Target RESERVATION_ACTION = Target.the("botón {1} de la reserva de {0}")
            .locatedBy(ROW + "//button[normalize-space()='{1}']");

    // Modal "Cancelar reserva"
    public static final Target CANCEL_REASON = Target.the("motivo de la cancelación")
            .locatedBy("css:[role='dialog'] textarea[name='reason']");
    public static final Target CONFIRM_CANCEL_BUTTON = Target.the("botón confirmar cancelación")
            .locatedBy("//div[@role='dialog']//button[normalize-space()='Confirmar cancelación']");

    // Modal "Reprogramar reserva"
    public static final Target NEXT_WEEK_IN_DIALOG = Target.the("botón semana siguiente de la ventana")
            .locatedBy("//div[@role='dialog']//button[normalize-space()='Siguiente →']");
    public static final Target SLOT_IN_DIALOG = Target.the("horario libre de la ventana")
            .locatedBy("css:[role='dialog'] [data-testid='slot']");
    public static final Target CONFIRM_RESCHEDULE_BUTTON = Target.the("botón confirmar nuevo horario")
            .locatedBy("//div[@role='dialog']//button[normalize-space()='Confirmar nuevo horario']");

    private AgendaUI() { }
}
