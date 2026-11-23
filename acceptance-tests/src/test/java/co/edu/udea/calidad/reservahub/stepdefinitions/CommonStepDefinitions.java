package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.questions.CurrentPage;
import co.edu.udea.calidad.reservahub.questions.TextOf;
import co.edu.udea.calidad.reservahub.questions.ToastMessage;
import co.edu.udea.calidad.reservahub.tasks.Logout;
import co.edu.udea.calidad.reservahub.tasks.NavigateTo;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;

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

    @Cuando("va a la sección {string}")
    public void goToSection(String section) {
        theActorInTheSpotlight().attemptsTo(NavigateTo.section(section));
    }

    @Cuando("abre directamente la página {string}")
    public void openPageDirectly(String path) {
        theActorInTheSpotlight().attemptsTo(OpenPage.at(path));
    }

    @Cuando("cierra la sesión")
    public void logout() {
        theActorInTheSpotlight().attemptsTo(Logout.now());
    }

    @Entonces("debería estar en la página {string}")
    public void shouldBeOnPage(String path) {
        theActorInTheSpotlight().should(seeThat(CurrentPage.afterNavigatingTo(path), equalTo(path)));
    }

    @Entonces("debería ver el título {string}")
    public void shouldSeeTitle(String title) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(PAGE_TITLE), equalTo(title)));
    }

    @Entonces("debería ver el mensaje de error {string}")
    public void shouldSeeErrorMessage(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(ERROR_MESSAGE), equalTo(message)));
    }

    @Entonces("debería ver en la ventana el mensaje de error {string}")
    public void shouldSeeDialogErrorMessage(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(DIALOG_ERROR), equalTo(message)));
    }

    @Entonces("debería ver el error de validación {string}")
    public void shouldSeeFieldError(String message) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(FIELD_ERROR), equalTo(message)));
    }

    @Entonces("debería ver la notificación {string}")
    public void shouldSeeToast(String message) {
        theActorInTheSpotlight().should(seeThat(ToastMessage.shown(), containsString(message)));
    }

    @Entonces("debería ver el aviso vacío {string}")
    public void shouldSeeEmptyState(String title) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(EMPTY_STATE_TITLE), equalTo(title)));
    }

    @Entonces("debería ver el rol {string} en la barra superior")
    public void shouldSeeRole(String role) {
        theActorInTheSpotlight().should(seeThat(TextOf.the(USER_ROLE), equalTo(role)));
    }
}
