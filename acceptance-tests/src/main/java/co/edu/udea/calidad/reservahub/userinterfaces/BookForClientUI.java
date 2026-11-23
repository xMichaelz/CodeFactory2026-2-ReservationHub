package co.edu.udea.calidad.reservahub.userinterfaces;

import net.serenitybdd.screenplay.targets.Target;

/** /provider/book (HU-13): reservar a nombre de un cliente buscado por correo. */
public class BookForClientUI {
    public static final Target SERVICE = Target.the("servicio a reservar").locatedBy("css:select[name='serviceId']");
    public static final Target CLIENT_EMAIL = Target.the("correo del cliente").locatedBy("css:input[name='clientEmail']");
    public static final Target SEARCH_BUTTON = Target.the("botón buscar cliente")
            .locatedBy("//button[@type='submit' and normalize-space()='Buscar']");
    public static final Target CLIENT_FOUND = Target.the("cliente encontrado").locatedBy("css:[data-testid='client-name']");
    public static final Target NEXT_WEEK = Target.the("botón semana siguiente")
            .locatedBy("//div[@data-testid='slot-picker']//button[normalize-space()='Siguiente →']");
    public static final Target CREATE_BUTTON = Target.the("botón crear reserva")
            .locatedBy("//button[normalize-space()='Crear reserva']");

    // Confirmación
    public static final Target CONFIRMED_CLIENT = Target.the("cliente de la reserva creada")
            .locatedBy("//div[@data-testid='booking-confirmation']//dt[normalize-space()='Cliente']/following-sibling::dd[1]");
    public static final Target SEE_AGENDA_LINK = Target.the("enlace ver agenda")
            .locatedBy("//div[@data-testid='booking-confirmation']//a[normalize-space()='Ver agenda']");

    private BookForClientUI() { }
}
