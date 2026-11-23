package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;

import java.time.Duration;
import java.time.Instant;

/**
 * Texto de un elemento que se espera que cambie (p. ej. una fila después de reprogramar): espera
 * hasta 10 s a que sea distinto de {@code previous} y responde el texto que tenga al final.
 */
public class ChangedTextOf implements Question<String> {
    private final Target target;
    private final String previous;

    private ChangedTextOf(Target target, String previous) {
        this.target = target;
        this.previous = previous;
    }

    public static ChangedTextOf the(Target target, String previous) { return new ChangedTextOf(target, previous); }

    @Override
    public String answeredBy(Actor actor) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        String text = read(actor);
        while (previous.equals(text) && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            text = read(actor);
        }
        return text;
    }

    private String read(Actor actor) {
        try {
            return TextOf.the(target).answeredBy(actor);
        } catch (RuntimeException stale) {
            // La tabla se vuelve a pintar mientras recarga: se lee de nuevo en la siguiente vuelta.
            return previous;
        }
    }
}
