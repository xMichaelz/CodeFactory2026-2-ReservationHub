package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.exceptions.ActorStateException;
import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import co.edu.udea.calidad.reservahub.models.ResourceInfo;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.questions.BrowserValidation;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.TextsShown;
import co.edu.udea.calidad.reservahub.tasks.provider.AddScheduleWindow;
import co.edu.udea.calidad.reservahub.tasks.provider.BlockScheduleWindow;
import co.edu.udea.calidad.reservahub.tasks.provider.ChangeServicePrice;
import co.edu.udea.calidad.reservahub.tasks.provider.CreateService;
import co.edu.udea.calidad.reservahub.tasks.provider.DeactivateService;
import co.edu.udea.calidad.reservahub.tasks.provider.GoToSection;
import co.edu.udea.calidad.reservahub.tasks.provider.LinkResource;
import co.edu.udea.calidad.reservahub.tasks.provider.RegisterResource;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import net.serenitybdd.screenplay.Actor;

import static co.edu.udea.calidad.reservahub.userinterfaces.ProviderUI.CARD_TITLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ProviderUI.PENDING_STEP;
import static co.edu.udea.calidad.reservahub.userinterfaces.ProviderUI.STAT_VALUE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.QUANTITY;
import static co.edu.udea.calidad.reservahub.userinterfaces.ResourcesUI.RESOURCE_ROW;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.BLOCKS;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.WINDOW_HOURS;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_PRICE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_RESOURCES;
import static co.edu.udea.calidad.reservahub.userinterfaces.ServicesUI.SERVICE_STATUS;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/** Panel del proveedor, HU-05/06/07 servicios, HU-10 recursos y HU-08/09 horarios y bloqueos. */
public class ProviderCatalogStepDefinitions {

    // ---------- panel ----------
    @Then("the provider should see the card {string}")
    public void providerShouldSeeCard(String title) {
        Actors.provider().should(seeThat(TextOf.the(CARD_TITLE.of(title)), equalTo(title)));
    }

    @Then("the provider should see the step {string} as pending")
    public void providerShouldSeePendingStep(String step) {
        Actors.provider().should(seeThat(TextOf.the(PENDING_STEP.of(step)), equalTo(step)));
    }

    @Then("the provider should see the statistic {string} with the value {string}")
    public void providerShouldSeeStat(String label, String value) {
        Actors.provider().should(seeThat(TextOf.the(STAT_VALUE.of(label)), equalTo(value)));
    }

    // ---------- servicios ----------
    @When("the provider creates the service {string} from the Services section")
    public void providerCreatesService(String baseName) {
        Actor provider = Actors.provider();
        ServiceInfo service = ServiceInfo.named(baseName);
        provider.attemptsTo(GoToSection.called("Servicios"), CreateService.with(service));
        ActorMemory.rememberService(provider, service);
    }

    @When("the provider tries to create another service with the same name as {string}")
    public void providerCreatesDuplicateService(String baseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Servicios"),
                CreateService.with(PreconditionStepDefinitions.serviceOf(provider, baseName)));
    }

    @When("the provider changes the price of the service {string} to {int}")
    public void providerChangesPrice(String baseName, int price) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Servicios"),
                ChangeServicePrice.of(serviceName(baseName), price));
    }

    @When("the provider deactivates the service {string}")
    public void providerDeactivatesService(String baseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Servicios"),
                DeactivateService.called(serviceName(baseName)));
    }

    @When("the provider links the resource {string} to the service {string}")
    public void providerLinksResource(String resourceBaseName, String serviceBaseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Servicios"),
                LinkResource.named(resourceName(resourceBaseName), serviceName(serviceBaseName)));
    }

    @Then("the provider should see the service {string} with the status {string}")
    public void providerShouldSeeServiceStatus(String baseName, String status) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_STATUS.of(serviceName(baseName))), equalTo(status)));
    }

    @Then("the provider should see the service {string} with a price of {string}")
    public void providerShouldSeeServicePrice(String baseName, String price) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_PRICE.of(serviceName(baseName))), containsString(price)));
    }

    @Then("the provider should see the resource {string} in the row of the service {string}")
    public void providerShouldSeeResourceInService(String resourceBaseName, String serviceBaseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_RESOURCES.of(serviceName(serviceBaseName))),
                containsString(resourceName(resourceBaseName))));
    }

    // ---------- recursos ----------
    @When("the provider registers the resource {string}")
    public void providerRegistersResource(String baseName) {
        Actor provider = Actors.provider();
        ResourceInfo resource = ResourceInfo.named(baseName);
        provider.attemptsTo(GoToSection.called("Recursos"), RegisterResource.with(resource));
        ActorMemory.rememberValue(provider, resource.name);
    }

    @When("the provider tries to register another resource with the same name as {string}")
    public void providerRegistersDuplicateResource(String baseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Recursos"),
                RegisterResource.with(ResourceInfo.exactly(resourceName(baseName))));
    }

    @When("the provider tries to register the resource {string} with quantity {string}")
    public void providerRegistersResourceWithQuantity(String baseName, String quantity) {
        Actors.provider().attemptsTo(
                GoToSection.called("Recursos"),
                RegisterResource.withQuantity(ResourceInfo.named(baseName), quantity));
    }

    @Then("the provider should see the resource {string} in My resources")
    public void providerShouldSeeResource(String baseName) {
        String name = resourceName(baseName);
        Actors.provider().should(seeThat(TextOf.the(RESOURCE_ROW.of(name)), containsString(name)));
    }

    @Then("the provider should not be able to submit a quantity lower than 1")
    public void providerCannotSubmitQuantityLowerThanOne() {
        Actors.provider().should(seeThat(BrowserValidation.of(QUANTITY), equalTo("rangeUnderflow")));
    }

    // ---------- horarios ----------
    @When("the provider adds a window on {string} from {string} to {string}")
    public void providerAddsWindow(String day, String startTime, String endTime) {
        Actors.provider().attemptsTo(
                GoToSection.called("Horarios"),
                AddScheduleWindow.on(day, startTime, endTime));
    }

    @When("the provider blocks the window on {string} from {string} to {string} for {string}")
    public void providerBlocksWindow(String day, String startTime, String endTime, String reason) {
        Actors.provider().attemptsTo(
                GoToSection.called("Horarios"),
                BlockScheduleWindow.on(day, startTime, endTime, reason));
    }

    @Then("the provider should see the window from {string} to {string} on {string}")
    public void providerShouldSeeWindow(String startTime, String endTime, String day) {
        Actors.provider().should(seeThat(TextsShown.in(WINDOW_HOURS.of(day)), hasItem(startTime + " – " + endTime)));
    }

    @Then("the provider should see the block {string} in the window on {string}")
    public void providerShouldSeeBlock(String reason, String day) {
        Actors.provider().should(seeThat(TextOf.the(BLOCKS.of(day)), containsString(reason)));
    }

    // ---------- apoyo ----------
    private static String serviceName(String baseName) {
        return PreconditionStepDefinitions.serviceOf(Actors.provider(), baseName).name;
    }

    /** El recurso que creó el escenario (por API o por la UI) a partir del nombre base. */
    private static String resourceName(String baseName) {
        String name = ActorMemory.value(Actors.provider());
        if (name == null || !name.startsWith(baseName + " ")) {
            throw new ActorStateException("El proveedor no tiene el recurso " + baseName);
        }
        return name;
    }
}
