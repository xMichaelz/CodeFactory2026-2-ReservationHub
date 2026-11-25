package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.TextsShown;
import co.edu.udea.calidad.reservahub.tasks.client.BookFirstFreeSlot;
import co.edu.udea.calidad.reservahub.tasks.client.CancelReservation;
import co.edu.udea.calidad.reservahub.tasks.client.ConsultReservations;
import co.edu.udea.calidad.reservahub.tasks.client.FilterCatalogByProvider;
import co.edu.udea.calidad.reservahub.tasks.client.FilterHistory;
import co.edu.udea.calidad.reservahub.tasks.client.OpenClientSection;
import co.edu.udea.calidad.reservahub.tasks.client.RescheduleReservation;
import co.edu.udea.calidad.reservahub.tasks.client.SearchCatalog;
import co.edu.udea.calidad.reservahub.tasks.client.ShowNextWeek;
import co.edu.udea.calidad.reservahub.tasks.client.StartBooking;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.Pages;
import co.edu.udea.calidad.reservahub.utils.TestData;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.AVAILABILITY_RANGE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_PROVIDER_NAMES;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_RESULT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_SERVICE_NAMES;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CONFIRMATION_DETAIL;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_LIFECYCLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_LIFECYCLES;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_SERVICES;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.HISTORY_TABLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_STATUS;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_TIME;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.UPCOMING_APPOINTMENTS;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * Área del cliente: inicio, catálogo (HU-05/HU-07 vistos por el cliente), reserva (HU-11/HU-12),
 * mis reservas (HU-14 cancelar, HU-16 reprogramar) e historial (HU-17).
 *
 * Los servicios se nombran en el .feature por su nombre base ("Corte"); el nombre real lleva un
 * sufijo único y se resuelve con PreconditionStepDefinitions.serviceOf.
 */
public class ClientStepDefinitions {
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' y", Locale.forLanguageTag("es-CO"));

    // ---------- inicio ----------
    @When("the client opens their home page")
    public void clientOpensHome() {
        // Recarga la página: la sesión se abrió en /client antes de que existiera la reserva.
        Actors.client().attemptsTo(OpenPage.at(Pages.CLIENT_HOME));
    }

    @Then("the client should see their welcome greeting")
    public void clientShouldSeeGreeting() {
        String user = ActorMemory.client(Actors.client()).email.split("@")[0];
        Actors.client().should(seeThat(TextOf.the(PAGE_TITLE), equalTo("Hola, " + user)));
    }

    @Then("the client should see the service {string} in their upcoming appointments")
    public void clientShouldSeeUpcomingAppointment(String service) {
        Actors.client().should(seeThat(TextsShown.in(UPCOMING_APPOINTMENTS),
                hasItem(containsString(serviceName(service)))));
    }

    // ---------- catálogo ----------
    @When("the client opens the catalog")
    public void clientOpensCatalog() {
        Actors.client().attemptsTo(OpenClientSection.catalog());
    }

    @When("the client searches the catalog for the service {string}")
    public void clientSearchesService(String service) {
        Actors.client().attemptsTo(SearchCatalog.forText(serviceName(service)));
    }

    @When("the client searches the catalog for the text {string}")
    public void clientSearchesText(String text) {
        Actors.client().attemptsTo(SearchCatalog.forText(text));
    }

    @When("the client searches the catalog for the provider's business")
    public void clientSearchesProviderBusiness() {
        Actors.client().attemptsTo(SearchCatalog.forText(businessName()));
    }

    @When("the client filters the catalog by the provider's business")
    public void clientFiltersCatalogByProvider() {
        Actors.client().attemptsTo(FilterCatalogByProvider.named(businessName()));
    }

    @Then("the catalog should show the service {string}")
    public void catalogShouldShowService(String service) {
        Actors.client().should(seeThat(TextsShown.in(CATALOG_SERVICE_NAMES), hasItem(serviceName(service))));
    }

    @Then("the catalog should not show the service {string}")
    public void catalogShouldNotShowService(String service) {
        Actors.client().should(seeThat(TextsShown.in(CATALOG_SERVICE_NAMES).onceVisible(CATALOG_RESULT),
                not(hasItem(serviceName(service)))));
    }

    @Then("the catalog should only show services from the provider's business")
    public void catalogShouldOnlyShowProviderServices() {
        // La tarjeta muestra el negocio en mayúsculas por CSS (text-transform).
        Actors.client().should(seeThat(TextsShown.in(CATALOG_PROVIDER_NAMES),
                everyItem(equalToIgnoringCase(businessName()))));
    }

