package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /services (HU-05). El proveedor recuerda el servicio con su id. */
public class CreateServiceThroughApi implements Task {
    private final ServiceInfo service;

    public CreateServiceThroughApi(ServiceInfo service) {
        this.service = service;
    }

    public static CreateServiceThroughApi with(ServiceInfo service) {
        return Tasks.instrumented(CreateServiceThroughApi.class, service);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/services").with(request -> Api.authenticated(actor, request)
                .body(Map.of(
                        "name", service.name,
                        "description", service.description,
                        "durationMinutes", service.durationMinutes,
                        "price", service.price))));
        String id = Api.expect(201, "crear el servicio por API").jsonPath().getString("id");
        ActorMemory.rememberService(actor, service.withId(id));
    }
}
