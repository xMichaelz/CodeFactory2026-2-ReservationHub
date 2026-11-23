package co.edu.udea.calidad.reservahub.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.LOGIN_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.LOGOUT_BUTTON;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-04: cierra la sesión desde la barra superior (revoca el JWT). */
public class Logout implements Task {
    public static Logout now() {
        return Tasks.instrumented(Logout.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(LOGOUT_BUTTON),
                WaitUntil.the(LOGIN_BUTTON, isVisible()).forNoMoreThan(10).seconds());
    }
}
