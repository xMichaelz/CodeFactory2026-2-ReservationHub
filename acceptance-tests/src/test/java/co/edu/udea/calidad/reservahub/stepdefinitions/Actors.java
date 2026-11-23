package co.edu.udea.calidad.reservahub.stepdefinitions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;

/**
 * Los actores de los escenarios. Las frases "el cliente", "el proveedor", "otro cliente" y
 * "alguien" (un visitante sin cuenta) de los .feature se resuelven aquí; el actor elegido pasa a
 * ser el del foco (theActorInTheSpotlight), que es quien responde los pasos "Entonces".
 */
public final class Actors {
    public static final String CLIENT = "Carlos el cliente";
    public static final String OTHER_CLIENT = "Diana otra clienta";
    public static final String PROVIDER = "Paula la proveedora";
    public static final String VISITOR = "Un visitante";

    private Actors() { }

    public static Actor client() { return OnStage.theActorCalled(CLIENT); }

    public static Actor otherClient() { return OnStage.theActorCalled(OTHER_CLIENT); }

    public static Actor provider() { return OnStage.theActorCalled(PROVIDER); }

    public static Actor visitor() { return OnStage.theActorCalled(VISITOR); }
}
