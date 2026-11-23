package co.edu.udea.calidad.reservahub.tasks;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.MENU_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Usa el menú lateral, como lo haría una persona: NavigateTo.section("Servicios"). */
public class NavigateTo implements Task {
    private final String section;

    public NavigateTo(String section) {
        this.section = section;
    }

    public static NavigateTo section(String section) {
        return Tasks.instrumented(NavigateTo.class, section);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(MENU_LINK.of(section)),
                WaitUntil.the(PAGE_TITLE, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
    }
}
