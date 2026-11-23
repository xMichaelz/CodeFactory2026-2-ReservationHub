package co.edu.udea.calidad.reservahub.interactions;

import co.edu.udea.calidad.reservahub.utils.Pages;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.actions.Open;
import org.openqa.selenium.JavascriptExecutor;

/** Abre la portada sin sesión: borra cookies y el JWT guardado en localStorage. */
public class OpenCleanSession implements Interaction {
    public static OpenCleanSession now() {
        return new OpenCleanSession();
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(Open.url(Pages.HOME));

        var driver = BrowseTheWeb.as(actor).getDriver();
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
                "window.localStorage.clear(); window.sessionStorage.clear();");

        actor.attemptsTo(Open.url(Pages.HOME));
    }
}
