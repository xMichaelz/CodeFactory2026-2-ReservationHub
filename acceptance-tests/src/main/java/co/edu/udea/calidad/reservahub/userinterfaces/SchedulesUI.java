package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/schedules: formulario "Nueva franja", semana con las franjas y modal de bloqueo. */
public class SchedulesUI {
    private static final String WINDOWS_OF_DAY = "//div[contains(@class,'week__day')][h4[normalize-space()='{0}']]"
            + "//div[@data-testid='schedule-window']";

    // Formulario de la página (fuera del modal)
    public static final Target DAY = Target.the("día de la franja").locatedBy("css:.split-layout select[name='dayOfWeek']");
    public static final Target START = Target.the("hora de inicio de la franja")
            .locatedBy("css:.split-layout input[name='startTime']");
    public static final Target END = Target.the("hora de fin de la franja")
            .locatedBy("css:.split-layout input[name='endTime']");
    public static final Target ADD_WINDOW_BUTTON = Target.the("botón agregar franja")
            .locatedBy("//button[@type='submit' and normalize-space()='Agregar franja']");

    // Semana
    public static final Target WINDOW_HOURS = Target.the("horas de las franjas del {0}")
            .locatedBy(WINDOWS_OF_DAY + "//strong");
    public static final Target BLOCK_BUTTON = Target.the("botón bloqueo de la franja del {0}")
            .locatedBy(WINDOWS_OF_DAY + "//button[normalize-space()='+ Bloqueo']");
    public static final Target BLOCKS = Target.the("bloqueos de la franja del {0}")
            .locatedBy(WINDOWS_OF_DAY + "//ul[contains(@class,'window__blocks')]/li");

    // Modal de bloqueo
    public static final Target BLOCK_START = Target.the("hora de inicio del bloqueo")
            .locatedBy("css:[role='dialog'] input[name='startTime']");
    public static final Target BLOCK_END = Target.the("hora de fin del bloqueo")
            .locatedBy("css:[role='dialog'] input[name='endTime']");
    public static final Target BLOCK_REASON = Target.the("motivo del bloqueo")
            .locatedBy("css:[role='dialog'] input[name='reason']");
    public static final Target REGISTER_BLOCK_BUTTON = Target.the("botón registrar bloqueo")
            .locatedBy("//div[@role='dialog']//button[normalize-space()='Registrar bloqueo']");

    private SchedulesUI() { }
}
