package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.rest.interactions.Post;

import java.util.Map;

/** POST /providers (HU-02) + login por API. El actor queda con su cuenta y su token. */
public class RegisterProviderThroughApi implements Task {
    private final ProviderAccount account;

    public RegisterProviderThroughApi(ProviderAccount account) {
        this.account = account;
    }

    public static RegisterProviderThroughApi with(ProviderAccount account) {
        return Tasks.instrumented(RegisterProviderThroughApi.class, account);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Post.to(Api.V1 + "/providers").with(request -> Api.json(request).body(Map.of(
                "email", account.email,
                "password", account.password,
                "firstName", account.firstName,
                "lastName", account.lastName,
                "businessName", account.businessName,
                "serviceType", account.serviceType,
                "address", account.address))));
        Api.expect(201, "registrar al proveedor por API");
        ActorMemory.rememberProvider(actor, account);
        actor.attemptsTo(LogInThroughApi.with(account.email, account.password));
    }
}
