import type { ReservationResponse } from '../../api/types';
import { formatDateShort, formatTime, isFutureSlot } from '../../utils/format';
import { Button, ReservationStatusBadge } from '../ui';

interface Props {
  reservations: ReservationResponse[];
  /** Columna "Proveedor" (vista cliente) o "Cliente" (vista proveedor). */
  perspective: 'client' | 'provider';
  /** clientId -> nombre, para la vista del proveedor. */
  clientNames?: Record<string, string>;
  onCancel: (reservation: ReservationResponse) => void;
  onReschedule: (reservation: ReservationResponse) => void;
}

/** Reservas de /reservations/mine con las acciones de cancelar y reprogramar cuando aplican. */
export function ReservationTable({ reservations, perspective, clientNames, onCancel, onReschedule }: Props) {
  return (
    <div className="table-wrap">
      <table className="table" data-testid="reservations-table">
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Hora</th>
            <th>Servicio</th>
            <th>{perspective === 'client' ? 'Proveedor' : 'Cliente'}</th>
            <th>Estado</th>
            <th className="table__actions">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map((r) => {
            const actionable = r.status === 'CONFIRMED' && isFutureSlot(r.date, r.startTime);
            return (
              <tr key={r.id} data-testid="reservation-row">
                <td>{formatDateShort(r.date)}</td>
                <td className="nowrap">
                  {formatTime(r.startTime)} – {formatTime(r.endTime)}
                </td>
                <td>{r.serviceName}</td>
                <td>
                  {perspective === 'client' ? (
                    r.providerName
                  ) : clientNames?.[r.clientId] ? (
                    clientNames[r.clientId]
                  ) : (
                    <code className="code-chip" title={r.clientId}>
                      {r.clientId.slice(0, 8)}…
                    </code>
                  )}
                </td>
                <td>
                  <ReservationStatusBadge status={r.status} />
                </td>
                <td className="table__actions">
                  {actionable ? (
                    <div className="row-actions">
                      <Button size="sm" variant="secondary" onClick={() => onReschedule(r)}>
                        Reprogramar
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => onCancel(r)}>
                        Cancelar
                      </Button>
                    </div>
                  ) : (
                    <span className="muted">—</span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
