package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.Reservation;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.restassured.response.Response;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Get;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.List;
import java.util.Map;

/**
 * El cliente reserva por API el primer hueco libre de una fecha concreta (HU-11 + HU-12).
 * Sirve cuando la reserva debe seguir siendo futura durante todo el escenario (p. ej. mañana),
 * porque el primer hueco de hoy puede empezar dentro de pocos minutos.
 */
public class BookSlotOnDateThroughApi implements Task {
    private final ServiceInfo service;
    private final String date;

    public BookSlotOnDateThroughApi(ServiceInfo service, String date) {
        this.service = service;
        this.date = date;
    }

    /** {@code date} en formato ISO (YYYY-MM-DD), en la zona del negocio. */
    public static BookSlotOnDateThroughApi of(ServiceInfo service, String date) {
        return Tasks.instrumented(BookSlotOnDateThroughApi.class, service, date);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Get.resource(Api.V1 + "/services/{id}/availability").with(request -> Api
                .authenticated(actor, request)
                .pathParam("id", service.id)
                .queryParam("from", date)
                .queryParam("to", date)));
        Response availability = Api.expect(200, "consultar la disponibilidad por API");

        List<String> starts = availability.jsonPath().getList("days[0].slots.start");
        if (starts == null || starts.isEmpty()) {
            throw new AssertionError("El servicio " + service.name + " no tiene huecos libres el " + date);
        }

        actor.attemptsTo(Post.to(Api.V1 + "/reservations").with(request -> Api.authenticated(actor, request)
                .body(Map.of("serviceId", service.id, "date", date, "startTime", starts.get(0)))));
        Response created = Api.expect(201, "reservar por API");
        ActorMemory.rememberReservation(actor, new Reservation(created.jsonPath().getString("id"), service.name,
                created.jsonPath().getString("date"), created.jsonPath().getString("startTime")));
    }
}
