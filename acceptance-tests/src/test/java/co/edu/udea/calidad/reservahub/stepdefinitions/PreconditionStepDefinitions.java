package co.edu.udea.calidad.reservahub.stepdefinitions;

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
import io.cucumber.java.es.Dado;
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
    @Dado("que el cliente está registrado")
    public void clientIsRegistered() {
        registerClient(Actors.client(), ClientAccount.random());
    }

    @Dado("que otro cliente llamado {string} {string} está registrado")
    public void otherClientIsRegistered(String firstName, String lastName) {
        registerClient(Actors.otherClient(), ClientAccount.named(firstName, lastName));
    }

    @Dado("que el proveedor está registrado")
    public void providerIsRegistered() {
        Actor provider = Actors.provider();
        if (!ActorMemory.hasProviderAccount(provider)) {
            provider.attemptsTo(RegisterProviderThroughApi.with(ProviderAccount.random()));
        }
    }

    // ---------- sesión en la UI ----------
    @Dado("que el cliente ha iniciado sesión")
    public void clientIsLoggedIn() {
        clientIsRegistered();
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @Dado("que el proveedor ha iniciado sesión")
    public void providerIsLoggedIn() {
        providerIsRegistered();
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    @Dado("que el cliente inicia sesión")
    public void clientLogsInAgain() {
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @Dado("que el proveedor inicia sesión")
    public void providerLogsInAgain() {
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    // ---------- negocio del proveedor ----------
    @Dado("que el proveedor ofrece el servicio {string} con agenda abierta toda la semana")
    public void providerOffersServiceWithOpenAgenda(String serviceName) {
        providerIsRegistered();
        Actor provider = Actors.provider();
        boolean firstService = ActorMemory.services(provider).isEmpty();
        provider.attemptsTo(CreateServiceThroughApi.with(ServiceInfo.named(serviceName)));
        if (firstService) {
            provider.attemptsTo(OpenWeeklyScheduleThroughApi.allDayLong());
        }
    }

    @Dado("que el proveedor tiene el servicio {string}")
    public void providerHasService(String serviceName) {
        providerIsRegistered();
        Actors.provider().attemptsTo(CreateServiceThroughApi.with(ServiceInfo.named(serviceName)));
    }

    @Dado("que el proveedor tiene el recurso {string}")
    public void providerHasResource(String resourceName) {
        providerIsRegistered();
        ResourceInfo resource = ResourceInfo.named(resourceName);
        Actors.provider().attemptsTo(CreateResourceThroughApi.with(resource));
        ActorMemory.rememberValue(Actors.provider(), resource.name);
    }

    @Dado("que el proveedor desactivó el servicio {string}")
    public void providerDeactivatedService(String serviceName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(DeactivateServiceThroughApi.of(serviceOf(provider, serviceName)));
    }

    // ---------- reservas ----------
    @Dado("que el cliente tiene una reserva del servicio {string}")
    public void clientHasReservation(String serviceName) {
        clientIsRegisteredOnce();
        Actors.client().attemptsTo(BookFreeSlotThroughApi.skipping(
                ActorMemory.reservationsCount(Actors.client()), serviceOf(Actors.provider(), serviceName)));
    }

    @Dado("que otro cliente tiene una reserva del servicio {string}")
    public void otherClientHasReservation(String serviceName) {
        Actor other = Actors.otherClient();
        if (!ActorMemory.hasClientAccount(other)) {
            registerClient(other, ClientAccount.named("Diana", "Directa"));
        }
        // Se salta los primeros huecos para no chocar con las reservas del cliente principal.
        other.attemptsTo(BookFreeSlotThroughApi.skipping(5, serviceOf(Actors.provider(), serviceName)));
    }

    @Dado("que el cliente canceló esa reserva")
    public void clientCancelledThatReservation() {
        Actor client = Actors.client();
        client.attemptsTo(CancelReservationThroughApi.of(ActorMemory.lastReservation(client)));
    }

    @Dado("que el proveedor canceló la reserva de otro cliente")
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
                .orElseThrow(() -> new IllegalStateException("El proveedor no tiene el servicio " + baseName));
    }
}
