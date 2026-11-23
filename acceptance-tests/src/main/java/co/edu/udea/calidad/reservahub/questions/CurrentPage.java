package co.edu.udea.calidad.reservahub.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

/**
 * Ruta actual del navegador (sin dominio), p. ej. "/client/catalog".
 * {@code is(path)} espera hasta 10 s a que la SPA navegue y responde la ruta final.
 */
public class CurrentPage implements Question<String> {
    private final String expected;

    private CurrentPage(String expected) { this.expected = expected; }

    public static CurrentPage path() { return new CurrentPage(null); }

    /** Espera a que la ruta sea {@code expected} (o se acabe el tiempo) y devuelve la ruta actual. */
    public static CurrentPage afterNavigatingTo(String expected) { return new CurrentPage(expected); }

    @Override
    public String answeredBy(Actor actor) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        String path = read(actor);
        while (expected != null && !expected.equals(path) && Instant.now().isBefore(deadline)) {
            sleep();
            path = read(actor);
        }
        return path;
    }

    private static String read(Actor actor) {
        String url = BrowseTheWeb.as(actor).getDriver().getCurrentUrl();
        String path = URI.create(url).getPath();
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
