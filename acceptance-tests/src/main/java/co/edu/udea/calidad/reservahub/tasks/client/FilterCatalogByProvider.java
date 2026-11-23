package co.edu.udea.calidad.reservahub.tasks.client;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.SelectFromOptions;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_PROVIDER_FILTER;
import static co.edu.udea.calidad.reservahub.userinterfaces.ClientUI.CATALOG_PROVIDER_OPTION;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;

/** Abre el catálogo y lo filtra por el nombre de un negocio en el selector de proveedores. */
public class FilterCatalogByProvider implements Task {
    private final String businessName;

    public FilterCatalogByProvider(String businessName) {
        this.businessName = businessName;
    }

    public static FilterCatalogByProvider named(String businessName) {
        return Tasks.instrumented(FilterCatalogByProvider.class, businessName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                OpenClientSection.called("Catálogo"),
                WaitUntil.the(CATALOG_PROVIDER_OPTION.of(businessName), isPresent()).forNoMoreThan(10).seconds(),
                SelectFromOptions.byVisibleText(businessName).from(CATALOG_PROVIDER_FILTER),
                Pause.of(300));
    }
}
