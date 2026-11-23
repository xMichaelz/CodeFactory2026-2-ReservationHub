package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.waits.WaitUntil;

import java.util.Map;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.ProviderUI.SECTION_LINK;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsText;

/**
 * Abre una sección del proveedor desde el menú lateral y espera a que la página termine de cargar:
 * GoToSection.called("Servicios").
 */
public class GoToSection implements Task {
    private static final Map<String, String> TITLES = Map.of(
            "Panel", "Panel del negocio",
            "Servicios", "Servicios",
            "Recursos", "Recursos",
            "Horarios", "Horarios de atención",
            "Agenda", "Agenda de reservas",
            "Reservar a cliente", "Reservar a nombre de un cliente",
            "Historial", "Historial del negocio",
            "Reportes", "Reportes");

    private final String section;

    public GoToSection(String section) {
        this.section = section;
    }

    public static GoToSection called(String section) {
        if (!TITLES.containsKey(section)) {
            throw new IllegalArgumentException("Sección del proveedor desconocida: " + section);
        }
        return Tasks.instrumented(GoToSection.class, section);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(SECTION_LINK.of(section)),
                WaitUntil.the(PAGE_TITLE, containsText(TITLES.get(section))).forNoMoreThan(10).seconds(),
                WaitUntilLoaded.page(),
                Pause.of(300));
    }
}
