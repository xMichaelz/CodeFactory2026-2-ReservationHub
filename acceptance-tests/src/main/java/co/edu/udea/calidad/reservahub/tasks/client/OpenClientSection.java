package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CLIENT_MENU_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Usa el menú lateral del cliente: OpenClientSection.called("Mis reservas"). */
public class OpenClientSection implements Task {
    private final String section;

    public OpenClientSection(String section) {
        this.section = section;
    }

    public static OpenClientSection called(String section) {
        return Tasks.instrumented(OpenClientSection.class, section);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(CLIENT_MENU_LINK.of(section), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(CLIENT_MENU_LINK.of(section)),
                WaitUntil.the(PAGE_TITLE, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
    }
}
