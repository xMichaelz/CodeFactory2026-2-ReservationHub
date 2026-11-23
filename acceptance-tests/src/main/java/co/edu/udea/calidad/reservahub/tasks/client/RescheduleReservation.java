package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import net.serenitybdd.core.pages.WebElementFacade;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.DIALOG_SLOTS;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_ACTION;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_TIME;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.SUCCESS_TOAST;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-16: mueve una reserva a otro horario libre. El actor recuerda la hora anterior
 * (ActorMemory.value) y elige el primer hueco con una hora distinta.
 */
public class RescheduleReservation implements Task {
    private final String serviceName;

    public RescheduleReservation(String serviceName) {
        this.serviceName = serviceName;
    }

    public static RescheduleReservation of(String serviceName) {
        return Tasks.instrumented(RescheduleReservation.class, serviceName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String currentTime = TextOf.the(RESERVATION_TIME.of(serviceName)).answeredBy(actor);
        ActorMemory.rememberValue(actor, currentTime);

        actor.attemptsTo(
                Click.on(RESERVATION_ACTION.of(serviceName, "Reprogramar")),
                WaitUntil.the(DIALOG, isVisible()).forNoMoreThan(5).seconds(),
                WaitUntil.the(DIALOG_SLOTS, isVisible()).forNoMoreThan(10).seconds());

        WebElementFacade newSlot = DIALOG_SLOTS.resolveAllFor(actor).stream()
                .filter(slot -> !slot.getText().trim().equals(currentTime))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No hay otro horario libre para " + serviceName));
        newSlot.click();

        actor.attemptsTo(
                Pause.of(300),
                Click.on(DIALOG_BUTTON.of("Confirmar nuevo horario")),
                // La notificación indica que la API respondió y que la tabla se está recargando.
                WaitUntil.the(SUCCESS_TOAST, isVisible()).forNoMoreThan(10).seconds());
    }
}
