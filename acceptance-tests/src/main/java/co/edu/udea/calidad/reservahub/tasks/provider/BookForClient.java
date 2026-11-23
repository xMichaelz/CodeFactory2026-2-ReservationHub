package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.SelectFromOptions;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.CLIENT_FOUND;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.CREATE_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.NEXT_WEEK;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.SERVICE;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.BOOKING_CONFIRMATION;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.FREE_SLOTS;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.SELECTED_SLOT;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isEnabled;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-13 completa: elige el servicio, busca al cliente por correo, toma el primer hueco libre de la
 * semana siguiente y crea la reserva.
 */
public class BookForClient implements Task {
    private final ServiceInfo service;
    private final String clientEmail;

    public BookForClient(ServiceInfo service, String clientEmail) {
        this.service = service;
        this.clientEmail = clientEmail;
    }

    public static BookForClient of(ServiceInfo service, String clientEmail) {
        return Tasks.instrumented(BookForClient.class, service, clientEmail);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                SelectFromOptions.byVisibleText(service.optionLabel()).from(SERVICE),
                LookUpClient.byEmail(clientEmail),
                WaitUntil.the(CLIENT_FOUND, isVisible()).forNoMoreThan(10).seconds(),
                WaitUntil.the(NEXT_WEEK, isEnabled()).forNoMoreThan(10).seconds(),
                Click.on(NEXT_WEEK),
                WaitUntil.the(FREE_SLOTS, isVisible()).forNoMoreThan(10).seconds(),
                Click.on(FREE_SLOTS),
                WaitUntil.the(SELECTED_SLOT, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300),
                Click.on(CREATE_BUTTON),
                WaitUntil.the(BOOKING_CONFIRMATION, isVisible()).forNoMoreThan(10).seconds());
    }
}
