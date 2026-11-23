import { useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { describeError } from '../../api/client';
import { reservationsApi, servicesApi, usersApi } from '../../api/endpoints';
import type { ClientLookup, ReservationResponse } from '../../api/types';
import { SlotPicker, type SelectedSlot } from '../../components/reservations/SlotPicker';
import {
  Alert,
  Button,
  Card,
  EmptyState,
  PageHeader,
  PageLoader,
  SelectField,
} from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync, useSubmit } from '../../hooks/useAsync';
import { formatDateLong, formatDuration, formatTime, toApiTime } from '../../utils/format';

/** HU-13: el proveedor crea una reserva a nombre de un cliente (buscado por correo). */
export function ProviderBookPage() {
  const toast = useToast();
  const services = useAsync(() => servicesApi.mine(), []);
  const active = (services.data ?? []).filter((s) => s.status === 'ACTIVE');

  const [serviceId, setServiceId] = useState('');
  const [email, setEmail] = useState('');
  const [client, setClient] = useState<ClientLookup | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [lookingUp, setLookingUp] = useState(false);
  const [selected, setSelected] = useState<SelectedSlot | null>(null);
  const [confirmation, setConfirmation] = useState<ReservationResponse | null>(null);
  const [pickerKey, setPickerKey] = useState(0);
  const create = useSubmit(reservationsApi.create);

  const service = active.find((s) => s.id === serviceId);
  const lookupId = useRef(0);

  const onLookup = async (e: FormEvent) => {
    e.preventDefault();
    const id = ++lookupId.current;
    setLookingUp(true);
    setLookupError(null);
    setClient(null);
    try {
      const found = await usersApi.lookup(email.trim());
      if (id === lookupId.current) setClient(found);
    } catch (err) {
      if (id === lookupId.current) setLookupError(describeError(err));
    } finally {
      if (id === lookupId.current) setLookingUp(false);
    }
  };

  const onConfirm = async () => {
    if (!service || !client || !selected) return;
    const reservation = await create.submit({
      serviceId: service.id,
      clientId: client.id,
      date: selected.date,
      startTime: toApiTime(selected.slot.start),
    });
    if (reservation) {
      toast.success(`Reserva creada para ${client.name}.`);
      setConfirmation(reservation);
    } else {
      setSelected(null);
      setPickerKey((k) => k + 1);
    }
  };

  const reset = () => {
    setConfirmation(null);
    setSelected(null);
    setClient(null);
    setEmail('');
    setPickerKey((k) => k + 1);
  };

  if (services.loading) return <PageLoader />;
  if (services.error) {
    return (
      <Alert kind="error" onRetry={services.reload}>
        {services.error}
      </Alert>
    );
  }

  if (confirmation && client) {
    return (
      <div className="confirmation" data-testid="booking-confirmation">
        <div className="confirmation__icon">✅</div>
        <h1>Reserva creada</h1>
        <dl className="details">
          <dt>Cliente</dt>
          <dd>{client.name}</dd>
          <dt>Servicio</dt>
          <dd>{confirmation.serviceName}</dd>
          <dt>Fecha</dt>
          <dd>{formatDateLong(confirmation.date)}</dd>
          <dt>Hora</dt>
          <dd>
            {formatTime(confirmation.startTime)} – {formatTime(confirmation.endTime)}
          </dd>
        </dl>
        <div className="row-actions row-actions--center">
          <Button variant="secondary" onClick={reset}>
            Crear otra
          </Button>
          <Link className="btn btn--primary" to="/provider/agenda">
            Ver agenda
          </Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        tag="Proveedor · HU-13"
        title="Reservar a nombre de un cliente"
        description="Útil para reservas hechas por teléfono o en persona. El cliente debe estar registrado."
      />
      {active.length === 0 ? (
        <EmptyState
          icon="🧾"
          title="No tienes servicios activos"
          action={
            <Link className="btn btn--primary" to="/provider/services">
              Crear servicio
            </Link>
          }
        />
      ) : (
        <div className="booking-layout">
          <div className="stack">
            <Card title="1. Servicio">
              <SelectField
                label="Servicio"
                name="serviceId"
                required
                value={serviceId}
                onChange={(e) => {
                  setServiceId(e.target.value);
                  setSelected(null);
                }}
              >
                <option value="">Selecciona un servicio…</option>
                {active.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} · {formatDuration(s.durationMinutes)}
                  </option>
                ))}
              </SelectField>
            </Card>
            <Card title="2. Cliente">
              <form className="inline-form" onSubmit={onLookup}>
                <input
                  className="input"
                  type="email"
                  required
                  placeholder="correo@cliente.com"
                  aria-label="Correo del cliente"
                  name="clientEmail"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setClient(null);
                    lookupId.current++; // descarta una búsqueda en curso
                    setLookingUp(false);
                  }}
                />
                <Button type="submit" variant="secondary" loading={lookingUp}>
                  Buscar
                </Button>
              </form>
              {client && (
                <Alert kind="success">
                  Cliente encontrado: <strong data-testid="client-name">{client.name}</strong>
                </Alert>
              )}
              {lookupError && <Alert kind="error">{lookupError}</Alert>}
            </Card>
            {service && (
              <Card title="3. Horario">
                <SlotPicker key={`${service.id}-${pickerKey}`} serviceId={service.id} selected={selected} onSelect={setSelected} />
              </Card>
            )}
          </div>
          <Card title="Resumen" className="booking-summary">
            <dl className="details">
              <dt>Servicio</dt>
              <dd>{service?.name ?? '—'}</dd>
              <dt>Cliente</dt>
              <dd>{client?.name ?? '—'}</dd>
              <dt>Horario</dt>
              <dd>
                {selected
                  ? `${formatDateLong(selected.date)}, ${formatTime(selected.slot.start)} – ${formatTime(selected.slot.end)}`
                  : '—'}
              </dd>
            </dl>
            {create.error && <Alert kind="error">{create.error}</Alert>}
            <Button
              className="btn--block"
              disabled={!service || !client || !selected}
              loading={create.submitting}
              onClick={onConfirm}
            >
              Crear reserva
            </Button>
          </Card>
        </div>
      )}
    </>
  );
}
