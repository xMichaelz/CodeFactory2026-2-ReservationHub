package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/history (HU-18): historial de reservas del negocio. */
public class BusinessHistoryUI {
    private static final String ROW = "//tr[@data-testid='history-row'][td[4][normalize-space()='{0}']]";

    public static final Target HISTORY_ROWS = Target.the("filas del historial").locatedBy("css:[data-testid='history-row']");
    public static final Target LIFECYCLE = Target.the("ciclo de vida de la reserva de {0}")
            .locatedBy(ROW + "/td[5]//span[contains(@class,'badge')]");
    public static final Target SAVED_STATUS = Target.the("estado guardado de la reserva de {0}")
            .locatedBy(ROW + "/td[6]//span[contains(@class,'badge')]");

    private BusinessHistoryUI() { }
}
