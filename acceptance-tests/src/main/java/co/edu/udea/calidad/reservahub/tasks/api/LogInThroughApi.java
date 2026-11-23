package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /auth/login (HU-03) y guarda el JWT en la memoria del actor. */
public class LogInThroughApi implements Task {
    private final String email;
    private final String password;

    public LogInThroughApi(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static LogInThroughApi with(String email, String password) {
        return Tasks.instrumented(LogInThroughApi.class, email, password);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/auth/login")
                .with(request -> Api.json(request).body(Map.of("email", email, "password", password))));
        String token = Api.expect(200, "iniciar sesión por API").jsonPath().getString("accessToken");
        ActorMemory.rememberToken(actor, token);
    }
}
