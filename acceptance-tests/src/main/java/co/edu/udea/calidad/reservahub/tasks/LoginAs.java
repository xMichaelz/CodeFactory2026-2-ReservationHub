package co.edu.udea.calidad.reservahub.tasks;

import co.edu.udea.calidad.reservahub.interactions.OpenCleanSession;
import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.Pages;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.EMAIL;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.LOGIN_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.PASSWORD;
import static co.edu.udea.calidad.reservahub.userinterfaces.CommonUI.USER_CHIP;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/** HU-03: inicio de sesión por la UI, partiendo de una sesión limpia. */
public class LoginAs implements Task {
    private final String email;
    private final String password;
    private final boolean expectSuccess;

    public LoginAs(String email, String password, Boolean expectSuccess) {
        this.email = email;
        this.password = password;
        this.expectSuccess = expectSuccess;
    }

    /** Inicia sesión con la cuenta que el actor recuerda (cliente o proveedor) y espera entrar. */
    public static LoginAs himself() {
        return Tasks.instrumented(LoginAs.class, "", "", true);
    }

    /** Intenta iniciar sesión con credenciales concretas, sin exigir que funcione. */
    public static LoginAs withCredentials(String email, String password) {
        return Tasks.instrumented(LoginAs.class, email, password, false);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String user = email;
        String secret = password;
        if (user.isEmpty()) {
            String[] credentials = ActorMemory.credentials(actor);
            user = credentials[0];
            secret = credentials[1];
        }
        actor.attemptsTo(
                OpenCleanSession.now(),
                OpenPage.at(Pages.LOGIN),
                Enter.theValue(user).into(EMAIL),
                Enter.theValue(secret).into(PASSWORD),
                Pause.of(300),
                Click.on(LOGIN_BUTTON));
        if (expectSuccess) {
            actor.attemptsTo(WaitUntil.the(USER_CHIP, isVisible()).forNoMoreThan(10).seconds());
        }
    }
}
