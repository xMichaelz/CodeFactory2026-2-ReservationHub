package co.edu.udea.calidad.reservahub.tasks.api;

import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;

/** Utilidades comunes de las tareas de preparación por API. */
final class Api {
    static final String V1 = "/api/v1";

    private Api() { }

    /** Petición JSON sin autenticación (registro, login). */
    static RequestSpecification json(RequestSpecification request) {
        return request.contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    /** Petición JSON con el JWT que el actor obtuvo al iniciar sesión. */
    static RequestSpecification authenticated(Actor actor, RequestSpecification request) {
        return json(request).header("Authorization", "Bearer " + ActorMemory.token(actor));
    }

    /** Falla con un mensaje claro si la última respuesta no tiene el estado esperado. */
    static Response expect(int status, String action) {
        Response response = SerenityRest.lastResponse();
        if (response.statusCode() != status) {
            throw new AssertionError(String.format("No se pudo %s: HTTP %d %s", action, response.statusCode(),
                    response.asString()));
        }
        return response;
    }
}
