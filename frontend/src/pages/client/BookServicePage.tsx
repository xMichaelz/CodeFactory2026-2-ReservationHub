import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { catalogApi, reservationsApi } from '../../api/endpoints';
import type { ReservationResponse } from '../../api/types';
import { SlotPicker, type SelectedSlot } from '../../components/reservations/SlotPicker';
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync, useSubmit } from '../../hooks/useAsync';
import { formatDateLong, formatDuration, formatPrice, formatTime, toApiTime } from '../../utils/format';

/** HU-11 + HU-12: el cliente ve la disponibilidad de un servicio y reserva un hueco para sí. */
export function BookServicePage() {
  const { serviceId = '' } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [selected, setSelected] = useState<SelectedSlot | null>(null);
  const [confirmation, setConfirmation] = useState<ReservationResponse | null>(null);
  const [pickerKey, setPickerKey] = useState(0);

  const catalog = useAsync(() => catalogApi.services(), []);
  const service = catalog.data?.find((s) => s.id === serviceId);
  const { submit, submitting, error } = useSubmit(reservationsApi.create);

  const onConfirm = async () => {
    if (!selected) return;
    const reservation = await submit({
      serviceId,
      date: selected.date,
      startTime: toApiTime(selected.slot.start),
    });
    if (reservation) {
      setConfirmation(reservation);
      toast.success('¡Reserva confirmada!');
    } else {
      // El horario pudo ocuparse mientras tanto: recargar la disponibilidad.
      setSelected(null);
      setPickerKey((k) => k + 1);
    }
  };

  if (catalog.loading) return <PageLoader />;
  if (catalog.error) return <Alert kind="error" onRetry={catalog.reload}>{catalog.error}</Alert>;
  if (!service) {
    return (
      <EmptyState
        icon="🔎"
        title="Servicio no disponible"
        action={
          <Link className="btn btn--primary" to="/client/catalog">
            Volver al catálogo
          </Link>
        }
      >
        El servicio no existe o ya no está activo.
      </EmptyState>
    );
  }

  if (confirmation) {
    return (
      <div className="confirmation" data-testid="booking-confirmation">
        <div className="confirmation__icon">✅</div>
        <h1>¡Reserva confirmada!</h1>
        <dl className="details">
          <dt>Servicio</dt>
          <dd>{confirmation.serviceName}</dd>
          <dt>Proveedor</dt>
          <dd>{confirmation.providerName}</dd>
          <dt>Fecha</dt>
          <dd>{formatDateLong(confirmation.date)}</dd>
          <dt>Hora</dt>
          <dd>
            {formatTime(confirmation.startTime)} – {formatTime(confirmation.endTime)}
          </dd>
          <dt>Código</dt>
          <dd>
            <code>{confirmation.id}</code>
          </dd>
        </dl>
        <div className="row-actions row-actions--center">
          <Button variant="secondary" onClick={() => navigate('/client/catalog')}>
            Seguir explorando
          </Button>
          <Button onClick={() => navigate('/client/reservations')}>Ver mis reservas</Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        tag={service.providerName}
        title={service.name}
        description={service.description ?? undefined}
        actions={
          <Link className="btn btn--ghost" to="/client/catalog">
            ← Catálogo
          </Link>
        }
      />
      <div className="booking-layout">
        <Card title="Elige un horario" subtitle="Solo se muestran los huecos libres.">
          <SlotPicker key={pickerKey} serviceId={service.id} selected={selected} onSelect={setSelected} />
        </Card>
        <Card title="Resumen" className="booking-summary">
          <dl className="details">
            <dt>Servicio</dt>
            <dd>{service.name}</dd>
            <dt>Proveedor</dt>
            <dd>{service.providerName}</dd>
            <dt>Duración</dt>
            <dd>{formatDuration(service.durationMinutes)}</dd>
            <dt>Precio</dt>
            <dd>{formatPrice(service.price)}</dd>
            <dt>Horario</dt>
            <dd>
              {selected
                ? `${formatDateLong(selected.date)}, ${formatTime(selected.slot.start)} – ${formatTime(selected.slot.end)}`
                : 'Selecciona un horario'}
            </dd>
          </dl>
          {error && <Alert kind="error">{error}</Alert>}
          <Button className="btn--block" disabled={!selected} loading={submitting} onClick={onConfirm}>
            Confirmar reserva
          </Button>
        </Card>
      </div>
    </>
  );
}
