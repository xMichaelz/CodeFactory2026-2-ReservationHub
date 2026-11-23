import { useState } from 'react';
import { servicesApi } from '../../api/endpoints';
import type { DateRange } from '../../api/types';
import { CancellationsReportView } from '../../components/reports/CancellationsReportView';
import { DemandReportView } from '../../components/reports/DemandReportView';
import { OccupancyReportView } from '../../components/reports/OccupancyReportView';
import { Card, DateRangeFilter, PageHeader } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { addDays, todayIso } from '../../utils/format';

type Tab = 'occupancy' | 'demand' | 'cancellations';

const TABS: { id: Tab; label: string; hu: string }[] = [
  { id: 'occupancy', label: 'Ocupación', hu: 'HU-19' },
  { id: 'demand', label: 'Demanda', hu: 'HU-20' },
  { id: 'cancellations', label: 'Cancelaciones', hu: 'HU-21' },
];

/** Reportes del proveedor: ocupación, demanda y cancelaciones (máx. 31 días). */
export function ReportsPage() {
  const today = todayIso();
  const [tab, setTab] = useState<Tab>('occupancy');
  const [range, setRange] = useState<DateRange>({ from: addDays(today, -15), to: addDays(today, 15) });
  const [serviceId, setServiceId] = useState('');
  const services = useAsync(() => servicesApi.mine(), []);
  const supportsService = tab !== 'demand';

  return (
    <>
      <PageHeader
        tag="Proveedor"
        title="Reportes"
        description="Indicadores de tu agenda en un rango de hasta 31 días."
      />
      <div className="tabs tabs--large" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            className={`tab ${tab === t.id ? 'tab--active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label} <span className="tab__count">{t.hu}</span>
          </button>
        ))}
      </div>
      <Card>
        <DateRangeFilter value={range} onChange={setRange}>
          {supportsService && (
            <label className="field field--inline">
              <span className="field__label">Servicio</span>
              <select
                className="input"
                value={serviceId}
                onChange={(e) => setServiceId(e.target.value)}
                aria-label="Servicio"
                name="serviceId"
              >
                <option value="">Todos</option>
                {(services.data ?? []).map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                    {s.status === 'INACTIVE' ? ' (inactivo)' : ''}
                  </option>
                ))}
              </select>
            </label>
          )}
        </DateRangeFilter>
      </Card>
      {tab === 'occupancy' && <OccupancyReportView range={range} serviceId={serviceId || undefined} />}
      {tab === 'demand' && <DemandReportView range={range} />}
      {tab === 'cancellations' && <CancellationsReportView range={range} serviceId={serviceId || undefined} />}
    </>
  );
}
