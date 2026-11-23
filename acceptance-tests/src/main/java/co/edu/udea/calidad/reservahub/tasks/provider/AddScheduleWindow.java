package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.SetFieldValue;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.SelectFromOptions;

import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.ADD_WINDOW_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.DAY;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.END;
import static co.edu.udea.calidad.reservahub.userinterfaces.SchedulesUI.START;

/** HU-08: agrega una franja semanal con el formulario "Nueva franja": AddScheduleWindow.on("Lunes", "08:00", "12:00"). */
public class AddScheduleWindow implements Task {
    private final String day;
    private final String startTime;
    private final String endTime;

    public AddScheduleWindow(String day, String startTime, String endTime) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AddScheduleWindow on(String day, String startTime, String endTime) {
        return Tasks.instrumented(AddScheduleWindow.class, day, startTime, endTime);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                SelectFromOptions.byVisibleText(day).from(DAY),
                SetFieldValue.of(START, startTime),
                SetFieldValue.of(END, endTime),
                Pause.of(300),
                Click.on(ADD_WINDOW_BUTTON));
    }
}
