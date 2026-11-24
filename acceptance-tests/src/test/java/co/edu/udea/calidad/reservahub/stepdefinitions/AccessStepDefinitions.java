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
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import net.serenitybdd.screenplay.Actor;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.ERROR_MESSAGE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;
import static org.hamcrest.Matchers.startsWith;

/** HU-01 registro de cliente, HU-02 registro de proveedor, HU-03 login, HU-04 logout y permisos por rol. */
public class AccessStepDefinitions {

    // ---------- login ----------
    @When("the client logs in")
    public void clientLogsIn() {
        Actors.client().attemptsTo(LoginAs.himself());
    }

    @When("the provider logs in")
    public void providerLogsIn() {
        Actors.provider().attemptsTo(LoginAs.himself());
    }

    @When("the client tries to log in with the password {string}")
    public void clientTriesWrongPassword(String password) {
        Actor client = Actors.client();
        client.attemptsTo(LoginAs.withCredentials(ActorMemory.client(client).email, password));
    }

    @When("the client fails to log in {int} times")
    public void clientFailsLoginSeveralTimes(int attempts) {
        for (int i = 0; i < attempts; i++) {
            clientTriesWrongPassword("Incorrecta" + i + "99");
        }
    }

    @When("someone tries to log in with the email {string}")
    public void someoneTriesUnknownEmail(String email) {
        Actors.visitor().attemptsTo(LoginAs.withCredentials(email, "Cualquiera123"));
    }

    @When("someone without a session opens the page {string}")
    public void visitorOpensProtectedPage(String path) {
        Actors.visitor().attemptsTo(OpenCleanSession.now(), OpenPage.at(path));
    }

    // ---------- registro ----------
    @When("a new client registers with their details")
    public void newClientRegisters() {
        Actors.client().attemptsTo(RegisterAsClient.with(ClientAccount.random()));
    }

    @When("a new client registers with the password {string}")
    public void newClientRegistersWithPassword(String password) {
        Actors.client().attemptsTo(RegisterAsClient.with(ClientAccount.random().withPassword(password)));
    }

    @When("a new client registers with a different password confirmation")
    public void newClientRegistersWithMismatchedConfirmation() {
        Actors.client().attemptsTo(
                RegisterAsClient.withMismatchedConfirmation(ClientAccount.random(), "OtraClave999"));
    }

    @When("another person tries to register with the client's email")
    public void someoneRegistersWithTakenEmail() {
        String takenEmail = ActorMemory.client(Actors.client()).email;
        Actors.otherClient().attemptsTo(
                RegisterAsClient.with(ClientAccount.named("Otra", "Persona").withEmail(takenEmail)));
    }

    @When("a new provider registers their business")
    public void newProviderRegisters() {
        Actors.provider().attemptsTo(RegisterAsProvider.with(ProviderAccount.random()));
    }

    @When("another provider tries to register a business with the same name")
    public void anotherProviderRegistersSameBusiness() {
        String takenName = ActorMemory.provider(Actors.provider()).businessName;
        Actors.visitor().attemptsTo(RegisterAsProvider.with(ProviderAccount.random().withBusinessName(takenName)));
    }

    // ---------- verificaciones ----------
    @Then("they should see an error message starting with {string}")
    public void shouldSeeErrorStartingWith(String prefix) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(ERROR_MESSAGE), startsWith(prefix)));
    }
}
