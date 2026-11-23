package co.edu.udea.calidad.reservahub.models;

import co.edu.udea.calidad.reservahub.utils.TestData;

/** Un servicio del catálogo (HU-05). El id se completa cuando el servicio existe en la API. */
public class ServiceInfo {
    public final String name;
    public final String description;
    public final int durationMinutes;
    public final int price;
    public final String id;

    private ServiceInfo(String name, String description, int durationMinutes, int price, String id) {
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.id = id;
    }

    /** "Corte" -> "Corte mx3k9a-7", único por escenario. */
    public static ServiceInfo named(String baseName) {
        return new ServiceInfo(baseName + " " + TestData.unique(), "Servicio de prueba Screenplay", 30, 25000, null);
    }

    public ServiceInfo withPrice(int newPrice) {
        return new ServiceInfo(name, description, durationMinutes, newPrice, id);
    }

    public ServiceInfo withId(String newId) {
        return new ServiceInfo(name, description, durationMinutes, price, newId);
    }

    /** Así aparece en el selector de "Reservar a cliente": "Corte xxx · 30 min". */
    public String optionLabel() {
        return name + " · " + durationMinutes + " min";
    }
}
