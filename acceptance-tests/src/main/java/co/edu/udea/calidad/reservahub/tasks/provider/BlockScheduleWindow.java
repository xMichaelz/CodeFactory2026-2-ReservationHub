package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.SetFieldValue;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.BLOCK_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.BLOCK_END;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.BLOCK_REASON;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.BLOCK_START;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.REGISTER_BLOCK_BUTTON;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-09: bloquea parte de una franja con el botón "+ Bloqueo". La fecha la propone la ventana (la
 * próxima fecha que cae en el día de la franja); aquí se eligen las horas y el motivo.
 */
public class BlockScheduleWindow implements Task {
    private final String day;
    private final String startTime;
    private final String endTime;
    private final String reason;

    public BlockScheduleWindow(String day, String startTime, String endTime, String reason) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public static BlockScheduleWindow on(String day, String startTime, String endTime, String reason) {
        return Tasks.instrumented(BlockScheduleWindow.class, day, startTime, endTime, reason);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(BLOCK_BUTTON.of(day)),
                WaitUntil.the(BLOCK_REASON, isVisible()).forNoMoreThan(10).seconds(),
                SetFieldValue.of(BLOCK_START, startTime),
                SetFieldValue.of(BLOCK_END, endTime),
                Enter.theValue(reason).into(BLOCK_REASON),
                Pause.of(300),
                Click.on(REGISTER_BLOCK_BUTTON));
    }
}
