package co.edu.udea.calidad.reservahub.models;

/** Una reserva creada durante el escenario (por API o por la UI). */
public class Reservation {
    public final String id;
    public final String serviceName;
    public final String date;
    public final String startTime;

    public Reservation(String id, String serviceName, String date, String startTime) {
        this.id = id;
        this.serviceName = serviceName;
        this.date = date;
        this.startTime = startTime;
    }

    /** "09:00:00" -> "09:00", como lo muestra la UI. */
    public String shortStartTime() {
        return startTime.length() >= 5 ? startTime.substring(0, 5) : startTime;
    }
}
