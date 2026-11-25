package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.exceptions.ActorStateException;
import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.models.ResourceInfo;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.tasks.LoginAs;
import co.edu.udea.calidad.reservahub.tasks.api.BookFreeSlotThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.CancelReservationThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.CreateResourceThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.CreateServiceThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.DeactivateServiceThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.OpenWeeklyScheduleThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.RegisterClientThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.RegisterProviderThroughApi;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.cucumber.java.en.Given;
import net.serenitybdd.screenplay.Actor;

/**
 * Los "Dado": dejan el sistema en el estado que el escenario necesita usando la API, que es más
 * rápido y estable que hacerlo por la UI. Lo que se prueba (Cuando/Entonces) va por la UI.
 *
 * Todos los nombres de servicios y recursos llevan un sufijo único, así que los escenarios se
 * pueden repetir sobre la misma base de datos.
 */
public class PreconditionStepDefinitions {

    // ---------- cuentas ----------
    @Given("the client is registered")
    public void clientIsRegistered() {
        registerClient(Actors.client(), ClientAccount.random());
    }

    @Given("another client named {string} {string} is registered")
    public void otherClientIsRegistered(String firstName, String lastName) {
        registerClient(Actors.otherClient(), ClientAccount.named(firstName, lastName));
    }

    @Given("the provider is registered")
    public void providerIsRegistered() {
        Actor provider = Actors.provider();
        if (!ActorMemory.hasProviderAccount(provider)) {
            provider.attemptsTo(RegisterProviderThroughApi.with(ProviderAccount.random()));
        }
    }

    // ---------- sesión en la UI ----------
    @Given("the client is logged in")
    public void clientIsLoggedIn() {
        clientIsRegistered();
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @Given("the provider is logged in")
    public void providerIsLoggedIn() {
        providerIsRegistered();
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    @Given("the client signs in again")
    public void clientLogsInAgain() {
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @Given("the provider signs in again")
    public void providerLogsInAgain() {
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    // ---------- negocio del proveedor ----------
    @Given("the provider offers the service {string} with an all-week open schedule")
    public void providerOffersServiceWithOpenAgenda(String serviceName) {
        providerIsRegistered();
        Actor provider = Actors.provider();
        boolean firstService = ActorMemory.services(provider).isEmpty();
        provider.attemptsTo(CreateServiceThroughApi.with(ServiceInfo.named(serviceName)));
        if (firstService) {
            provider.attemptsTo(OpenWeeklyScheduleThroughApi.allDayLong());
        }
    }

    @Given("the provider has the service {string}")
    public void providerHasService(String serviceName) {
        providerIsRegistered();
        Actors.provider().attemptsTo(CreateServiceThroughApi.with(ServiceInfo.named(serviceName)));
    }

    @Given("the provider has the resource {string}")
    public void providerHasResource(String resourceName) {
        providerIsRegistered();
        ResourceInfo resource = ResourceInfo.named(resourceName);
        Actors.provider().attemptsTo(CreateResourceThroughApi.with(resource));
        ActorMemory.rememberValue(Actors.provider(), resource.name);
    }

    @Given("the provider deactivated the service {string}")
    public void providerDeactivatedService(String serviceName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(DeactivateServiceThroughApi.of(serviceOf(provider, serviceName)));
    }

    // ---------- reservas ----------
    @Given("the client has a reservation for the service {string}")
    public void clientHasReservation(String serviceName) {
        clientIsRegisteredOnce();
        Actors.client().attemptsTo(BookFreeSlotThroughApi.skipping(
                ActorMemory.reservationsCount(Actors.client()), serviceOf(Actors.provider(), serviceName)));
    }

    @Given("another client has a reservation for the service {string}")
    public void otherClientHasReservation(String serviceName) {
        Actor other = Actors.otherClient();
        if (!ActorMemory.hasClientAccount(other)) {
            registerClient(other, ClientAccount.named("Diana", "Directa"));
        }
        // Se salta los primeros huecos para no chocar con las reservas del cliente principal.
        other.attemptsTo(BookFreeSlotThroughApi.skipping(5, serviceOf(Actors.provider(), serviceName)));
    }

    @Given("the client cancelled that reservation")
    public void clientCancelledThatReservation() {
        Actor client = Actors.client();
        client.attemptsTo(CancelReservationThroughApi.of(ActorMemory.lastReservation(client)));
    }

    @Given("the provider cancelled the other client's reservation")
    public void providerCancelledOtherClientReservation() {
        Actors.provider().attemptsTo(CancelReservationThroughApi.of(
                ActorMemory.lastReservation(Actors.otherClient()), "Cancelada por el proveedor desde la API"));
    }

    // ---------- apoyo ----------
    private void clientIsRegisteredOnce() {
        if (!ActorMemory.hasClientAccount(Actors.client())) {
            clientIsRegistered();
        }
    }

    private static void registerClient(Actor actor, ClientAccount account) {
        actor.attemptsTo(RegisterClientThroughApi.with(account));
    }

    /** "Corte" se refiere al servicio que el proveedor creó a partir de ese nombre base. */
    static ServiceInfo serviceOf(Actor provider, String baseName) {
        return ActorMemory.services(provider).stream()
                .filter(service -> service.name.startsWith(baseName + " "))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new ActorStateException("El proveedor no tiene el servicio " + baseName));
    }
}
