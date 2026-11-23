package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.Reservation;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.TestData;
import io.restassured.response.Response;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Get;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * El cliente consulta la disponibilidad (HU-11) y reserva un hueco libre (HU-12) por API.
 * {@code skip} permite elegir otro hueco que no sea el primero (p. ej. para tener dos reservas).
 */
public class BookFreeSlotThroughApi implements Task {
    private final ServiceInfo service;
    private final int skip;

    public BookFreeSlotThroughApi(ServiceInfo service, Integer skip) {
        this.service = service;
        this.skip = skip;
    }

    public static BookFreeSlotThroughApi of(ServiceInfo service) {
        return Tasks.instrumented(BookFreeSlotThroughApi.class, service, 0);
    }

    public static BookFreeSlotThroughApi skipping(int slots, ServiceInfo service) {
        return Tasks.instrumented(BookFreeSlotThroughApi.class, service, slots);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        LocalDate today = TestData.today();
        actor.attemptsTo(Get.resource(Api.V1 + "/services/{id}/availability").with(request -> Api
                .authenticated(actor, request)
                .pathParam("id", service.id)
                .queryParam("from", today.toString())
                .queryParam("to", today.plusDays(6).toString())));
        Response availability = Api.expect(200, "consultar la disponibilidad por API");

        List<String[]> free = new ArrayList<>();
        List<Map<String, Object>> days = availability.jsonPath().getList("days");
        for (Map<String, Object> day : days) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> slots = (List<Map<String, String>>) day.get("slots");
            for (Map<String, String> slot : slots) {
                free.add(new String[] {(String) day.get("date"), slot.get("start")});
            }
        }
        if (free.size() <= skip) {
            throw new AssertionError("El servicio " + service.name + " no tiene huecos libres suficientes");
        }
        String[] chosen = free.get(skip);

        actor.attemptsTo(Post.to(Api.V1 + "/reservations").with(request -> Api.authenticated(actor, request)
                .body(Map.of("serviceId", service.id, "date", chosen[0], "startTime", chosen[1]))));
        Response created = Api.expect(201, "reservar por API");
        ActorMemory.rememberReservation(actor, new Reservation(created.jsonPath().getString("id"), service.name,
                created.jsonPath().getString("date"), created.jsonPath().getString("startTime")));
    }
}
