package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_RESULT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CLIENT_MENU_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_TABLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATIONS_TABLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Usa el menú lateral del cliente: OpenClientSection.called("Mis reservas") o los atajos por sección. */
public class OpenClientSection implements Task {
    private final String section;
    private final Target readyMarker;

    public OpenClientSection(String section, Target readyMarker) {
        this.section = section;
        this.readyMarker = readyMarker;
    }

    public static OpenClientSection called(String section) {
        return Tasks.instrumented(OpenClientSection.class, section, null);
    }

    public static OpenClientSection catalog() {
        return Tasks.instrumented(OpenClientSection.class, "Catálogo", CATALOG_RESULT);
    }

    public static OpenClientSection reservations() {
        return Tasks.instrumented(OpenClientSection.class, "Mis reservas", RESERVATIONS_TABLE);
    }

    public static OpenClientSection history() {
        return Tasks.instrumented(OpenClientSection.class, "Historial", HISTORY_TABLE);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(CLIENT_MENU_LINK.of(section), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(CLIENT_MENU_LINK.of(section)),
                WaitUntil.the(PAGE_TITLE, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
        if (readyMarker != null) {
            actor.attemptsTo(WaitUntil.the(readyMarker, isVisible()).forNoMoreThan(10).seconds());
        }
    }
}
