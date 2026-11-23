package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.ChooseDate;
import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.utils.TestData;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;

import java.time.LocalDate;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.RANGE_FROM;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.RANGE_TO;

/** Cambia el rango "Desde / Hasta" (en días contados desde hoy) y pulsa "Consultar". */
public class ConsultReservations implements Task {
    private final int fromDays;
    private final int toDays;

    public ConsultReservations(Integer fromDays, Integer toDays) {
        this.fromDays = fromDays;
        this.toDays = toDays;
    }

    public static ConsultReservations betweenDays(int fromDays, int toDays) {
        return Tasks.instrumented(ConsultReservations.class, fromDays, toDays);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        LocalDate today = TestData.today();
        actor.attemptsTo(
                ChooseDate.of(today.plusDays(fromDays), RANGE_FROM),
                ChooseDate.of(today.plusDays(toDays), RANGE_TO),
                Pause.of(300),
                Click.on(BUTTON.of("Consultar")));
    }
}
