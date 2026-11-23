package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.OpenCleanSession;
import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.tasks.LoginAs;
import co.edu.udea.calidad.reservahub.tasks.RegisterAsClient;
import co.edu.udea.calidad.reservahub.tasks.RegisterAsProvider;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.screenplay.Actor;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.ERROR_MESSAGE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;
import static org.hamcrest.Matchers.startsWith;

/** HU-01 registro de cliente, HU-02 registro de proveedor, HU-03 login, HU-04 logout y permisos por rol. */
public class AccessStepDefinitions {

    // ---------- login ----------
    @Cuando("el cliente inicia sesión")
    public void clientLogsIn() {
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @Cuando("el proveedor inicia sesión")
    public void providerLogsIn() {
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    @Cuando("el cliente intenta iniciar sesión con la contraseña {string}")
    public void clientTriesWrongPassword(String password) {
        Actor client = Actors.client();
        client.attemptsTo(LoginAs.withCredentials(ActorMemory.client(client).email, password));
    }

    @Cuando("el cliente falla el inicio de sesión {int} veces")
    public void clientFailsLoginSeveralTimes(int attempts) {
        for (int i = 0; i < attempts; i++) {
            clientTriesWrongPassword("Incorrecta" + i + "99");
        }
    }

    @Cuando("alguien intenta iniciar sesión con el correo {string}")
    public void someoneTriesUnknownEmail(String email) {
        Actors.visitor().attemptsTo(LoginAs.withCredentials(email, "Cualquiera123"));
    }

    @Cuando("alguien sin sesión abre la página {string}")
    public void visitorOpensProtectedPage(String path) {
        Actors.visitor().attemptsTo(OpenCleanSession.now(), OpenPage.at(path));
    }

    // ---------- registro ----------
    @Cuando("un cliente nuevo se registra con sus datos")
    public void newClientRegisters() {
        Actors.client().attemptsTo(RegisterAsClient.with(ClientAccount.random()));
    }

    @Cuando("un cliente nuevo se registra con la contraseña {string}")
    public void newClientRegistersWithPassword(String password) {
        Actors.client().attemptsTo(RegisterAsClient.with(ClientAccount.random().withPassword(password)));
    }

    @Cuando("un cliente nuevo se registra con una confirmación de contraseña distinta")
    public void newClientRegistersWithMismatchedConfirmation() {
        Actors.client().attemptsTo(
                RegisterAsClient.withMismatchedConfirmation(ClientAccount.random(), "OtraClave999"));
    }

    @Cuando("otra persona intenta registrarse con el mismo correo del cliente")
    public void someoneRegistersWithTakenEmail() {
        String takenEmail = ActorMemory.client(Actors.client()).email;
        Actors.otherClient().attemptsTo(
                RegisterAsClient.with(ClientAccount.named("Otra", "Persona").withEmail(takenEmail)));
    }

    @Cuando("un proveedor nuevo registra su negocio")
    public void newProviderRegisters() {
        Actors.provider().attemptsTo(RegisterAsProvider.with(ProviderAccount.random()));
    }

    @Cuando("otro proveedor intenta registrar un negocio con el mismo nombre")
    public void anotherProviderRegistersSameBusiness() {
        String takenName = ActorMemory.provider(Actors.provider()).businessName;
        Actors.visitor().attemptsTo(RegisterAsProvider.with(ProviderAccount.random().withBusinessName(takenName)));
    }

    // ---------- verificaciones ----------
    @Entonces("debería ver un mensaje de error que empieza por {string}")
    public void shouldSeeErrorStartingWith(String prefix) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(ERROR_MESSAGE), startsWith(prefix)));
    }
}
