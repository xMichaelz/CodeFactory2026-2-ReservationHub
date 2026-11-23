package co.edu.udea.calidad.reservahub.tasks.provider;

import co.edu.udea.calidad.reservahub.interactions.Pause;
import co.edu.udea.calidad.reservahub.interactions.WaitUntilLoaded;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.Tasks;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.SelectFromOptions;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import java.util.Map;

import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.CANCELLATIONS_REPORT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.DEMAND_REPORT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.OCCUPANCY_REPORT;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.SERVICE_FILTER;
import static co.edu.udea.calidad.reservahub.userinterfaces.ReportsUI.TAB;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

/**
 * HU-19/20/21: abre una pestaña de Reportes ("Ocupación", "Demanda" o "Cancelaciones") con el rango
 * por defecto (hoy ±15 días) y, si se pide, filtra por un servicio. Espera a que el reporte cargue.
 */
public class ShowReport implements Task {
    private static final Map<String, Target> REPORTS = Map.of(
            "Ocupación", OCCUPANCY_REPORT,
            "Demanda", DEMAND_REPORT,
            "Cancelaciones", CANCELLATIONS_REPORT);

    private final String tab;
    private final String serviceName;

    public ShowReport(String tab, String serviceName) {
        this.tab = tab;
        this.serviceName = serviceName;
    }

    public static ShowReport tab(String tab) {
        return Tasks.instrumented(ShowReport.class, tab, "");
    }

    public static ShowReport tabFilteredBy(String tab, String serviceName) {
        return Tasks.instrumented(ShowReport.class, tab, serviceName);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Target report = REPORTS.get(tab);
        if (report == null) {
            throw new IllegalArgumentException("Pestaña de reportes desconocida: " + tab);
        }
        actor.attemptsTo(Click.on(TAB.of(tab)));
        if (!serviceName.isEmpty()) {
            actor.attemptsTo(SelectFromOptions.byVisibleText(serviceName).from(SERVICE_FILTER));
        }
        actor.attemptsTo(
                WaitUntilLoaded.page(),
                WaitUntil.the(report, isVisible()).forNoMoreThan(10).seconds(),
                Pause.of(300));
    }
}
