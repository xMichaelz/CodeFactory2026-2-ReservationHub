package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.LINK_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.RESOURCE_CHECKBOX;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_ACTION;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Asocia un recurso a un servicio desde el botón "Recursos" de su fila. */
public class LinkResource implements Task {
    private final String resourceName;
    private final String serviceName;

    public LinkResource(String resourceName, String serviceName) {
        this.resourceName = resourceName;
        this.serviceName = serviceName;
    }

    public static LinkResource named(String resourceName, String serviceName) {
        return Tasks.instrumented(LinkResource.class, resourceName, serviceName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(SERVICE_ACTION.of(serviceName, "Recursos")),
                WaitUntil.the(RESOURCE_CHECKBOX.of(resourceName), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(RESOURCE_CHECKBOX.of(resourceName)),
                Pause.of(300),
                Click.on(LINK_BUTTON));
    }
}
