package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.BOOK_SERVICE_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.SLOT_PICKER;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.FREE_SLOTS;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-11: busca el servicio en el catálogo, pulsa "Reservar" y espera los horarios libres. */
public class StartBooking implements Task {
    private final String serviceName;

    public StartBooking(String serviceName) {
        this.serviceName = serviceName;
    }

    public static StartBooking of(String serviceName) {
        return Tasks.instrumented(StartBooking.class, serviceName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                SearchCatalog.forText(serviceName),
                WaitUntil.the(BOOK_SERVICE_LINK.of(serviceName), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(BOOK_SERVICE_LINK.of(serviceName)),
                WaitUntil.the(SLOT_PICKER, isVisible()).forNoMoreThan(10).seconds(),
                WaitUntil.the(FREE_SLOTS, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
    }
}
