package co.edu.udea.calidad.reservahub.utils;

import java.util.Map;

/** Días de la semana como los muestra la UI ("Lunes") y como los espera la API ("MONDAY"). */
public final class WeekDays {
    private static final Map<String, String> API_VALUES = Map.of(
            "Lunes", "MONDAY",
            "Martes", "TUESDAY",
            "Miércoles", "WEDNESDAY",
            "Jueves", "THURSDAY",
            "Viernes", "FRIDAY",
            "Sábado", "SATURDAY",
            "Domingo", "SUNDAY");

    private WeekDays() { }

    public static String apiValueOf(String label) {
        String value = API_VALUES.get(label);
        if (value == null) {
            throw new IllegalArgumentException("Día desconocido: " + label);
        }
        return value;
    }
}
