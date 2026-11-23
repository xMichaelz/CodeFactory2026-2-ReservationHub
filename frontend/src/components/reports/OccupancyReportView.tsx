import { reportsApi } from '../../api/endpoints';
import type { DateRange } from '../../api/types';
import { useAsync } from '../../hooks/useAsync';
import { formatDateShort, formatPercent } from '../../utils/format';
import { Alert, BarList, Card, PageLoader, StatCard } from '../ui';

/** HU-19: huecos ocupados frente a los ofrecidos por la agenda. */
export function OccupancyReportView({ range, serviceId }: { range: DateRange; serviceId?: string }) {
  const { data, error, loading, reload } = useAsync(
    () => reportsApi.occupancy(range, serviceId),
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
    <div className="stack" data-testid="occupancy-report">
      <div className="stats-grid">
        <StatCard label="Tasa de ocupación" value={formatPercent(data.occupancyRate)} tone="blue" />
        <StatCard label="Huecos ofrecidos" value={data.offeredSlots} />
        <StatCard label="Huecos ocupados" value={data.occupiedSlots} tone="green" />
        <StatCard label="Huecos libres" value={data.freeSlots} tone="amber" />
      </div>
      <div className="grid-2 grid-2--top">
        <Card title="Por servicio">
          <BarList
            max={1}
            items={data.byService.map((s) => ({
              key: s.serviceId,
              label: s.serviceName,
              value: Number(s.occupancyRate),
              display: `${formatPercent(s.occupancyRate)} · ${s.occupiedSlots}/${s.offeredSlots}`,
            }))}
          />
        </Card>
        <Card title="Por día">
          <BarList
            max={1}
            items={data.byDate
              .filter((d) => d.offeredSlots > 0)
              .map((d) => ({
                key: d.date,
                label: formatDateShort(d.date),
                value: Number(d.occupancyRate),
                display: `${formatPercent(d.occupancyRate)} · ${d.occupiedSlots}/${d.offeredSlots}`,
              }))}
            emptyText="No hubo agenda ofrecida en el periodo."
          />
        </Card>
      </div>
    </div>
  );
}
