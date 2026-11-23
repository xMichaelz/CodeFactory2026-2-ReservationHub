import { useEffect, useState, type FormEvent } from 'react';
import { schedulesApi } from '../../api/endpoints';
import type { DayOfWeek, ProviderSchedule } from '../../api/types';
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Modal,
  PageHeader,
  PageLoader,
  SelectField,
  TextField,
} from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync, useSubmit } from '../../hooks/useAsync';
import {
  DAYS_OF_WEEK,
  dayLabel,
  dayOfWeekOf,
  formatDateLong,
  formatTime,
  toApiTime,
  todayIso,
} from '../../utils/format';

/** HU-08: franjas de atención semanales. HU-09: bloqueos puntuales dentro de una franja. */
export function SchedulesPage() {
  const toast = useToast();
  const { data, error, loading, reload } = useAsync(() => schedulesApi.mine(), []);
  const [form, setForm] = useState<{ dayOfWeek: DayOfWeek; startTime: string; endTime: string }>({
    dayOfWeek: 'MONDAY',
    startTime: '08:00',
    endTime: '17:00',
  });
  const create = useSubmit(schedulesApi.create);
  const [blocking, setBlocking] = useState<ProviderSchedule | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (form.startTime >= form.endTime) {
      create.setError('La hora de inicio debe ser anterior a la hora de fin.');
      return;
    }
    const created = await create.submit({
      dayOfWeek: form.dayOfWeek,
      startTime: toApiTime(form.startTime),
      endTime: toApiTime(form.endTime),
    });
    if (created) {
      toast.success(`Franja del ${dayLabel(created.dayOfWeek).toLowerCase()} creada.`);
      void reload();
    }
  };

  const byDay = DAYS_OF_WEEK.map((day) => ({
    ...day,
    schedules: (data ?? [])
      .filter((s) => s.dayOfWeek === day.value)
      .sort((a, b) => a.startTime.localeCompare(b.startTime)),
  }));

  return (
    <>
      <PageHeader
        tag="Proveedor"
        title="Horarios de atención"
        description="Define en qué franjas atiendes cada día. Los bloqueos quitan huecos de una fecha concreta."
      />
      <div className="split-layout">
        <Card title="Nueva franja">
          <form className="stack" onSubmit={onSubmit}>
            <SelectField
              label="Día"
              name="dayOfWeek"
              required
              value={form.dayOfWeek}
              onChange={(e) => setForm((f) => ({ ...f, dayOfWeek: e.target.value as DayOfWeek }))}
            >
              {DAYS_OF_WEEK.map((d) => (
                <option key={d.value} value={d.value}>
                  {d.label}
                </option>
              ))}
            </SelectField>
            <div className="grid-2">
              <TextField
                label="Desde"
                type="time"
                name="startTime"
                required
                value={form.startTime}
                onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))}
              />
              <TextField
                label="Hasta"
                type="time"
                name="endTime"
                required
                value={form.endTime}
                onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))}
              />
            </div>
            {create.error && <Alert kind="error">{create.error}</Alert>}
            <Button type="submit" loading={create.submitting}>
              Agregar franja
            </Button>
          </form>
        </Card>

        <Card title="Semana">
          {loading && <PageLoader />}
          {error && (
            <Alert kind="error" onRetry={reload}>
              {error}
            </Alert>
          )}
          {data && data.length === 0 && (
            <EmptyState icon="⏰" title="Sin horarios definidos">
              Agrega al menos una franja para que tus servicios tengan disponibilidad.
            </EmptyState>
          )}
          {data && data.length > 0 && (
            <div className="week" data-testid="schedule-week">
              {byDay.map((day) => (
                <div key={day.value} className="week__day">
                  <h4 className="week__label">{day.label}</h4>
                  {day.schedules.length === 0 ? (
                    <span className="muted small">No atiende</span>
                  ) : (
                    day.schedules.map((s) => (
                      <div key={s.id} className="window" data-testid="schedule-window">
                        <div className="window__head">
                          <strong>
                            {formatTime(s.startTime)} – {formatTime(s.endTime)}
                          </strong>
                          <Button size="sm" variant="ghost" onClick={() => setBlocking(s)}>
                            + Bloqueo
                          </Button>
                        </div>
                        {s.blocks.length > 0 && (
                          <ul className="window__blocks">
                            {[...s.blocks]
                              .sort((a, b) => (a.blockDate + a.startTime).localeCompare(b.blockDate + b.startTime))
                              .map((b) => (
                                <li key={b.id}>
                                  🚫 {formatDateLong(b.blockDate)} · {formatTime(b.startTime)}–{formatTime(b.endTime)}
                                  {b.reason && <span className="muted"> — {b.reason}</span>}
                                </li>
                              ))}
                          </ul>
                        )}
                      </div>
                    ))
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <BlockModal
        schedule={blocking}
        onClose={() => setBlocking(null)}
        onCreated={() => {
          setBlocking(null);
          toast.success('Bloqueo registrado.');
          void reload();
        }}
      />
    </>
  );
}

/** Formulario de bloqueo (HU-09). La fecha debe caer en el día de la franja y dentro de su horario. */
function BlockModal({
  schedule,
  onClose,
  onCreated,
}: {
  schedule: ProviderSchedule | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [form, setForm] = useState({ blockDate: '', startTime: '', endTime: '', reason: '' });
  const { submit, submitting, error, setError } = useSubmit((id: string, body: Parameters<typeof schedulesApi.createBlock>[1]) =>
    schedulesApi.createBlock(id, body),
  );

  useEffect(() => {
    if (!schedule) return;
    setError(null);
    setForm({
      blockDate: nextDateFor(schedule.dayOfWeek),
      startTime: formatTime(schedule.startTime),
      endTime: formatTime(schedule.endTime),
      reason: '',
    });
  }, [schedule, setError]);

  if (!schedule) return null;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (dayOfWeekOf(form.blockDate) !== schedule.dayOfWeek) {
      setError(`La fecha debe ser un ${dayLabel(schedule.dayOfWeek).toLowerCase()}, el día de esta franja.`);
      return;
    }
    if (form.startTime >= form.endTime) {
      setError('La hora de inicio debe ser anterior a la hora de fin.');
      return;
    }
    if (form.startTime < formatTime(schedule.startTime) || form.endTime > formatTime(schedule.endTime)) {
      setError('El bloqueo debe estar dentro del horario de la franja.');
      return;
    }
    const created = await submit(schedule.id, {
      blockDate: form.blockDate,
      startTime: toApiTime(form.startTime),
      endTime: toApiTime(form.endTime),
      reason: form.reason.trim() || undefined,
    });
    if (created) onCreated();
  };

  return (
    <Modal
      open
      title={`Bloquear franja del ${dayLabel(schedule.dayOfWeek).toLowerCase()}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="block-form" loading={submitting}>
            Registrar bloqueo
          </Button>
        </>
      }
    >
      <form id="block-form" className="stack" onSubmit={onSubmit}>
        <p className="muted">
          Franja: {formatTime(schedule.startTime)} – {formatTime(schedule.endTime)}. El bloqueo debe estar dentro de
          ese horario.
        </p>
        <TextField
          label="Fecha"
          type="date"
          name="blockDate"
          required
          min={todayIso()}
          value={form.blockDate}
          onChange={(e) => setForm((f) => ({ ...f, blockDate: e.target.value }))}
          hint={form.blockDate ? formatDateLong(form.blockDate) : undefined}
        />
        <div className="grid-2">
          <TextField
            label="Desde"
            type="time"
            name="startTime"
            required
            value={form.startTime}
            onChange={(e) => setForm((f) => ({ ...f, startTime: e.target.value }))}
          />
          <TextField
            label="Hasta"
            type="time"
            name="endTime"
            required
            value={form.endTime}
            onChange={(e) => setForm((f) => ({ ...f, endTime: e.target.value }))}
          />
        </div>
        <TextField
          label="Motivo"
          name="reason"
          maxLength={500}
          placeholder="Ej.: capacitación, festivo"
          value={form.reason}
          onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))}
        />
        {error && <Alert kind="error">{error}</Alert>}
      </form>
    </Modal>
  );
}

/** Próxima fecha (desde hoy) que cae en el día indicado. */
function nextDateFor(day: DayOfWeek): string {
  let date = todayIso();
  for (let i = 0; i < 7 && dayOfWeekOf(date) !== day; i++) {
    const [y, m, d] = date.split('-').map(Number);
    const next = new Date(Date.UTC(y, m - 1, d + 1));
    date = next.toISOString().slice(0, 10);
  }
  return date;
}
