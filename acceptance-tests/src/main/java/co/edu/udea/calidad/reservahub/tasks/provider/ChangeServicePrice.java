package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.PRICE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_ACTION;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-06: edita el precio de un servicio desde el botón "Editar" de su fila. */
public class ChangeServicePrice implements Task {
    private final String serviceName;
    private final int price;

    public ChangeServicePrice(String serviceName, Integer price) {
        this.serviceName = serviceName;
        this.price = price;
    }

    public static ChangeServicePrice of(String serviceName, int price) {
        return Tasks.instrumented(ChangeServicePrice.class, serviceName, price);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(SERVICE_ACTION.of(serviceName, "Editar")),
                WaitUntil.the(PRICE, isVisible()).forNoMoreThan(10).seconds(),
                Enter.theValue(String.valueOf(price)).into(PRICE),
                Pause.of(300),
                Click.on(DIALOG_BUTTON.of("Guardar cambios")));
    }
}
