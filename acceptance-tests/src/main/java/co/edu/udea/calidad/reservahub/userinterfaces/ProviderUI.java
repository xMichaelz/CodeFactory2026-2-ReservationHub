package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** Elementos comunes del área del proveedor: menú, tarjetas, estadísticas y el panel del negocio. */
public class ProviderUI {
    // Menú lateral: el enlace lleva un ícono antes del texto, por eso se busca con contains()
    public static final Target SECTION_LINK = Target.the("sección {0} del menú")
            .locatedBy("//nav[@aria-label='Principal']//a[contains(normalize-space(),'{0}')]");
    public static final Target PAGE_LOADER = Target.the("indicador de carga").locatedBy("css:.page-loader");

    // Tarjetas y estadísticas (panel y reportes)
    public static final Target CARD_TITLE = Target.the("tarjeta {0}")
            .locatedBy("//section[contains(@class,'card')]//h2[contains(@class,'card__title') and normalize-space()='{0}']");
    public static final Target STAT_VALUE = Target.the("valor de la estadística {0}")
            .locatedBy("//div[contains(@class,'stat-card')][span[contains(@class,'stat-card__label') and normalize-space()='{0}']]"
                    + "/strong[contains(@class,'stat-card__value')]");

    // Panel: lista "Completa tu configuración"
    public static final Target PENDING_STEP = Target.the("paso pendiente {0}")
            .locatedBy("//li[contains(@class,'checklist__item') and not(contains(@class,'checklist__item--done'))]"
                    + "//a[normalize-space()='{0}']");

    private ProviderUI() { }
}
