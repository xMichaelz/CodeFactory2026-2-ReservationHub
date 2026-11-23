package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.core.pages.WebElementFacade;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;

import java.util.ArrayList;
import java.util.List;

/** Textos de todos los elementos que coinciden (filas de una tabla, tarjetas del catálogo...). */
public class TextsOf implements Question<List<String>> {
    private final Target target;

    private TextsOf(Target target) { this.target = target; }

    public static TextsOf each(Target target) { return new TextsOf(target); }

    @Override
    public List<String> answeredBy(Actor actor) {
        List<String> texts = new ArrayList<>();
        for (WebElementFacade element : target.resolveAllFor(actor)) {
            texts.add(element.getText().trim());
        }
        return texts;
    }
}
