import { useState } from 'react';
import { reportsApi } from '../../api/endpoints';
import type { DateRange } from '../../api/types';
import { HistoryTable } from '../../components/reservations/HistoryTable';
import { Alert, Card, DateRangeFilter, PageHeader, PageLoader } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { addDays, todayIso } from '../../utils/format';

/** HU-17 (cliente, /reports/my-reservations) y HU-18 (proveedor, /reports/business-reservations). */
export function HistoryPage({ perspective }: { perspective: 'client' | 'provider' }) {
  const today = todayIso();
  const [range, setRange] = useState<DateRange>({ from: addDays(today, -15), to: addDays(today, 15) });
  const isProvider = perspective === 'provider';

  const { data, error, loading, reload } = useAsync(
    () => (isProvider ? reportsApi.businessReservations(range) : reportsApi.myReservations(range)),
    [isProvider, range.from, range.to],
  );

  return (
    <>
      <PageHeader
        tag={isProvider ? 'Proveedor · HU-18' : 'Cliente · HU-17'}
        title={isProvider ? 'Historial del negocio' : 'Historial de reservas'}
        description="Reservas pasadas y futuras, incluidas las canceladas y completadas, con su ciclo de vida."
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
      {!loading && data && (
        <Card title={`${data.length} reserva${data.length === 1 ? '' : 's'} en el periodo`}>
          <HistoryTable items={data} showClient={isProvider} />
        </Card>
      )}
    </>
  );
}
