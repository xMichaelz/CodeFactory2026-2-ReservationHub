package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.TextsOf;
import co.edu.udea.calidad.reservahub.tasks.provider.ChangeDateRange;
import co.edu.udea.calidad.reservahub.tasks.provider.GoToSection;
import co.edu.udea.calidad.reservahub.tasks.provider.ShowReport;
import co.edu.udea.calidad.reservahub.utils.TestData;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.screenplay.Actor;

import java.time.LocalDate;

import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.BAR_LABELS;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.BAR_VALUE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/** HU-19 ocupación, HU-20 demanda y HU-21 cancelaciones (rango por defecto: hoy ±15 días). */
public class ProviderReportStepDefinitions {

    @Cuando("el proveedor consulta el reporte de {string}")
    public void providerOpensReport(String tab) {
        Actors.provider().attemptsTo(GoToSection.called("Reportes"), ShowReport.tab(tab));
    }

    @Cuando("el proveedor consulta el reporte de {string} solo del servicio {string}")
    public void providerOpensReportForService(String tab, String serviceBaseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Reportes"),
                ShowReport.tabFilteredBy(tab, PreconditionStepDefinitions.serviceOf(provider, serviceBaseName).name));
    }

    @Cuando("el proveedor consulta los reportes de los próximos {int} días")
    public void providerChangesRange(int days) {
        LocalDate today = TestData.today();
        Actors.provider().attemptsTo(
                GoToSection.called("Reportes"),
                ChangeDateRange.between(today, today.plusDays(days - 1L)));
    }

    @Entonces("el proveedor debería ver el servicio {string} en la tarjeta {string}")
    public void providerShouldSeeServiceInCard(String serviceBaseName, String card) {
        Actor provider = Actors.provider();
        provider.should(seeThat(TextsOf.each(BAR_LABELS.of(card)), hasItem(serviceName(serviceBaseName))));
    }

    @Entonces("el proveedor no debería ver el servicio {string} en la tarjeta {string}")
    public void providerShouldNotSeeServiceInCard(String serviceBaseName, String card) {
        Actor provider = Actors.provider();
        provider.should(seeThat(TextsOf.each(BAR_LABELS.of(card)), not(hasItem(serviceName(serviceBaseName)))));
    }

    @Entonces("el proveedor debería ver para el servicio {string} el valor {string} en la tarjeta {string}")
    public void providerShouldSeeBarValue(String serviceBaseName, String value, String card) {
        Actors.provider().should(seeThat(TextOf.the(BAR_VALUE.of(card, serviceName(serviceBaseName))), equalTo(value)));
    }

    private static String serviceName(String baseName) {
        return PreconditionStepDefinitions.serviceOf(Actors.provider(), baseName).name;
    }
}
