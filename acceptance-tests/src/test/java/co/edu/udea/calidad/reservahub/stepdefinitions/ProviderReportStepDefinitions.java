package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.TextsOf;
import co.edu.udea.calidad.reservahub.tasks.provider.ChangeDateRange;
import co.edu.udea.calidad.reservahub.tasks.provider.GoToSection;
import co.edu.udea.calidad.reservahub.tasks.provider.ShowReport;
import co.edu.udea.calidad.reservahub.utils.TestData;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
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

    @When("the provider views the {string} report")
    public void providerOpensReport(String tab) {
        Actors.provider().attemptsTo(GoToSection.called("Reportes"), ShowReport.tab(tab));
    }

    @When("the provider views the {string} report for the service {string} only")
    public void providerOpensReportForService(String tab, String serviceBaseName) {
        Actor provider = Actors.provider();
        provider.attemptsTo(
                GoToSection.called("Reportes"),
                ShowReport.tabFilteredBy(tab, PreconditionStepDefinitions.serviceOf(provider, serviceBaseName).name));
    }

    @When("the provider views the reports for the next {int} days")
    public void providerChangesRange(int days) {
        LocalDate today = TestData.today();
        Actors.provider().attemptsTo(
                GoToSection.called("Reportes"),
                ChangeDateRange.between(today, today.plusDays(days - 1L)));
    }

    @Then("the provider should see the service {string} in the card {string}")
    public void providerShouldSeeServiceInCard(String serviceBaseName, String card) {
        Actor provider = Actors.provider();
        provider.should(seeThat(TextsOf.each(BAR_LABELS.of(card)), hasItem(serviceName(serviceBaseName))));
    }

    @Then("the provider should not see the service {string} in the card {string}")
    public void providerShouldNotSeeServiceInCard(String serviceBaseName, String card) {
        Actor provider = Actors.provider();
        provider.should(seeThat(TextsOf.each(BAR_LABELS.of(card)), not(hasItem(serviceName(serviceBaseName)))));
    }

    @Then("the provider should see for the service {string} the value {string} in the card {string}")
    public void providerShouldSeeBarValue(String serviceBaseName, String value, String card) {
        Actors.provider().should(seeThat(TextOf.the(BAR_VALUE.of(card, serviceName(serviceBaseName))), equalTo(value)));
    }

    private static String serviceName(String baseName) {
        return PreconditionStepDefinitions.serviceOf(Actors.provider(), baseName).name;
    }
}
