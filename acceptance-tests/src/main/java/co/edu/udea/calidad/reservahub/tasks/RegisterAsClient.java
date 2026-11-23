package co.edu.udea.calidad.reservahub.tasks;

import co.edu.udea.calidad.reservahub.interactions.OpenCleanSession;
import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.Pages;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;

import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.CONFIRM_PASSWORD;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.CREATE_ACCOUNT_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.EMAIL;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.FIRST_NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.LAST_NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.PASSWORD;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.PHONE;

/** HU-01: registro de cliente por la UI. El actor recuerda la cuenta para iniciar sesión después. */
public class RegisterAsClient implements Task {
    private final ClientAccount account;
    private final String confirmation;

    public RegisterAsClient(ClientAccount account, String confirmation) {
        this.account = account;
        this.confirmation = confirmation;
    }

    public static RegisterAsClient with(ClientAccount account) {
        return Tasks.instrumented(RegisterAsClient.class, account, account.password);
    }

    /** Escribe una confirmación de contraseña distinta. */
    public static RegisterAsClient withMismatchedConfirmation(ClientAccount account, String confirmation) {
        return Tasks.instrumented(RegisterAsClient.class, account, confirmation);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                OpenCleanSession.now(),
                OpenPage.at(Pages.REGISTER_CLIENT),
                Enter.theValue(account.firstName).into(FIRST_NAME),
                Enter.theValue(account.lastName).into(LAST_NAME),
                Enter.theValue(account.email).into(EMAIL),
                Enter.theValue(account.phone).into(PHONE),
                Enter.theValue(account.password).into(PASSWORD),
                Enter.theValue(confirmation).into(CONFIRM_PASSWORD),
                Pause.of(300),
                Click.on(CREATE_ACCOUNT_BUTTON));
        ActorMemory.rememberClient(actor, account);
    }
}
