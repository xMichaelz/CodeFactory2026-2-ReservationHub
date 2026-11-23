package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Por qué el navegador no deja enviar un campo (validación HTML5): "valueMissing" si es obligatorio
 * y está vacío, "rangeUnderflow" si está por debajo de min, ... o "valid" si no hay problema.
 */
public class BrowserValidation implements Question<String> {
    private static final String SCRIPT = String.join("\n",
            "const validity = arguments[0].validity;",
            "for (const key in validity) { if (key !== 'valid' && validity[key]) return key; }",
            "return validity.valid ? 'valid' : 'invalid';");

    private final Target target;

    private BrowserValidation(Target target) { this.target = target; }

    public static BrowserValidation of(Target target) { return new BrowserValidation(target); }

    @Override
    public String answeredBy(Actor actor) {
        var element = target.resolveFor(actor);
        return String.valueOf(((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript(SCRIPT, element));
    }
}
