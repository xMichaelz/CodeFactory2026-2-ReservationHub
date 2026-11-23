package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;

import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.CLIENT_EMAIL;
import static co.edu.udea.calidad.reservahub.userinterfaces.BookForClientUI.SEARCH_BUTTON;

/** HU-13: busca un cliente por correo en "Reservar a cliente". */
public class LookUpClient implements Task {
    private final String email;

    public LookUpClient(String email) {
        this.email = email;
    }

    public static LookUpClient byEmail(String email) {
        return Tasks.instrumented(LookUpClient.class, email);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Enter.theValue(email).into(CLIENT_EMAIL),
                Pause.of(300),
                Click.on(SEARCH_BUTTON));
    }
}
