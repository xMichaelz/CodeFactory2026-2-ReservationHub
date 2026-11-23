import { useState } from 'react';
import { servicesApi } from '../../api/endpoints';
import type { Slot, UUID } from '../../api/types';
import { useAsync } from '../../hooks/useAsync';
import { addDays, formatDateLong, formatTime, todayIso } from '../../utils/format';
import { Alert, Button, EmptyState, Spinner } from '../ui';

export interface SelectedSlot {
  date: string;
  slot: Slot;
}

interface SlotPickerProps {
  serviceId: UUID;
  selected: SelectedSlot | null;
  onSelect: (selection: SelectedSlot) => void;
  /** Días por página (máximo 31 en la API). */
  windowDays?: number;
}

/** HU-11: muestra los huecos libres de un servicio agrupados por fecha, con navegación por semanas. */
export function SlotPicker({ serviceId, selected, onSelect, windowDays = 7 }: SlotPickerProps) {
  const today = todayIso();
  const [from, setFrom] = useState(today);
  const to = addDays(from, windowDays - 1);

  const { data, error, loading, reload } = useAsync(
    () => servicesApi.availability(serviceId, { from, to }),
    [serviceId, from, to],
  );

  const days = data?.days ?? [];
  const daysWithSlots = days.filter((d) => d.slots.length > 0);

  return (
    <div className="slot-picker" data-testid="slot-picker">
      <div className="slot-picker__nav">
        <Button
          variant="secondary"
          size="sm"
          disabled={from <= today || loading}
          onClick={() => setFrom(addDays(from, -windowDays) < today ? today : addDays(from, -windowDays))}
        >
          ← Anterior
        </Button>
        <span className="slot-picker__range">
          {formatDateLong(from)} — {formatDateLong(to)}
        </span>
        <Button variant="secondary" size="sm" disabled={loading} onClick={() => setFrom(addDays(from, windowDays))}>
          Siguiente →
        </Button>
      </div>

      {loading && <Spinner label="Buscando horarios libres…" />}
      {error && (
        <Alert kind="error" onRetry={reload}>
          {error}
        </Alert>
      )}
      {!loading && !error && daysWithSlots.length === 0 && (
        <EmptyState icon="🗓️" title="Sin horarios libres en estas fechas">
          Prueba con la semana siguiente. El proveedor puede no tener agenda o estar completo.
        </EmptyState>
      )}
      {!loading &&
        !error &&
        daysWithSlots.map((day) => (
          <div key={day.date} className="slot-day">
            <h4 className="slot-day__title">{formatDateLong(day.date)}</h4>
            <div className="slot-day__slots">
              {day.slots.map((slot) => {
                const isSelected = selected?.date === day.date && selected.slot.start === slot.start;
                return (
                  <button
                    key={slot.start}
                    type="button"
                    className={`slot ${isSelected ? 'slot--selected' : ''}`}
                    aria-pressed={isSelected}
                    data-testid="slot"
                    onClick={() => onSelect({ date: day.date, slot })}
                  >
                    {formatTime(slot.start)} – {formatTime(slot.end)}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
    </div>
  );
}
