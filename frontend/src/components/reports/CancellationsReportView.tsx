import { reportsApi } from '../../api/endpoints';
import type { DateRange } from '../../api/types';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Card, PageLoader, StackedBarList, StatCard } from '../ui';

/** HU-21: cancelaciones separadas por quién las hizo. */
export function CancellationsReportView({ range, serviceId }: { range: DateRange; serviceId?: string }) {
  const { data, error, loading, reload } = useAsync(
    () => reportsApi.cancellations(range, serviceId),
    [range.from, range.to, serviceId],
  );

  if (loading) return <PageLoader />;
  if (error || !data) {
    return (
      <Alert kind="error" onRetry={reload}>
        {error}
      </Alert>
    );
  }

  return (
    <div className="stack" data-testid="cancellations-report">
      <div className="stats-grid">
        <StatCard label="Total cancelaciones" value={data.total} tone="red" />
        <StatCard label="Por el cliente" value={data.cancelledByClient} tone="amber" />
        <StatCard label="Por el proveedor" value={data.cancelledByProvider} tone="red" />
      </div>
      <Card title="Cancelaciones por servicio">
        <StackedBarList
          labelA="Cliente"
          labelB="Proveedor"
          items={data.byService.map((s) => ({
            key: s.serviceId,
            label: s.serviceName,
            a: s.cancelledByClient,
            b: s.cancelledByProvider,
          }))}
        />
      </Card>
    </div>
  );
}
