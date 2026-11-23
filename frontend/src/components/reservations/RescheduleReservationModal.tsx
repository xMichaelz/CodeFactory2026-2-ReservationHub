import { useEffect, useState } from 'react';
import { reservationsApi } from '../../api/endpoints';
import type { ReservationResponse } from '../../api/types';
import { useSubmit } from '../../hooks/useAsync';
import { formatDateLong, formatTime, toApiTime } from '../../utils/format';
import { Alert, Button, Modal } from '../ui';
import { SlotPicker, type SelectedSlot } from './SlotPicker';

interface Props {
  reservation: ReservationResponse | null;
  onClose: () => void;
  onRescheduled: (updated: ReservationResponse) => void;
}

/** HU-16: mueve una reserva futura a otro hueco libre del mismo servicio. */
export function RescheduleReservationModal({ reservation, onClose, onRescheduled }: Props) {
  const [selected, setSelected] = useState<SelectedSlot | null>(null);
  const { submit, submitting, error, setError } = useSubmit(reservationsApi.reschedule);

  useEffect(() => {
    setSelected(null);
    setError(null);
  }, [reservation, setError]);

  if (!reservation) return null;

  const onConfirm = async () => {
    if (!selected) return;
    const updated = await submit(reservation.id, {
      date: selected.date,
      startTime: toApiTime(selected.slot.start),
    });
    if (updated) onRescheduled(updated);
  };

  return (
    <Modal
      open
      size="lg"
      title="Reprogramar reserva"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Volver
          </Button>
          <Button onClick={onConfirm} disabled={!selected} loading={submitting}>
            Confirmar nuevo horario
          </Button>
        </>
      }
    >
      <div className="stack">
        <p>
          <strong>{reservation.serviceName}</strong> · actualmente el {formatDateLong(reservation.date)} a las{' '}
          {formatTime(reservation.startTime)}. Elige un nuevo horario libre:
        </p>
        <SlotPicker serviceId={reservation.serviceId} selected={selected} onSelect={setSelected} />
        {selected && (
          <Alert kind="info">
            Nuevo horario: {formatDateLong(selected.date)} de {formatTime(selected.slot.start)} a{' '}
            {formatTime(selected.slot.end)}
          </Alert>
        )}
        {error && <Alert kind="error">{error}</Alert>}
      </div>
    </Modal>
  );
}
