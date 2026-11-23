package co.edu.udea.calidad.reservahub.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.JavascriptExecutor;

import java.time.LocalDate;

/**
 * Escribe una fecha en un {@code <input type="date">}. Chrome muestra el campo con el formato
 * local (dd/mm/aaaa), así que se fija el valor ISO y se avisa a React con el evento "input".
 */
public class ChooseDate implements Interaction {
    private static final String SET_VALUE = "const input = arguments[0];"
            + "const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;"
            + "setter.call(input, arguments[1]);"
            + "input.dispatchEvent(new Event('input', { bubbles: true }));"
            + "input.dispatchEvent(new Event('change', { bubbles: true }));";

    private final LocalDate date;
    private final Target field;

    private ChooseDate(LocalDate date, Target field) {
        this.date = date;
        this.field = field;
    }

    public static ChooseDate of(LocalDate date, Target field) {
        return new ChooseDate(date, field);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver())
                .executeScript(SET_VALUE, field.resolveFor(actor).getElement(), date.toString());
    }
}
