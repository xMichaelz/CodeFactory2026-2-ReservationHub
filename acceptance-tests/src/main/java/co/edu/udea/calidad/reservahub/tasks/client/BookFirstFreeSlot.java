package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.BOOKING_CONFIRMATION;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.FREE_SLOTS;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.SELECTED_SLOT;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-12: con la disponibilidad abierta, elige el primer horario libre y confirma la reserva. */
public class BookFirstFreeSlot implements Task {
    public static BookFirstFreeSlot now() {
        return Tasks.instrumented(BookFirstFreeSlot.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(FREE_SLOTS, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(FREE_SLOTS),
                WaitUntil.the(SELECTED_SLOT, isVisible()).forNoMoreThan(5).seconds(),
                Pause.of(300),
                Click.on(BUTTON.of("Confirmar reserva")),
                WaitUntil.the(BOOKING_CONFIRMATION, isVisible()).forNoMoreThan(10).seconds());
    }
}
