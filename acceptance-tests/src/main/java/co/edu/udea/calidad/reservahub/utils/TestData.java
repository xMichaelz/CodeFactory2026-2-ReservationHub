package co.edu.udea.calidad.reservahub.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/** Datos únicos por ejecución: así los escenarios se pueden repetir sobre la misma base de datos. */
public final class TestData {
    public static final String PASSWORD = "Secreta123";
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private static final String RUN = Long.toString(System.currentTimeMillis(), 36);
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private TestData() { }

    /** Sufijo único (ejecución + contador), p. ej. "mx3k9a-7". */
    public static String unique() {
        return RUN + "-" + SEQUENCE.incrementAndGet();
    }

    public static String emailFor(String prefix) {
        return prefix + "-" + unique() + "@screenplay.test";
    }

    public static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
