package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_RESULT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_SEARCH;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** Abre el catálogo desde el menú y escribe en el buscador (servicio, proveedor o descripción). */
public class SearchCatalog implements Task {
    private final String text;

    public SearchCatalog(String text) {
        this.text = text;
    }

    public static SearchCatalog forText(String text) {
        return Tasks.instrumented(SearchCatalog.class, text);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                OpenClientSection.called("Catálogo"),
                WaitUntil.the(CATALOG_RESULT, isVisible()).forNoMoreThan(10).seconds(),
                Enter.theValue(text).into(CATALOG_SEARCH),
                Pause.of(300));
    }
}
