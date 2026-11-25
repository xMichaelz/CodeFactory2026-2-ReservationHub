package co.edu.udea.calidad.reservahub.interactions;

import co.edu.udea.calidad.reservahub.utils.Pages;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Open;

/** Navega directamente a una ruta del frontend (p. ej. Pages.SERVICES). */
public class OpenPage implements Interaction {
    private final String path;

    public OpenPage(String path) {
        this.path = path;
    }

    public static OpenPage at(String path) {
        return Tasks.instrumented(OpenPage.class, path);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Open.url(Pages.url(path)));
    }
}
