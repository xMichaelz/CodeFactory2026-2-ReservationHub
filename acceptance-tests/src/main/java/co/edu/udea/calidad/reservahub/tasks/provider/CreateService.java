package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.DESCRIPTION;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.DURATION;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.NEW_SERVICE_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.PRICE;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-05: crea un servicio con el modal "Nuevo servicio" de la sección Servicios. */
public class CreateService implements Task {
    private final ServiceInfo service;

    public CreateService(ServiceInfo service) {
        this.service = service;
    }

    public static CreateService with(ServiceInfo service) {
        return Tasks.instrumented(CreateService.class, service);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(NEW_SERVICE_BUTTON),
                WaitUntil.the(NAME, isVisible()).forNoMoreThan(10).seconds(),
                Enter.theValue(service.name).into(NAME),
                Enter.theValue(service.description).into(DESCRIPTION),
                Enter.theValue(String.valueOf(service.durationMinutes)).into(DURATION),
                Enter.theValue(String.valueOf(service.price)).into(PRICE),
                Pause.of(300),
                Click.on(DIALOG_BUTTON.of("Crear servicio")));
    }
}
