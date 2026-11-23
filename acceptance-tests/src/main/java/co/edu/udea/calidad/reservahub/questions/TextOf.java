package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.questions.Text;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Texto visible de un elemento. Espera a que aparezca (las respuestas de la API son asíncronas). */
public class TextOf implements Question<String> {
    private final Target target;

    private TextOf(Target target) { this.target = target; }

    public static TextOf the(Target target) { return new TextOf(target); }

    @Override
    public String answeredBy(Actor actor) {
        actor.attemptsTo(WaitUntil.the(target, isVisible()).forNoMoreThan(10).seconds());
        return Text.of(target).answeredBy(actor).trim();
    }
}
