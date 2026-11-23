package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.Reservation;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Patch;

import java.util.Map;

/** PATCH /reservations/{id}/cancel (HU-14 / HU-15). El motivo es obligatorio si cancela el proveedor. */
public class CancelReservationThroughApi implements Task {
    private final Reservation reservation;
    private final String reason;

    public CancelReservationThroughApi(Reservation reservation, String reason) {
        this.reservation = reservation;
        this.reason = reason;
    }

    public static CancelReservationThroughApi of(Reservation reservation) {
        return Tasks.instrumented(CancelReservationThroughApi.class, reservation, "Cancelada desde la API");
    }

    public static CancelReservationThroughApi of(Reservation reservation, String reason) {
        return Tasks.instrumented(CancelReservationThroughApi.class, reservation, reason);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Patch.to(Api.V1 + "/reservations/{id}/cancel").with(request -> Api
                .authenticated(actor, request)
                .pathParam("id", reservation.id)
                .body(Map.of("reason", reason))));
        Api.expect(200, "cancelar la reserva por API");
    }
}
