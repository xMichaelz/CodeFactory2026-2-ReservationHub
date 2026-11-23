package co.edu.udea.calidad.reservahub.interactions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;

/**
 * Pausa breve para demostraciones. Con la variable de entorno DEMO_DELAY (ms) se fija para todas;
 * con DEMO_DELAY=0 las pruebas corren sin pausas.
 */
public class Pause implements Interaction {
    private final long millis;

    private Pause(long millis) { this.millis = millis; }

    public static Pause of(long millis) { return new Pause(millis); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String env = System.getenv("DEMO_DELAY");
        long ms = (env != null) ? Long.parseLong(env) : millis;
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
