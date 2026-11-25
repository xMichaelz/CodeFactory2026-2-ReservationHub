package co.edu.udea.calidad.reservahub.exceptions;

/**
 * Estado faltante o inválido en la memoria del actor durante la ejecución de un escenario.
 */
public class ActorStateException extends IllegalStateException {
    public ActorStateException(String message) {
        super(message);
    }
}
