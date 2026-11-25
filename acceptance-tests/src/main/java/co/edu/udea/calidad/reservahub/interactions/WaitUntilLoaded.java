package co.edu.udea.calidad.reservahub.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.time.Instant;

/**
 * Espera (hasta 10 s) a que la página no muestre el indicador de carga (.page-loader). Se consulta
 * con JavaScript: WaitUntil(isNotPresent) pagaría la espera implícita de 5 s en cada intento, y las
 * notificaciones de éxito solo duran 4 s.
 */
public class WaitUntilLoaded implements Interaction {
    public static WaitUntilLoaded page() {
        return Tasks.instrumented(WaitUntilLoaded.class);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Object loading = js.executeScript("return document.querySelector('.page-loader') !== null;");
            if (Boolean.FALSE.equals(loading)) {
                return;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
