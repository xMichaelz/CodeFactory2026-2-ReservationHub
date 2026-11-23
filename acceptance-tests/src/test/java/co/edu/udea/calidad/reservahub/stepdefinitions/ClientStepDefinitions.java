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
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.screenplay.waits.WaitUntil;

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
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATIONS_TABLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_STATUS;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.RESERVATION_TIME;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.UPCOMING_APPOINTMENTS;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;
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
    @Cuando("el cliente abre su inicio")
    public void clientOpensHome() {
        // Recarga la página: la sesión se abrió en /client antes de que existiera la reserva.
        Actors.client().attemptsTo(OpenPage.at(Pages.CLIENT_HOME));
    }

    @Entonces("el cliente debería ver su saludo de bienvenida")
    public void clientShouldSeeGreeting() {
        String user = ActorMemory.client(Actors.client()).email.split("@")[0];
        Actors.client().should(seeThat(TextOf.the(PAGE_TITLE), equalTo("Hola, " + user)));
    }

    @Entonces("el cliente debería ver el servicio {string} en sus próximas citas")
    public void clientShouldSeeUpcomingAppointment(String service) {
        Actors.client().should(seeThat(TextsShown.in(UPCOMING_APPOINTMENTS),
                hasItem(containsString(serviceName(service)))));
    }

    // ---------- catálogo ----------
    @Cuando("el cliente abre el catálogo")
    public void clientOpensCatalog() {
        Actors.client().attemptsTo(
                OpenClientSection.called("Catálogo"),
                WaitUntil.the(CATALOG_RESULT, isVisible()).forNoMoreThan(10).seconds());
    }

    @Cuando("el cliente busca en el catálogo el servicio {string}")
    public void clientSearchesService(String service) {
        Actors.client().attemptsTo(SearchCatalog.forText(serviceName(service)));
    }

    @Cuando("el cliente busca en el catálogo el texto {string}")
    public void clientSearchesText(String text) {
        Actors.client().attemptsTo(SearchCatalog.forText(text));
    }

    @Cuando("el cliente busca en el catálogo el negocio del proveedor")
    public void clientSearchesProviderBusiness() {
        Actors.client().attemptsTo(SearchCatalog.forText(businessName()));
    }

    @Cuando("el cliente filtra el catálogo por el negocio del proveedor")
    public void clientFiltersCatalogByProvider() {
        Actors.client().attemptsTo(FilterCatalogByProvider.named(businessName()));
    }

    @Entonces("el catálogo debería mostrar el servicio {string}")
    public void catalogShouldShowService(String service) {
        Actors.client().should(seeThat(TextsShown.in(CATALOG_SERVICE_NAMES), hasItem(serviceName(service))));
    }

    @Entonces("el catálogo no debería mostrar el servicio {string}")
    public void catalogShouldNotShowService(String service) {
        Actors.client().should(seeThat(TextsShown.in(CATALOG_SERVICE_NAMES).onceVisible(CATALOG_RESULT),
                not(hasItem(serviceName(service)))));
    }

    @Entonces("el catálogo solo debería mostrar servicios del negocio del proveedor")
    public void catalogShouldOnlyShowProviderServices() {
        // La tarjeta muestra el negocio en mayúsculas por CSS (text-transform).
        Actors.client().should(seeThat(TextsShown.in(CATALOG_PROVIDER_NAMES),
                everyItem(equalToIgnoringCase(businessName()))));
    }

    // ---------- reservar ----------
    @Cuando("el cliente abre la disponibilidad del servicio {string}")
    public void clientOpensAvailability(String service) {
        Actors.client().attemptsTo(StartBooking.of(serviceName(service)));
    }

    @Cuando("el cliente reserva el primer horario libre del servicio {string}")
    public void clientBooksFirstFreeSlot(String service) {
        Actors.client().attemptsTo(StartBooking.of(serviceName(service)), BookFirstFreeSlot.now());
    }

    @Cuando("el cliente pasa a la semana siguiente de la disponibilidad")
    public void clientShowsNextWeek() {
        Actors.client().attemptsTo(ShowNextWeek.ofAvailability());
    }

    @Entonces("el cliente debería ver la confirmación de la reserva del servicio {string}")
    public void clientShouldSeeConfirmation(String service) {
        Actors.client().should(
                seeThat(TextOf.the(CONFIRMATION_DETAIL.of("Servicio")), equalTo(serviceName(service))),
                seeThat(TextOf.the(CONFIRMATION_DETAIL.of("Proveedor")), equalTo(businessName())));
    }

    @Entonces("el cliente debería ver la disponibilidad a partir de dentro de {int} días")
    public void clientShouldSeeAvailabilityFrom(int days) {
        String text = LONG_DATE.format(TestData.today().plusDays(days));
        String expected = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        Actors.client().should(seeThat(TextOf.the(AVAILABILITY_RANGE), startsWith(expected)));
    }

    // ---------- mis reservas ----------
    @Cuando("el cliente revisa sus reservas")
    public void clientReviewsReservations() {
        Actors.client().attemptsTo(
                OpenClientSection.called("Mis reservas"),
                WaitUntil.the(RESERVATIONS_TABLE, isVisible()).forNoMoreThan(10).seconds());
    }

    @Cuando("el cliente cancela la reserva del servicio {string}")
    public void clientCancelsReservation(String service) {
        Actors.client().attemptsTo(CancelReservation.of(serviceName(service)));
    }

    @Cuando("el cliente cancela la reserva del servicio {string} con el motivo {string}")
    public void clientCancelsReservationWithReason(String service, String reason) {
        Actors.client().attemptsTo(CancelReservation.of(serviceName(service), reason));
    }

    @Cuando("el cliente reprograma la reserva del servicio {string}")
    public void clientReschedulesReservation(String service) {
        Actors.client().attemptsTo(RescheduleReservation.of(serviceName(service)));
    }

    @Cuando("el cliente consulta sus reservas desde dentro de {int} días hasta dentro de {int} días")
    public void clientConsultsRange(int fromDays, int toDays) {
        Actors.client().attemptsTo(ConsultReservations.betweenDays(fromDays, toDays));
    }

    @Entonces("el cliente debería ver la reserva del servicio {string} con el estado {string}")
    public void clientShouldSeeReservationStatus(String service, String status) {
        Actors.client().should(seeThat(TextOf.the(RESERVATION_STATUS.of(serviceName(service))), equalTo(status)));
    }

    @Entonces("el cliente debería ver la reserva del servicio {string} en un horario distinto al anterior")
    public void clientShouldSeeNewTime(String service) {
        String previous = ActorMemory.value(Actors.client());
        Actors.client().should(seeThat(TextOf.the(RESERVATION_TIME.of(serviceName(service))),
                not(equalTo(previous))));
    }

    // ---------- historial (HU-17) ----------
    @Cuando("el cliente revisa su historial")
    public void clientReviewsHistory() {
        Actors.client().attemptsTo(
                OpenClientSection.called("Historial"),
                WaitUntil.the(HISTORY_TABLE, isVisible()).forNoMoreThan(10).seconds());
    }

    @Cuando("el cliente filtra su historial por {string}")
    public void clientFiltersHistory(String lifecycle) {
        Actors.client().attemptsTo(FilterHistory.by(lifecycle));
    }

    @Entonces("el cliente debería ver en su historial el servicio {string} como {string}")
    public void clientShouldSeeLifecycle(String service, String lifecycle) {
        Actors.client().should(seeThat(TextOf.the(HISTORY_LIFECYCLE.of(serviceName(service))), equalTo(lifecycle)));
    }

    @Entonces("el historial del cliente solo debería mostrar reservas {string}")
    public void historyShouldOnlyShow(String lifecycle) {
        Actors.client().should(seeThat(TextsShown.in(HISTORY_LIFECYCLES), everyItem(equalTo(lifecycle))));
    }

    @Entonces("el historial del cliente no debería incluir el servicio {string}")
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
