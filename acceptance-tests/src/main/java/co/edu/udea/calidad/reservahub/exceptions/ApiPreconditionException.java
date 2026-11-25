package co.edu.udea.calidad.reservahub.exceptions;

/**
 * Fallo al preparar precondiciones de un escenario a través de la API REST.
 */
public class ApiPreconditionException extends AssertionError {
    public ApiPreconditionException(String action, int statusCode, String responseBody) {
        super(String.format("No se pudo %s: HTTP %d %s", action, statusCode, responseBody));
    }
}
