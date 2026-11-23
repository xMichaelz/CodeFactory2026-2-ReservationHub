package co.edu.udea.calidad.reservahub.stepdefinitions;

import co.edu.udea.calidad.reservahub.models.ClientAccount;
import co.edu.udea.calidad.reservahub.models.ProviderAccount;
import co.edu.udea.calidad.reservahub.tasks.api.BookSlotOnDateThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.CreateScheduleThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.RegisterClientThroughApi;
import co.edu.udea.calidad.reservahub.tasks.api.RegisterProviderThroughApi;
import co.edu.udea.calidad.reservahub.utils.ActorMemory;
import co.edu.udea.calidad.reservahub.utils.TestData;
import co.edu.udea.calidad.reservahub.utils.WeekDays;
import io.cucumber.java.es.Dado;
import net.serenitybdd.screenplay.Actor;

/** "Dado" adicionales del área del proveedor. Igual que los generales, preparan los datos por API. */
public class ProviderPreconditionStepDefinitions {

    @Dado("que el proveedor tiene una franja el {string} de {string} a {string}")
    public void providerHasScheduleWindow(String day, String startTime, String endTime) {
        Actor provider = Actors.provider();
        if (!ActorMemory.hasProviderAccount(provider)) {
            provider.attemptsTo(RegisterProviderThroughApi.with(ProviderAccount.random()));
        }
        provider.attemptsTo(CreateScheduleThroughApi.on(WeekDays.apiValueOf(day), startTime, endTime));
    }

    /**
     * El cliente reserva el primer hueco de mañana. A diferencia de "el cliente tiene una reserva",
     * la reserva sigue siendo futura todo el escenario, así que se puede cancelar o reprogramar.
     */
    @Dado("que el proveedor tiene una cita del cliente mañana para {string}")
    public void providerHasAppointmentTomorrow(String serviceName) {
        Actor client = Actors.client();
        if (!ActorMemory.hasClientAccount(client)) {
            client.attemptsTo(RegisterClientThroughApi.with(ClientAccount.random()));
        }
        client.attemptsTo(BookSlotOnDateThroughApi.of(
                PreconditionStepDefinitions.serviceOf(Actors.provider(), serviceName),
                TestData.today().plusDays(1).toString()));
    }
}
