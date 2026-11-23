import { useState } from 'react';
import { Link } from 'react-router-dom';
import { reportsApi, reservationsApi } from '../../api/endpoints';
import type { DateRange, ReservationResponse } from '../../api/types';
import { CancelReservationModal } from '../../components/reservations/CancelReservationModal';
import { ReservationTable } from '../../components/reservations/ReservationTable';
import { RescheduleReservationModal } from '../../components/reservations/RescheduleReservationModal';
import { Alert, Card, DateRangeFilter, EmptyState, PageHeader, PageLoader } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync } from '../../hooks/useAsync';
import { addDays, todayIso } from '../../utils/format';

/**
 * GET /reservations/mine: para el cliente son sus reservas, para el proveedor su agenda.
 * Desde aquí se cancela (HU-14/HU-15) y se reprograma (HU-16).
 */
export function ReservationsPage({ perspective }: { perspective: 'client' | 'provider' }) {
  const toast = useToast();
  const today = todayIso();
  const [range, setRange] = useState<DateRange>({ from: today, to: addDays(today, 30) });
  const [toCancel, setToCancel] = useState<ReservationResponse | null>(null);
  const [toReschedule, setToReschedule] = useState<ReservationResponse | null>(null);

  const isProvider = perspective === 'provider';
  const { data, error, loading, reload } = useAsync(() => reservationsApi.mine(range), [range.from, range.to]);
  // /reservations/mine no trae el nombre del cliente; el historial del negocio (HU-18) sí.
  const names = useAsync(
    async () => {
      const history = await reportsApi.businessReservations(range);
      return Object.fromEntries(history.map((h) => [h.clientId, h.clientName]));
    },
    [range.from, range.to],
    isProvider,
  );

  return (
    <>
      <PageHeader
        tag={isProvider ? 'Proveedor' : 'Cliente'}
        title={isProvider ? 'Agenda de reservas' : 'Mis reservas'}
        description={
          isProvider
            ? 'Todas las reservas de tu negocio en el rango elegido. Puedes cancelarlas (con motivo) o moverlas.'
            : 'Tus reservas en el rango elegido. Las futuras se pueden cancelar o reprogramar.'
        }
        actions={
          <Link className="btn btn--primary" to={isProvider ? '/provider/book' : '/client/catalog'}>
            {isProvider ? 'Reservar a un cliente' : 'Nueva reserva'}
          </Link>
        }
      />
      <Card>
        <DateRangeFilter value={range} onChange={setRange} loading={loading} />
      </Card>
      {loading && <PageLoader />}
      {error && (
        <Alert kind="error" onRetry={reload}>
          {error}
        </Alert>
      )}
      {!loading && data && data.length === 0 && (
        <EmptyState icon="📅" title="No hay reservas en este rango">
          Cambia las fechas para ver otras reservas.
        </EmptyState>
      )}
      {!loading && data && data.length > 0 && (
        <Card title={`${data.length} reserva${data.length === 1 ? '' : 's'}`}>
          <ReservationTable
            reservations={data}
            perspective={perspective}
            clientNames={names.data}
            onCancel={setToCancel}
            onReschedule={setToReschedule}
          />
        </Card>
      )}

      <CancelReservationModal
        reservation={toCancel}
        asProvider={isProvider}
        onClose={() => setToCancel(null)}
        onCancelled={() => {
          setToCancel(null);
          toast.success('Reserva cancelada.');
          void reload();
        }}
      />
      <RescheduleReservationModal
        reservation={toReschedule}
        onClose={() => setToReschedule(null)}
        onRescheduled={() => {
          setToReschedule(null);
          toast.success('Reserva reprogramada.');
          void reload();
        }}
      />
    </>
  );
}
