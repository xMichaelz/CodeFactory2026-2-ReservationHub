package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.CONFIRM_RESCHEDULE_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.NEXT_WEEK_IN_DIALOG;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.RESERVATION_ACTION;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.SLOT_IN_DIALOG;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isEnabled;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-16: el proveedor mueve la reserva de un cliente al primer hueco libre de la semana siguiente
 * (así el nuevo horario es otro día y no corre el riesgo de quedar en el pasado).
 */
public class RescheduleReservation implements Task {
    private final String clientName;

    public RescheduleReservation(String clientName) {
        this.clientName = clientName;
    }

    public static RescheduleReservation ofClient(String clientName) {
        return Tasks.instrumented(RescheduleReservation.class, clientName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(RESERVATION_ACTION.of(clientName, "Reprogramar"), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(RESERVATION_ACTION.of(clientName, "Reprogramar")),
                WaitUntil.the(NEXT_WEEK_IN_DIALOG, isEnabled()).forNoMoreThan(10).seconds(),
                Click.on(NEXT_WEEK_IN_DIALOG),
                WaitUntil.the(SLOT_IN_DIALOG, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(SLOT_IN_DIALOG),
                Pause.of(300),
                Click.on(CONFIRM_RESCHEDULE_BUTTON));
    }
}
