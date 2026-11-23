package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_ACTION;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_FIELD;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.SUCCESS_TOAST;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-14: el cliente cancela una de sus reservas desde "Mis reservas"; el motivo es opcional. */
public class CancelReservation implements Task {
    private final String serviceName;
    private final String reason;

    public CancelReservation(String serviceName, String reason) {
        this.serviceName = serviceName;
        this.reason = reason;
    }

    public static CancelReservation of(String serviceName) {
        return Tasks.instrumented(CancelReservation.class, serviceName, "");
    }

    public static CancelReservation of(String serviceName, String reason) {
        return Tasks.instrumented(CancelReservation.class, serviceName, reason);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(RESERVATION_ACTION.of(serviceName, "Cancelar"), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(RESERVATION_ACTION.of(serviceName, "Cancelar")),
                WaitUntil.the(DIALOG, isVisible()).forNoMoreThan(5).seconds());
        if (!reason.isEmpty()) {
            actor.attemptsTo(Enter.theValue(reason).into(DIALOG_FIELD.of("reason")));
        }
        actor.attemptsTo(
                Pause.of(300),
                Click.on(DIALOG_BUTTON.of("Confirmar cancelación")),
                // La notificación indica que la API respondió y que la tabla se está recargando.
                WaitUntil.the(SUCCESS_TOAST, isVisible()).forNoMoreThan(10).seconds());
    }
}
