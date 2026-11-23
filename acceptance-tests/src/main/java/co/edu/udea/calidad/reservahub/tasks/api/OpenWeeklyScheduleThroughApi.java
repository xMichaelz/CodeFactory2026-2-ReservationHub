package co.edu.udea.calidad.reservahub.tasks.api;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.List;
import java.util.Map;

/**
 * POST /schedules (HU-08) para los 7 días. Por defecto de 00:00 a 23:30, así siempre hay huecos
 * libres en los próximos días sin importar la hora a la que corran las pruebas.
 */
public class OpenWeeklyScheduleThroughApi implements Task {
    private static final List<String> DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private final String startTime;
    private final String endTime;

    public OpenWeeklyScheduleThroughApi(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static OpenWeeklyScheduleThroughApi allDayLong() {
        return Tasks.instrumented(OpenWeeklyScheduleThroughApi.class, "00:00:00", "23:30:00");
    }

    public static OpenWeeklyScheduleThroughApi from(String startTime, String endTime) {
        return Tasks.instrumented(OpenWeeklyScheduleThroughApi.class, startTime + ":00", endTime + ":00");
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        for (String day : DAYS) {
            actor.attemptsTo(Post.to(Api.V1 + "/schedules").with(request -> Api.authenticated(actor, request)
                    .body(Map.of("dayOfWeek", day, "startTime", startTime, "endTime", endTime))));
            Api.expect(201, "crear la franja del " + day + " por API");
        }
    }
}
