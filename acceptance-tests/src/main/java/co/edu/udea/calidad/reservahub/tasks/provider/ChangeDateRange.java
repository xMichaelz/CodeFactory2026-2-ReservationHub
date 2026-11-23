package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.SetFieldValue;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;

import java.time.LocalDate;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.RANGE_FROM;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.RANGE_TO;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.CONSULT_BUTTON;

/** Cambia el filtro de fechas (agenda, historial, reportes) y pulsa "Consultar". */
public class ChangeDateRange implements Task {
    private final String from;
    private final String to;

    public ChangeDateRange(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public static ChangeDateRange between(LocalDate from, LocalDate to) {
        return Tasks.instrumented(ChangeDateRange.class, from.toString(), to.toString());
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                SetFieldValue.of(RANGE_FROM, from),
                SetFieldValue.of(RANGE_TO, to),
                Pause.of(300),
                Click.on(CONSULT_BUTTON));
    }
}
