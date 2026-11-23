package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_ACTION;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-07: desactiva un servicio y lo confirma en la ventana "Desactivar servicio". */
public class DeactivateService implements Task {
    private final String serviceName;

    public DeactivateService(String serviceName) {
        this.serviceName = serviceName;
    }

    public static DeactivateService called(String serviceName) {
        return Tasks.instrumented(DeactivateService.class, serviceName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(SERVICE_ACTION.of(serviceName, "Desactivar")),
                WaitUntil.the(DIALOG, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300),
                Click.on(DIALOG_BUTTON.of("Desactivar")));
    }
}
