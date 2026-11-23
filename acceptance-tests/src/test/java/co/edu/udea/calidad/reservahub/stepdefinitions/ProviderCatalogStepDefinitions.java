package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import co.edu.udea.calidad.reservahub.models.ResourceInfo;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import co.edu.udea.calidad.reservahub.questions.BrowserValidation;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.TextsOf;
import co.edu.udea.calidad.reservahub.tasks.provider.AddScheduleWindow;
import co.edu.udea.calidad.reservahub.tasks.provider.BlockScheduleWindow;
import co.edu.udea.calidad.reservahub.tasks.provider.ChangeServicePrice;
import co.edu.udea.calidad.reservahub.tasks.provider.CreateService;
import co.edu.udea.calidad.reservahub.tasks.provider.DeactivateService;
import co.edu.udea.calidad.reservahub.tasks.provider.GoToSection;
import co.edu.udea.calidad.reservahub.tasks.provider.LinkResource;
import co.edu.udea.calidad.reservahub.tasks.provider.RegisterResource;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.waits.WaitUntil;

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
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/** Panel del proveedor, HU-05/06/07 servicios, HU-10 recursos y HU-08/09 horarios y bloqueos. */
public class ProviderCatalogStepDefinitions {

    // ---------- panel ----------
    @Entonces("el proveedor debería ver la tarjeta {string}")
    public void providerShouldSeeCard(String title) {
        Actors.provider().should(seeThat(TextOf.the(CARD_TITLE.of(title)), equalTo(title)));
    }

    @Entonces("el proveedor debería ver pendiente el paso {string}")
    public void providerShouldSeePendingStep(String step) {
        Actors.provider().should(seeThat(TextOf.the(PENDING_STEP.of(step)), equalTo(step)));
    }

    @Entonces("el proveedor debería ver la estadística {string} con el valor {string}")
    public void providerShouldSeeStat(String label, String value) {
        Actors.provider().should(seeThat(TextOf.the(STAT_VALUE.of(label)), equalTo(value)));
    }

    // ---------- servicios ----------
    @Cuando("el proveedor crea el servicio {string} desde la sección Servicios")
    public void providerCreatesService(String baseName) {
        Actor provider = Actors.provider();
        ServiceInfo service = ServiceInfo.named(baseName);
        provider.attemptsTo(GoToSection.called("Servicios"), CreateService.with(service));
        ActorMemory.rememberService(provider, service);
    }

    @Cuando("el proveedor intenta crear otro servicio con el mismo nombre de {string}")
    public void providerCreatesDuplicateService(String baseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Servicios"),
                CreateService.with(PreconditionStepDefinitions.serviceOf(provider, baseName)));
    }

    @Cuando("el proveedor cambia el precio del servicio {string} a {int}")
    public void providerChangesPrice(String baseName, int price) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Servicios"),
                ChangeServicePrice.of(serviceName(baseName), price));
    }

    @Cuando("el proveedor desactiva el servicio {string}")
    public void providerDeactivatesService(String baseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Servicios"),
                DeactivateService.called(serviceName(baseName)));
    }

    @Cuando("el proveedor asocia el recurso {string} al servicio {string}")
    public void providerLinksResource(String resourceBaseName, String serviceBaseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Servicios"),
                LinkResource.named(resourceName(resourceBaseName), serviceName(serviceBaseName)));
    }

    @Entonces("el proveedor debería ver el servicio {string} con el estado {string}")
    public void providerShouldSeeServiceStatus(String baseName, String status) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_STATUS.of(serviceName(baseName))), equalTo(status)));
    }

    @Entonces("el proveedor debería ver el servicio {string} con un precio de {string}")
    public void providerShouldSeeServicePrice(String baseName, String price) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_PRICE.of(serviceName(baseName))), containsString(price)));
    }

    @Entonces("el proveedor debería ver el recurso {string} en la fila del servicio {string}")
    public void providerShouldSeeResourceInService(String resourceBaseName, String serviceBaseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(SERVICE_RESOURCES.of(serviceName(serviceBaseName))),
                containsString(resourceName(resourceBaseName))));
    }

    // ---------- recursos ----------
    @Cuando("el proveedor registra el recurso {string}")
    public void providerRegistersResource(String baseName) {
        Actor provider = Actors.provider();
        ResourceInfo resource = ResourceInfo.named(baseName);
        provider.attemptsTo(GoToSection.called("Recursos"), RegisterResource.with(resource));
        ActorMemory.rememberValue(provider, resource.name);
    }

    @Cuando("el proveedor intenta registrar otro recurso con el mismo nombre de {string}")
    public void providerRegistersDuplicateResource(String baseName) {
        Actors.provider().attemptsTo(
                GoToSection.called("Recursos"),
                RegisterResource.with(ResourceInfo.exactly(resourceName(baseName))));
    }

    @Cuando("el proveedor intenta registrar el recurso {string} con cantidad {string}")
    public void providerRegistersResourceWithQuantity(String baseName, String quantity) {
        Actors.provider().attemptsTo(
                GoToSection.called("Recursos"),
                RegisterResource.withQuantity(ResourceInfo.named(baseName), quantity));
    }

    @Entonces("el proveedor debería ver el recurso {string} en Mis recursos")
    public void providerShouldSeeResource(String baseName) {
        String name = resourceName(baseName);
        Actors.provider().should(seeThat(TextOf.the(RESOURCE_ROW.of(name)), containsString(name)));
    }

    @Entonces("el navegador no debería dejar enviar una cantidad menor que 1")
    public void browserRejectsQuantity() {
        Actors.provider().should(seeThat(BrowserValidation.of(QUANTITY), equalTo("rangeUnderflow")));
    }

    // ---------- horarios ----------
    @Cuando("el proveedor agrega una franja el {string} de {string} a {string}")
    public void providerAddsWindow(String day, String startTime, String endTime) {
        Actors.provider().attemptsTo(
                GoToSection.called("Horarios"),
                AddScheduleWindow.on(day, startTime, endTime));
    }

    @Cuando("el proveedor bloquea la franja del {string} de {string} a {string} por {string}")
    public void providerBlocksWindow(String day, String startTime, String endTime, String reason) {
        Actors.provider().attemptsTo(
                GoToSection.called("Horarios"),
                BlockScheduleWindow.on(day, startTime, endTime, reason));
    }

    @Entonces("el proveedor debería ver la franja de {string} a {string} el {string}")
    public void providerShouldSeeWindow(String startTime, String endTime, String day) {
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntil.the(WINDOW_HOURS.of(day), isVisible()).forNoMoreThan(10).seconds());
        provider.should(seeThat(TextsOf.each(WINDOW_HOURS.of(day)), hasItem(startTime + " – " + endTime)));
    }

    @Entonces("el proveedor debería ver el bloqueo {string} en la franja del {string}")
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
            throw new IllegalStateException("El proveedor no tiene el recurso " + baseName);
        }
        return name;
    }
}
