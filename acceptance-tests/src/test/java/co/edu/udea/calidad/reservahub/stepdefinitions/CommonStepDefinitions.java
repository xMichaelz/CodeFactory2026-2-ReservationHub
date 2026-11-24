package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.questions.CurrentPage;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.ToastMessage;
import co.edu.udea.calidad.reservahub.tasks.Logout;
import co.edu.udea.calidad.reservahub.tasks.NavigateTo;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.DIALOG_ERROR;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.EMPTY_STATE_TITLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.ERROR_MESSAGE;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.FIELD_ERROR;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.PAGE_TITLE;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.USER_ROLE;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Pasos que sirven en cualquier área. Los "Entonces" los responde el actor del foco: el último
 * que actuó en el escenario.
 */
public class CommonStepDefinitions {

    @When("they go to the section {string}")
    public void goToSection(String section) {
        theActorInTheSpotlight().attemptsTo(NavigateTo.section(section));
    }

    @When("they open the page {string} directly")
    public void openPageDirectly(String path) {
        theActorInTheSpotlight().attemptsTo(OpenPage.at(path));
    }

    @When("they log out")
    public void logout() {
        theActorInTheSpotlight().attemptsTo(Logout.now());
    }

    @Then("they should be on the page {string}")
    public void shouldBeOnPage(String path) {
        theActorInTheSpotlight().should(seeThat(CurrentPage.afterNavigatingTo(path), equalTo(path)));
    }

    @Then("they should see the title {string}")
    public void shouldSeeTitle(String title) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(PAGE_TITLE), equalTo(title)));
    }

    @Then("they should see the error message {string}")
    public void shouldSeeErrorMessage(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(ERROR_MESSAGE), equalTo(message)));
    }

    @Then("they should see the error message {string} in the dialog")
    public void shouldSeeDialogErrorMessage(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(DIALOG_ERROR), equalTo(message)));
    }

    @Then("they should see the validation error {string}")
    public void shouldSeeFieldError(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(FIELD_ERROR), equalTo(message)));
    }

    @Then("they should see the notification {string}")
    public void shouldSeeToast(String message) {
        theActorInTheSpotlight().should(seeThat(ToastMessage.shown(), containsString(message)));
    }

    @Then("they should see the empty-state notice {string}")
    public void shouldSeeEmptyState(String title) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(EMPTY_STATE_TITLE), equalTo(title)));
    }

    @Then("they should see the role {string} in the top bar")
    public void shouldSeeRole(String role) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(USER_ROLE), equalTo(role)));
    }
}
