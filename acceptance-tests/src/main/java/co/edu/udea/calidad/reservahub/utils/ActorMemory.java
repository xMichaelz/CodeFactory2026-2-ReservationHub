package co.edu.udea.calidad.reservahub.utils;

import co.edu.udea.calidad.reservahub.exceptions.ActorStateException;
import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.models.Reservation;
import co.edu.udea.calidad.reservahub.models.ServiceInfo;
import net.serenitybdd.screenplay.Actor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que cada actor recuerda durante un escenario: su cuenta, su token, los servicios que creó
 * y las reservas que hizo. Equivale a CartMemory del proyecto de referencia.
 */
public final class ActorMemory {
    private static final String CLIENT_ACCOUNT = "clientAccount";
    private static final String PROVIDER_ACCOUNT = "providerAccount";
    private static final String TOKEN = "accessToken";
    private static final String SERVICES = "services";
    private static final String RESERVATIONS = "reservations";
    private static final String LAST = "lastValue";

    private ActorMemory() { }

    public static void reset(Actor actor) {
        actor.forget(CLIENT_ACCOUNT);
        actor.forget(PROVIDER_ACCOUNT);
        actor.forget(TOKEN);
        actor.remember(SERVICES, new LinkedHashMap<String, ServiceInfo>());
        actor.remember(RESERVATIONS, new ArrayList<Reservation>());
        actor.forget(LAST);
    }

    // ---------- cuentas ----------
    public static void rememberClient(Actor actor, ClientAccount account) {
        actor.remember(CLIENT_ACCOUNT, account);
    }

    public static ClientAccount client(Actor actor) {
        ClientAccount account = actor.recall(CLIENT_ACCOUNT);
        if (account == null) {
            throw new ActorStateException(actor.getName() + " no tiene una cuenta de cliente");
        }
        return account;
    }

    public static void rememberProvider(Actor actor, ProviderAccount account) {
        actor.remember(PROVIDER_ACCOUNT, account);
    }

    public static ProviderAccount provider(Actor actor) {
        ProviderAccount account = actor.recall(PROVIDER_ACCOUNT);
        if (account == null) {
            throw new ActorStateException(actor.getName() + " no tiene una cuenta de proveedor");
        }
        return account;
    }

    public static boolean hasClientAccount(Actor actor) {
        return actor.recall(CLIENT_ACCOUNT) != null;
    }

    public static boolean hasProviderAccount(Actor actor) {
        return actor.recall(PROVIDER_ACCOUNT) != null;
    }

    /** Correo y contraseña de la cuenta del actor, sea cliente o proveedor. */
    public static String[] credentials(Actor actor) {
        ProviderAccount provider = actor.recall(PROVIDER_ACCOUNT);
        if (provider != null) {
            return new String[] {provider.email, provider.password};
        }
        ClientAccount client = client(actor);
        return new String[] {client.email, client.password};
    }

    // ---------- token de la API ----------
    public static void rememberToken(Actor actor, String token) {
        actor.remember(TOKEN, token);
    }

    public static String token(Actor actor) {
        String token = actor.recall(TOKEN);
        if (token == null) {
            throw new ActorStateException(actor.getName() + " no ha iniciado sesión en la API");
        }
        return token;
    }

    // ---------- servicios ----------
    public static void rememberService(Actor actor, ServiceInfo service) {
        Map<String, ServiceInfo> services = actor.recall(SERVICES);
        services.put(service.name, service);
        actor.remember(SERVICES, services);
    }

    public static ServiceInfo service(Actor actor, String name) {
        Map<String, ServiceInfo> services = actor.recall(SERVICES);
        ServiceInfo service = services.get(name);
        if (service == null) {
            throw new ActorStateException(actor.getName() + " no conoce el servicio " + name);
        }
        return service;
    }

    /** El primer servicio que creó el actor (el "servicio principal" del escenario). */
    public static ServiceInfo mainService(Actor actor) {
        Map<String, ServiceInfo> services = actor.recall(SERVICES);
        if (services.isEmpty()) {
            throw new ActorStateException(actor.getName() + " no ha creado servicios");
        }
        return services.values().iterator().next();
    }

    public static List<ServiceInfo> services(Actor actor) {
        Map<String, ServiceInfo> services = actor.recall(SERVICES);
        return new ArrayList<>(services.values());
    }

    // ---------- reservas ----------
    public static void rememberReservation(Actor actor, Reservation reservation) {
        List<Reservation> reservations = actor.recall(RESERVATIONS);
        reservations.add(reservation);
        actor.remember(RESERVATIONS, reservations);
    }

    public static int reservationsCount(Actor actor) {
        List<Reservation> reservations = actor.recall(RESERVATIONS);
        return reservations.size();
    }

    public static Reservation lastReservation(Actor actor) {
        List<Reservation> reservations = actor.recall(RESERVATIONS);
        if (reservations.isEmpty()) {
            throw new ActorStateException(actor.getName() + " no tiene reservas");
        }
        return reservations.get(reservations.size() - 1);
    }

    // ---------- valor suelto (p. ej. el texto de una fila antes de cambiarla) ----------
    public static void rememberValue(Actor actor, String value) {
        actor.remember(LAST, value);
    }

    public static String value(Actor actor) {
        return actor.recall(LAST);
    }
}
