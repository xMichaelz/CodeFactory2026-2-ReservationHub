package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.FREE_SLOTS;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isEnabled;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-11: en el selector de horarios pasa a la semana siguiente con "Siguiente →". */
public class ShowNextWeek implements Task {
    public static ShowNextWeek ofAvailability() {
        return Tasks.instrumented(ShowNextWeek.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(BUTTON.of("Siguiente →"), isEnabled()).forNoMoreThan(10).seconds(),
                Click.on(BUTTON.of("Siguiente →")),
                WaitUntil.the(BUTTON.of("Siguiente →"), isEnabled()).forNoMoreThan(10).seconds(),
                WaitUntil.the(FREE_SLOTS, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
    }
}
