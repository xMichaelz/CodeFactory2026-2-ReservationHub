package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.models.ResourceInfo;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;

import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.DESCRIPTION;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.QUANTITY;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.REGISTER_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.TYPE;

/** HU-10: registra un recurso con el formulario "Nuevo recurso". */
public class RegisterResource implements Task {
    private final ResourceInfo resource;
    private final String quantity;

    public RegisterResource(ResourceInfo resource, String quantity) {
        this.resource = resource;
        this.quantity = quantity;
    }

    public static RegisterResource with(ResourceInfo resource) {
        return Tasks.instrumented(RegisterResource.class, resource, String.valueOf(resource.quantity));
    }

    /** Escribe una cantidad concreta (también inválida, como "0"). */
    public static RegisterResource withQuantity(ResourceInfo resource, String quantity) {
        return Tasks.instrumented(RegisterResource.class, resource, quantity);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Enter.theValue(resource.name).into(NAME),
                Enter.theValue(resource.resourceType).into(TYPE),
                Enter.theValue(quantity).into(QUANTITY));
        if (!resource.description.isEmpty()) {
            actor.attemptsTo(Enter.theValue(resource.description).into(DESCRIPTION));
        }
        actor.attemptsTo(
                Pause.of(300),
                Click.on(REGISTER_BUTTON));
    }
}
