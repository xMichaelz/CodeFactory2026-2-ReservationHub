package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_TAB;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-17: filtra el historial con una pestaña del ciclo de vida ("Pendiente", "Cancelada"...). */
public class FilterHistory implements Task {
    private final String lifecycle;

    public FilterHistory(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static FilterHistory by(String lifecycle) {
        return Tasks.instrumented(FilterHistory.class, lifecycle);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(HISTORY_TAB.of(lifecycle), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(HISTORY_TAB.of(lifecycle)),
                Pause.of(300));
    }
}
