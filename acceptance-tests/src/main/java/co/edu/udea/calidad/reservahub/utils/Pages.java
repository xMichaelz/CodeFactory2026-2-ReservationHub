package co.edu.udea.calidad.reservahub.utils;

/** Rutas del frontend de ReservaHub. La base se cambia con -Dreservahub.url=... */
public final class Pages {
    public static final String BASE_URL = System.getProperty("reservahub.url", "http://localhost:3001")
            .replaceAll("/$", "");

    public static final String HOME = BASE_URL + "/";
    public static final String LOGIN = "/login";
    public static final String REGISTER_CLIENT = "/register";
    public static final String REGISTER_PROVIDER = "/register/provider";

    public static final String CLIENT_HOME = "/client";
    public static final String CATALOG = "/client/catalog";
    public static final String MY_RESERVATIONS = "/client/reservations";
    public static final String CLIENT_HISTORY = "/client/history";

    public static final String PROVIDER_HOME = "/provider";
    public static final String SERVICES = "/provider/services";
    public static final String RESOURCES = "/provider/resources";
    public static final String SCHEDULES = "/provider/schedules";
    public static final String AGENDA = "/provider/agenda";
    public static final String BOOK_FOR_CLIENT = "/provider/book";
    public static final String PROVIDER_HISTORY = "/provider/history";
    public static final String REPORTS = "/provider/reports";

    private Pages() { }

    public static String url(String path) {
        return BASE_URL + path;
    }
}
