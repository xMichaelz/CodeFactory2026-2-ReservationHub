package co.edu.udea.calidad.reservahub.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Escribe el valor de un campo de fecha u hora (input type="date"/"time"). Teclearlo depende del
 * idioma del navegador (a. m./p. m., dd/mm/aaaa), así que se asigna el valor ISO y se avisa a React
 * con los eventos input y change, igual que cuando lo cambia una persona.
 */
public class SetFieldValue implements Interaction {
    private static final String SCRIPT = String.join("\n",
            "const el = arguments[0];",
            "const proto = el.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;",
            "Object.getOwnPropertyDescriptor(proto, 'value').set.call(el, arguments[1]);",
            "el.dispatchEvent(new Event('input', { bubbles: true }));",
            "el.dispatchEvent(new Event('change', { bubbles: true }));");

    private final Target target;
    private final String value;

    private SetFieldValue(Target target, String value) {
        this.target = target;
        this.value = value;
    }

    /** SetFieldValue.of(target, "08:00") o SetFieldValue.of(target, "2026-11-30"). */
    public static SetFieldValue of(Target target, String value) {
        return new SetFieldValue(target, value);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        var element = target.resolveFor(actor);
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript(SCRIPT, element, value);
    }
}
