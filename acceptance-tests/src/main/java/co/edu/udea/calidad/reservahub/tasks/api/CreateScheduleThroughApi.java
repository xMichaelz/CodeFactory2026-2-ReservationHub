package co.edu.udea.calidad.reservahub.tasks.api;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /schedules (HU-08) para un solo día: CreateScheduleThroughApi.on("MONDAY", "08:00", "12:00"). */
public class CreateScheduleThroughApi implements Task {
    private final String dayOfWeek;
    private final String startTime;
    private final String endTime;

    public CreateScheduleThroughApi(String dayOfWeek, String startTime, String endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static CreateScheduleThroughApi on(String dayOfWeek, String startTime, String endTime) {
        return Tasks.instrumented(CreateScheduleThroughApi.class, dayOfWeek, startTime + ":00", endTime + ":00");
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/schedules").with(request -> Api.authenticated(actor, request)
                .body(Map.of("dayOfWeek", dayOfWeek, "startTime", startTime, "endTime", endTime))));
        Api.expect(201, "crear la franja del " + dayOfWeek + " por API");
    }
}
