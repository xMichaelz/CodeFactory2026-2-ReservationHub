package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Patch;

/** PATCH /services/{id}/deactivate (HU-07). */
public class DeactivateServiceThroughApi implements Task {
    private final ServiceInfo service;

    public DeactivateServiceThroughApi(ServiceInfo service) {
        this.service = service;
    }

    public static DeactivateServiceThroughApi of(ServiceInfo service) {
        return Tasks.instrumented(DeactivateServiceThroughApi.class, service);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Patch.to(Api.V1 + "/services/{id}/deactivate").with(request -> Api
                .authenticated(actor, request)
                .pathParam("id", service.id)));
        Api.expect(200, "desactivar el servicio por API");
    }
}
