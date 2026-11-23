package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.ResourceInfo;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /resources (HU-10). */
public class CreateResourceThroughApi implements Task {
    private final ResourceInfo resource;

    public CreateResourceThroughApi(ResourceInfo resource) {
        this.resource = resource;
    }

    public static CreateResourceThroughApi with(ResourceInfo resource) {
        return Tasks.instrumented(CreateResourceThroughApi.class, resource);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/resources").with(request -> Api.authenticated(actor, request)
                .body(Map.of(
                        "name", resource.name,
                        "resourceType", resource.resourceType,
                        "quantity", resource.quantity,
                        "description", resource.description))));
        Api.expect(201, "crear el recurso por API");
    }
}
