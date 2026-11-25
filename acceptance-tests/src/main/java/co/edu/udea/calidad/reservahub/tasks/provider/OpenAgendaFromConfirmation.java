package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.SEE_AGENDA_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsText;

/** HU-13: desde la confirmación de una reserva creada para un cliente, navega a la agenda. */
public class OpenAgendaFromConfirmation implements Task {
    public static OpenAgendaFromConfirmation now() {
        return Tasks.instrumented(OpenAgendaFromConfirmation.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(SEE_AGENDA_LINK),
                WaitUntil.the(PAGE_TITLE, containsText("Agenda de reservas")).forNoMoreThan(10).seconds(),
                WaitUntilLoaded.page());
    }
}
