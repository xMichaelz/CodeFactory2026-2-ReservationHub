package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.Pages;
import io.cucumber.java.Before;
import net.serenitybdd.screenplay.Ability;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;

/**
 * Prepara el escenario. Cada actor puede navegar (BrowseTheWeb, de OnlineCast) y llamar a la API
 * (CallAnApi) para preparar datos. Actores: Actors.CLIENT, Actors.PROVIDER, Actors.OTHER_CLIENT.
 */
public class Hooks {
    @Before
    public void setTheStage() {
        OnStage.setTheStage(new OnlineCast() {
            @Override
            public Actor actorNamed(String actorName, Ability... abilities) {
                Actor actor = super.actorNamed(actorName, abilities);
                if (actor.abilityTo(CallAnApi.class) == null) {
                    actor.can(CallAnApi.at(Pages.BASE_URL));
                    ActorMemory.reset(actor);
                }
                return actor;
            }
        });
    }
}
