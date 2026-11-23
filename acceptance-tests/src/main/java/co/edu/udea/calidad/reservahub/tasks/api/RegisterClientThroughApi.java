package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /users (HU-01) + login por API. El actor queda con su cuenta y su token. */
public class RegisterClientThroughApi implements Task {
    private final ClientAccount account;

    public RegisterClientThroughApi(ClientAccount account) {
        this.account = account;
    }

    public static RegisterClientThroughApi with(ClientAccount account) {
        return Tasks.instrumented(RegisterClientThroughApi.class, account);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/users").with(request -> Api.json(request).body(Map.of(
                "email", account.email,
                "password", account.password,
                "firstName", account.firstName,
                "lastName", account.lastName,
                "phone", account.phone))));
        Api.expect(201, "registrar al cliente por API");
        ActorMemory.rememberClient(actor, account);
        actor.attemptsTo(LogInThroughApi.with(account.email, account.password));
    }
}
