package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.SetFieldValue;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.CANCEL_REASON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.CONFIRM_CANCEL_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AgendaUI.RESERVATION_ACTION;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-15: el proveedor cancela desde su agenda la reserva de un cliente (buscada por su nombre).
 * Con motivo vacío no se escribe nada en el campo, para probar la validación del navegador.
 */
public class CancelReservationAsProvider implements Task {
    private final String clientName;
    private final String reason;

    public CancelReservationAsProvider(String clientName, String reason) {
        this.clientName = clientName;
        this.reason = reason;
    }

    public static CancelReservationAsProvider of(String clientName, String reason) {
        return Tasks.instrumented(CancelReservationAsProvider.class, clientName, reason);
    }

    public static CancelReservationAsProvider withoutReason(String clientName) {
        return Tasks.instrumented(CancelReservationAsProvider.class, clientName, "");
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(RESERVATION_ACTION.of(clientName, "Cancelar"), isVisible()).forNoMoreThan(10).seconds(),
                Click.on(RESERVATION_ACTION.of(clientName, "Cancelar")),
                WaitUntil.the(CANCEL_REASON, isVisible()).forNoMoreThan(10).seconds());
        if (!reason.isEmpty()) {
            // Se asigna tal cual para conservar un motivo de solo espacios.
            actor.attemptsTo(SetFieldValue.of(CANCEL_REASON, reason));
        }
        actor.attemptsTo(
                Pause.of(300),
                Click.on(CONFIRM_CANCEL_BUTTON));
    }
}
