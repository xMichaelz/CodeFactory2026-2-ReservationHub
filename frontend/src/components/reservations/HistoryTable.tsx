import { useMemo, useState } from 'react';
import type { ReservationHistoryItem, ReservationLifecycle } from '../../api/types';
import { formatDateShort, formatTime, LIFECYCLE_LABELS } from '../../utils/format';
import { LifecycleBadge, ReservationStatusBadge } from '../ui';

const FILTERS: (ReservationLifecycle | 'ALL')[] = ['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];

/** HU-17 / HU-18: historial con estado guardado y ciclo de vida calculado, filtrable por ciclo. */
export function HistoryTable({ items, showClient }: { items: ReservationHistoryItem[]; showClient: boolean }) {
  const [filter, setFilter] = useState<ReservationLifecycle | 'ALL'>('ALL');

  const counts = useMemo(() => {
    const result: Record<string, number> = { ALL: items.length };
    items.forEach((i) => {
      result[i.lifecycle] = (result[i.lifecycle] ?? 0) + 1;
    });
    return result;
  }, [items]);

  const visible = filter === 'ALL' ? items : items.filter((i) => i.lifecycle === filter);

  return (
    <>
      <div className="tabs" role="tablist">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            role="tab"
            aria-selected={filter === f}
            className={`tab ${filter === f ? 'tab--active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f === 'ALL' ? 'Todas' : LIFECYCLE_LABELS[f]}
            <span className="tab__count">{counts[f] ?? 0}</span>
          </button>
        ))}
      </div>
      <div className="table-wrap">
        <table className="table" data-testid="history-table">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Hora</th>
              <th>Servicio</th>
              {showClient ? <th>Cliente</th> : <th>Proveedor</th>}
              <th>Ciclo de vida</th>
              <th>Estado guardado</th>
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 && (
              <tr>
                <td colSpan={6} className="table__empty">
                  No hay reservas con este filtro.
                </td>
              </tr>
            )}
            {visible.map((item) => (
              <tr key={item.id} data-testid="history-row">
                <td>{formatDateShort(item.date)}</td>
                <td className="nowrap">
                  {formatTime(item.startTime)} – {formatTime(item.endTime)}
                </td>
                <td>{item.serviceName}</td>
                <td>{showClient ? item.clientName : item.providerName}</td>
                <td>
                  <LifecycleBadge lifecycle={item.lifecycle} />
                </td>
                <td>
                  <ReservationStatusBadge status={item.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
