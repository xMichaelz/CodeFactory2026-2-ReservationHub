package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.questions.BrowserValidation;
import co.edu.udea.calidad.reservahub.questions.ChangedTextOf;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.tasks.provider.BookForClient;
import co.edu.udea.calidad.reservahub.tasks.provider.CancelReservationAsProvider;
import co.edu.udea.calidad.reservahub.tasks.provider.GoToSection;
import co.edu.udea.calidad.reservahub.tasks.provider.LookUpClient;
import co.edu.udea.calidad.reservahub.tasks.provider.RescheduleReservation;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.CANCEL_REASON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.RESERVATION_ROW;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.RESERVATION_STATUS;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.CLIENT_FOUND;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.CONFIRMED_CLIENT;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.SEE_AGENDA_LINK;
import static co.edu.udea.calidad.reservahub.userinterfaces.BusinessHistoryUI.LIFECYCLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.BusinessHistoryUI.SAVED_STATUS;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/** HU-15/16 agenda del proveedor, HU-13 reservar a nombre de un cliente y HU-18 historial del negocio. */
public class ProviderAgendaStepDefinitions {

    // ---------- agenda ----------
    @Cuando("el proveedor abre su agenda")
    public void providerOpensAgenda() {
        Actors.provider().attemptsTo(GoToSection.called("Agenda"));
    }

    @Cuando("el proveedor intenta cancelar la reserva del cliente sin escribir un motivo")
    public void providerCancelsWithoutReason() {
        String name = clientName();
        Actors.provider().attemptsTo(
                GoToSection.called("Agenda"),
                CancelReservationAsProvider.withoutReason(name));
    }

    @Cuando("el proveedor cancela la reserva del cliente con el motivo {string}")
    public void providerCancelsWithReason(String reason) {
        String name = clientName();
        Actors.provider().attemptsTo(
                GoToSection.called("Agenda"),
                CancelReservationAsProvider.of(name, reason));
    }

    @Cuando("el proveedor reprograma la reserva del cliente a un horario de la semana siguiente")
    public void providerReschedules() {
        Actor provider = Actors.provider();
        provider.attemptsTo(GoToSection.called("Agenda"));
        ActorMemory.rememberValue(provider, TextOf.the(RESERVATION_ROW.of(clientName())).answeredBy(provider));
        provider.attemptsTo(RescheduleReservation.ofClient(clientName()));
    }

    @Entonces("el proveedor debería ver en su agenda la reserva del cliente con el estado {string}")
    public void providerShouldSeeReservationStatus(String status) {
        String name = clientName();
        Actor provider = Actors.provider();
        provider.attemptsTo(WaitUntilLoaded.page());
        provider.should(seeThat(TextOf.the(RESERVATION_STATUS.of(name)), equalTo(status)));
    }

    @Entonces("el proveedor debería ver la reserva del cliente en otro horario")
    public void providerShouldSeeReservationMoved() {
        Actor provider = Actors.provider();
        String before = ActorMemory.value(provider);
        provider.should(seeThat(ChangedTextOf.the(RESERVATION_ROW.of(clientName()), before),
                allOf(not(equalTo(before)), containsString("Confirmada"))));
    }

    @Entonces("el navegador no debería dejar enviar la cancelación sin motivo")
    public void browserRequiresReason() {
        Actors.provider().should(seeThat(BrowserValidation.of(CANCEL_REASON), equalTo("valueMissing")));
    }

    // ---------- reservar a nombre de un cliente ----------
    @Cuando("el proveedor busca al cliente con el correo {string}")
    public void providerLooksUpEmail(String email) {
        Actors.provider().attemptsTo(GoToSection.called("Reservar a cliente"), LookUpClient.byEmail(email));
    }

    @Cuando("el proveedor busca al cliente por su correo")
    public void providerLooksUpClient() {
        providerLooksUpEmail(clientAccount(Actors.client()).email);
    }

    @Cuando("el proveedor reserva {string} a nombre del cliente en un horario de la semana siguiente")
    public void providerBooksForClient(String serviceBaseName) {
        String email = clientAccount(Actors.client()).email;
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Reservar a cliente"),
                BookForClient.of(PreconditionStepDefinitions.serviceOf(provider, serviceBaseName), email));
    }

    @Cuando("el proveedor va a su agenda desde la confirmación")
    public void providerGoesToAgendaFromConfirmation() {
        Actors.provider().attemptsTo(
                Click.on(SEE_AGENDA_LINK),
                WaitUntil.the(PAGE_TITLE, containsText("Agenda de reservas")).forNoMoreThan(10).seconds(),
                WaitUntilLoaded.page());
    }

    @Entonces("el proveedor debería ver que encontró al cliente por su nombre completo")
    public void providerShouldSeeClientFound() {
        Actors.provider().should(seeThat(TextOf.the(CLIENT_FOUND), equalTo(clientName())));
    }

    @Entonces("el proveedor debería ver la reserva creada a nombre del cliente")
    public void providerShouldSeeConfirmation() {
        Actors.provider().should(seeThat(TextOf.the(CONFIRMED_CLIENT), equalTo(clientName())));
    }

    // ---------- historial del negocio ----------
    @Cuando("el proveedor abre el historial del negocio")
    public void providerOpensHistory() {
        Actors.provider().attemptsTo(GoToSection.called("Historial"));
    }

    @Entonces("el proveedor debería ver en el historial la reserva de {string} con el ciclo {string} y el estado {string}")
    public void providerShouldSeeHistoryRow(String who, String lifecycle, String status) {
        String name = nameOf(who);
        Actor provider = Actors.provider();
        provider.should(
                seeThat(TextOf.the(LIFECYCLE.of(name)), equalTo(lifecycle)),
                seeThat(TextOf.the(SAVED_STATUS.of(name)), equalTo(status)));
    }

    // ---------- apoyo ----------
    /**
     * Datos del cliente sin quitarle el foco al proveedor: OnStage.theActorCalled() cambia el actor
     * del foco, y los "Entonces" comunes (notificaciones, errores) los responde el actor del foco.
     */
    private static ClientAccount clientAccount(Actor client) {
        ClientAccount account = ActorMemory.client(client);
        Actors.provider();
        return account;
    }

    private static String clientName() {
        return clientAccount(Actors.client()).fullName();
    }

    /** "el cliente" u "otro cliente" -> el nombre completo con el que se registró. */
    private static String nameOf(String who) {
        switch (who) {
            case "el cliente":
                return clientName();
            case "otro cliente":
                return clientAccount(Actors.otherClient()).fullName();
            default:
                throw new IllegalArgumentException("Actor desconocido: " + who);
        }
    }
}
