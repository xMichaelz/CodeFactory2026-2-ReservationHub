import { useEffect, useState, type FormEvent } from 'react';
import { reservationsApi } from '../../api/endpoints';
import type { ReservationResponse } from '../../api/types';
import { useSubmit } from '../../hooks/useAsync';
import { formatDateLong, formatTime } from '../../utils/format';
import { Alert, Button, Modal, TextAreaField } from '../ui';

interface Props {
  reservation: ReservationResponse | null;
  /** HU-15: el proveedor debe dar un motivo; para el cliente es opcional (HU-14). */
  asProvider: boolean;
  onClose: () => void;
  onCancelled: (updated: ReservationResponse) => void;
}

export function CancelReservationModal({ reservation, asProvider, onClose, onCancelled }: Props) {
  const [reason, setReason] = useState('');
  const { submit, submitting, error, setError } = useSubmit(reservationsApi.cancel);

  useEffect(() => {
    setReason('');
    setError(null);
  }, [reservation, setError]);

  if (!reservation) return null;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (asProvider && !reason.trim()) {
      setError('Como proveedor debes indicar el motivo de la cancelación.');
      return;
    }
    const updated = await submit(reservation.id, { reason: reason.trim() || undefined });
    if (updated) onCancelled(updated);
  };

  return (
    <Modal
      open
      title="Cancelar reserva"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Volver
          </Button>
          <Button variant="danger" type="submit" form="cancel-form" loading={submitting}>
            Confirmar cancelación
          </Button>
        </>
      }
    >
      <form id="cancel-form" onSubmit={onSubmit} className="stack">
        <p>
          Vas a cancelar <strong>{reservation.serviceName}</strong> del{' '}
          <strong>{formatDateLong(reservation.date)}</strong> a las{' '}
          <strong>{formatTime(reservation.startTime)}</strong>. El horario quedará libre de nuevo.
        </p>
        <TextAreaField
          label={asProvider ? 'Motivo de la cancelación' : 'Motivo (opcional)'}
          required={asProvider}
          name="reason"
          maxLength={500}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={asProvider ? 'Ej.: el profesional no estará disponible' : 'Cuéntanos por qué (opcional)'}
          hint={`${reason.length}/500`}
        />
        {error && <Alert kind="error">{error}</Alert>}
      </form>
    </Modal>
  );
}
