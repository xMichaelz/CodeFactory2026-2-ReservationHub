package co.edu.udea.calidad.reservahub.tasks;

import co.edu.udea.calidad.reservahub.interactions.OpenCleanSession;
import co.edu.udea.calidad.reservahub.interactions.OpenPage;
import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.Pages;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;

import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.ADDRESS;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.BUSINESS_NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.CONFIRM_PASSWORD;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.EMAIL;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.FIRST_NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.LAST_NAME;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.PASSWORD;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.REGISTER_BUSINESS_BUTTON;
import static co.edu.udea.calidad.reservahub.userinterfaces.AccessUI.SERVICE_TYPE;

/** HU-02: registro de proveedor y su negocio por la UI. */
public class RegisterAsProvider implements Task {
    private final ProviderAccount account;

    public RegisterAsProvider(ProviderAccount account) {
        this.account = account;
    }

    public static RegisterAsProvider with(ProviderAccount account) {
        return Tasks.instrumented(RegisterAsProvider.class, account);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                OpenCleanSession.now(),
                OpenPage.at(Pages.REGISTER_PROVIDER),
                Enter.theValue(account.firstName).into(FIRST_NAME),
                Enter.theValue(account.lastName).into(LAST_NAME),
                Enter.theValue(account.email).into(EMAIL),
                Enter.theValue(account.password).into(PASSWORD),
                Enter.theValue(account.password).into(CONFIRM_PASSWORD),
                Enter.theValue(account.businessName).into(BUSINESS_NAME),
                Enter.theValue(account.serviceType).into(SERVICE_TYPE),
                Enter.theValue(account.address).into(ADDRESS),
                Pause.of(300),
                Click.on(REGISTER_BUSINESS_BUTTON));
        ActorMemory.rememberProvider(actor, account);
    }
}