    // ---------- reservar ----------
    @When("the client opens the availability of the service {string}")
    public void clientOpensAvailability(String service) {
        Actors.client().attemptsTo(StartBooking.of(serviceName(service)));
    }

    @When("the client books the first free slot of the service {string}")
    public void clientBooksFirstFreeSlot(String service) {
        Actors.client().attemptsTo(StartBooking.of(serviceName(service)), BookFirstFreeSlot.now());
    }

    @When("the client moves to the next week of availability")
    public void clientShowsNextWeek() {
        Actors.client().attemptsTo(ShowNextWeek.ofAvailability());
    }

    @Then("the client should see the booking confirmation for the service {string}")
    public void clientShouldSeeConfirmation(String service) {
        Actors.client().should(
                seeThat(TextOf.the(CONFIRMATION_DETAIL.of("Servicio")), equalTo(serviceName(service))),
                seeThat(TextOf.the(CONFIRMATION_DETAIL.of("Proveedor")), equalTo(businessName())));
    }

    @Then("the client should see availability starting {int} days from now")
    public void clientShouldSeeAvailabilityFrom(int days) {
        String text = LONG_DATE.format(TestData.today().plusDays(days));
        String expected = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        Actors.client().should(seeThat(TextOf.the(AVAILABILITY_RANGE), startsWith(expected)));
    }

    // ---------- mis reservas ----------
    @When("the client reviews their reservations")
    public void clientReviewsReservations() {
        Actors.client().attemptsTo(OpenClientSection.reservations());
    }

    @When("the client cancels the reservation for the service {string}")
    public void clientCancelsReservation(String service) {
        Actors.client().attemptsTo(CancelReservation.of(serviceName(service)));
    }

    @When("the client cancels the reservation for the service {string} with the reason {string}")
    public void clientCancelsReservationWithReason(String service, String reason) {
        Actors.client().attemptsTo(CancelReservation.of(serviceName(service), reason));
    }

    @When("the client reschedules the reservation for the service {string}")
    public void clientReschedulesReservation(String service) {
        Actors.client().attemptsTo(RescheduleReservation.of(serviceName(service)));
    }

    @When("the client views their reservations from {int} days from now to {int} days from now")
    public void clientConsultsRange(int fromDays, int toDays) {
        Actors.client().attemptsTo(ConsultReservations.betweenDays(fromDays, toDays));
    }

    @Then("the client should see the reservation for the service {string} with the status {string}")
    public void clientShouldSeeReservationStatus(String service, String status) {
        Actors.client().should(seeThat(TextOf.the(RESERVATION_STATUS.of(serviceName(service))), equalTo(status)));
    }

    @Then("the client should see the reservation for the service {string} at a different time than before")
    public void clientShouldSeeNewTime(String service) {
        String previous = ActorMemory.value(Actors.client());
        Actors.client().should(seeThat(TextOf.the(RESERVATION_TIME.of(serviceName(service))),
                not(equalTo(previous))));
    }

    // ---------- historial (HU-17) ----------
    @When("the client reviews their history")
    public void clientReviewsHistory() {
        Actors.client().attemptsTo(OpenClientSection.history());
    }

    @When("the client filters their history by {string}")
    public void clientFiltersHistory(String lifecycle) {
        Actors.client().attemptsTo(FilterHistory.by(lifecycle));
    }

    @Then("the client should see the service {string} in their history as {string}")
    public void clientShouldSeeLifecycle(String service, String lifecycle) {
        Actors.client().should(seeThat(TextOf.the(HISTORY_LIFECYCLE.of(serviceName(service))), equalTo(lifecycle)));
    }

    @Then("the client's history should only show {string} reservations")
    public void historyShouldOnlyShow(String lifecycle) {
        Actors.client().should(seeThat(TextsShown.in(HISTORY_LIFECYCLES), everyItem(equalTo(lifecycle))));
    }

    @Then("the client's history should not include the service {string}")
    public void historyShouldNotInclude(String service) {
        Actors.client().should(seeThat(TextsShown.in(HISTORY_SERVICES).onceVisible(HISTORY_TABLE),
                not(hasItem(serviceName(service)))));
    }

    // ---------- apoyo ----------
    // Consultar la memoria del proveedor lo pone en el foco; se devuelve el foco al cliente para que
    // los "Entonces" genéricos (p. ej. la notificación) los responda el navegador del cliente.
    private static String serviceName(String baseName) {
        String name = PreconditionStepDefinitions.serviceOf(Actors.provider(), baseName).name;
        Actors.client();
        return name;
    }

    private static String businessName() {
        String name = ActorMemory.provider(Actors.provider()).businessName;
        Actors.client();
        return name;
    }
}
