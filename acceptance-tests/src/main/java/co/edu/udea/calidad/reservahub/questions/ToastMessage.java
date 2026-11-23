package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.waits.WaitUntil;

import java.util.List;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.SUCCESS_TOAST;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Texto de las notificaciones de éxito visibles (pueden apilarse varias), unidas por " | ". */
public class ToastMessage implements Question<String> {
    public static ToastMessage shown() { return new ToastMessage(); }

    @Override
    public String answeredBy(Actor actor) {
        actor.attemptsTo(WaitUntil.the(SUCCESS_TOAST, isVisible()).forNoMoreThan(10).seconds());
        List<String> toasts = TextsOf.each(SUCCESS_TOAST).answeredBy(actor);
        return String.join(" | ", toasts).replace("×", "").trim();
    }
}
