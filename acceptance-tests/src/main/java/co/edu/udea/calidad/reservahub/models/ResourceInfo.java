package co.edu.udea.calidad.reservahub.models;

import co.edu.udea.calidad.reservahub.utils.TestData;

/** Un recurso del proveedor (HU-10). */
public class ResourceInfo {
    public final String name;
    public final String resourceType;
    public final int quantity;
    public final String description;

    private ResourceInfo(String name, String resourceType, int quantity, String description) {
        this.name = name;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.description = description;
    }

    /** "Silla" -> "Silla mx3k9a-7", único por escenario. */
    public static ResourceInfo named(String baseName) {
        return new ResourceInfo(baseName + " " + TestData.unique(), "Equipo", 2, "Recurso de prueba Screenplay");
    }

    /** El mismo nombre exacto (para probar duplicados). */
    public static ResourceInfo exactly(String name) {
        return new ResourceInfo(name, "Equipo", 1, "");
    }
}
