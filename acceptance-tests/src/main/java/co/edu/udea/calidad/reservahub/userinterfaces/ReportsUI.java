package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/reports: pestañas Ocupación (HU-19), Demanda (HU-20) y Cancelaciones (HU-21). */
public class ReportsUI {
    public static final Target TAB = Target.the("pestaña {0}")
            .locatedBy("//button[@role='tab'][starts-with(normalize-space(),'{0}')]");
    public static final Target OCCUPANCY_REPORT = Target.the("reporte de ocupación")
            .locatedBy("css:[data-testid='occupancy-report']");
    public static final Target DEMAND_REPORT = Target.the("reporte de demanda")
            .locatedBy("css:[data-testid='demand-report']");
    public static final Target CANCELLATIONS_REPORT = Target.the("reporte de cancelaciones")
            .locatedBy("css:[data-testid='cancellations-report']");

    public static final Target SERVICE_FILTER = Target.the("filtro por servicio").locatedBy("css:select[name='serviceId']");
    public static final Target CONSULT_BUTTON = Target.the("botón consultar")
            .locatedBy("//form[contains(@class,'range-filter')]//button[@type='submit']");

    /** Etiquetas de las barras dentro de la tarjeta {0} (p. ej. "Por servicio"). */
    public static final Target BAR_LABELS = Target.the("barras de la tarjeta {0}")
            .locatedBy("//section[.//h2[normalize-space()='{0}']]//span[contains(@class,'bar-list__label')]");
    public static final Target BAR_VALUE = Target.the("valor de la barra {1} en la tarjeta {0}")
            .locatedBy("//section[.//h2[normalize-space()='{0}']]"
                    + "//li[span[contains(@class,'bar-list__label') and normalize-space()='{1}']]"
                    + "/span[contains(@class,'bar-list__value')]");

    private ReportsUI() { }
}
