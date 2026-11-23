package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import java.util.List;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * Textos de los elementos que coinciden, esperando antes a que la página termine de cargar:
 * {@code TextsShown.in(lista).onceVisible(marca)} espera a que {@code marca} sea visible.
 */
public class TextsShown implements Question<List<String>> {
    private final Target target;
    private final Target loadedWhen;

    private TextsShown(Target target, Target loadedWhen) {
        this.target = target;
        this.loadedWhen = loadedWhen;
    }

    public static TextsShown in(Target target) { return new TextsShown(target, target); }

    public TextsShown onceVisible(Target marker) { return new TextsShown(target, marker); }

    @Override
    public List<String> answeredBy(Actor actor) {
        actor.attemptsTo(WaitUntil.the(loadedWhen, isVisible()).forNoMoreThan(10).seconds());
        return TextsOf.each(target).answeredBy(actor);
    }
}
