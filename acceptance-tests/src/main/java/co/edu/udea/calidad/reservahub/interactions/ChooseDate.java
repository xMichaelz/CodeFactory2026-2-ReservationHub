package co.edu.udea.calidad.reservahub.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.targets.Target;

import java.time.LocalDate;

/**
 * Escribe una fecha en un {@code <input type="date">}. Delega en {@link SetFieldValue} para
 * fijar el valor ISO y notificar a React sin depender del formato regional del navegador.
 */
public class ChooseDate implements Interaction {
    private final LocalDate date;
    private final Target field;

    public ChooseDate(LocalDate date, Target field) {
        this.date = date;
        this.field = field;
    }

    public static ChooseDate of(LocalDate date, Target field) {
        return Tasks.instrumented(ChooseDate.class, date, field);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(SetFieldValue.of(field, date.toString()));
    }
}
