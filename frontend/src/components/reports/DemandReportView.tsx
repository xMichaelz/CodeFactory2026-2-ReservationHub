import { reportsApi } from '../../api/endpoints';
import type { DateRange } from '../../api/types';
import { useAsync } from '../../hooks/useAsync';
import { Alert, BarList, Card, PageLoader, StatCard } from '../ui';

/** HU-20: reservas confirmadas por servicio (las canceladas no cuentan). */
export function DemandReportView({ range }: { range: DateRange }) {
  const { data, error, loading, reload } = useAsync(() => reportsApi.demand(range), [range.from, range.to]);

  if (loading) return <PageLoader />;
  if (error || !data) {
    return (
      <Alert kind="error" onRetry={reload}>
        {error}
      </Alert>
    );
  }

  const sorted = [...data.byService].sort((a, b) => b.deliveredCount - a.deliveredCount);
  const top = sorted[0];

  return (
    <div className="stack" data-testid="demand-report">
      <div className="stats-grid">
        <StatCard label="Reservas confirmadas" value={data.totalDelivered} tone="green" />
        <StatCard
          label="Servicio más demandado"
          value={top && top.deliveredCount > 0 ? top.serviceName : '—'}
          hint={top && top.deliveredCount > 0 ? `${top.deliveredCount} reservas` : undefined}
        />
      </div>
      <Card title="Reservas por servicio">
        <BarList
          items={sorted.map((s) => ({ key: s.serviceId, label: s.serviceName, value: s.deliveredCount }))}
        />
      </Card>
    </div>
  );
}
