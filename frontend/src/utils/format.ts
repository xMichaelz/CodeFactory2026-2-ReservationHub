import type { DayOfWeek, ReservationLifecycle, ReservationStatus } from '../api/types';

/** Zona horaria del negocio: la API calcula disponibilidad y ciclo de vida en esta zona. */
export const BUSINESS_TIME_ZONE = 'America/Bogota';

export const MAX_RANGE_DAYS = 31;

const pad = (n: number) => String(n).padStart(2, '0');

/** Fecha de hoy (YYYY-MM-DD) en la zona del negocio. */
export function todayIso(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  return parts; // en-CA ya produce YYYY-MM-DD
}

/** Suma días a una fecha ISO sin depender de la zona del navegador. */
export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d + days));
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

/** Días entre dos fechas ISO, inclusive. */
export function daysBetweenInclusive(from: string, to: string): number {
  const toUtc = (iso: string) => {
    const [y, m, d] = iso.split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  };
  return Math.round((toUtc(to) - toUtc(from)) / 86_400_000) + 1;
}

/** Valida un rango igual que la API: from <= to y máximo 31 días. Devuelve el error o null. */
export function validateRange(from: string, to: string): string | null {
  if (!from || !to) return 'Debes elegir ambas fechas.';
  if (from > to) return 'La fecha "desde" no puede ser posterior a "hasta".';
  if (daysBetweenInclusive(from, to) > MAX_RANGE_DAYS) return `El rango no puede superar ${MAX_RANGE_DAYS} días.`;
  return null;
}

/** "09:00:00" -> "09:00" */
export function formatTime(time: string | null | undefined): string {
  return time ? time.slice(0, 5) : '';
}

const WEEKDAY_FMT = new Intl.DateTimeFormat('es-CO', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});
const SHORT_DATE_FMT = new Intl.DateTimeFormat('es-CO', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  timeZone: 'UTC',
});

function isoToUtcDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

export function formatDateLong(iso: string): string {
  const text = WEEKDAY_FMT.format(isoToUtcDate(iso));
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatDateShort(iso: string): string {
  return SHORT_DATE_FMT.format(isoToUtcDate(iso));
}

export function formatDateTime(instant: string | null | undefined): string {
  if (!instant) return '—';
  return new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: BUSINESS_TIME_ZONE,
  }).format(new Date(instant));
}

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 });
export function formatPrice(value: number | string): string {
  return COP.format(Number(value));
}

export function formatPercent(rate: number | string): string {
  return `${(Number(rate) * 100).toFixed(1)}%`;
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

/** Convierte "HH:mm" del input a "HH:mm:ss", el formato que espera LocalTime. */
export function toApiTime(value: string): string {
  return value.length === 5 ? `${value}:00` : value;
}

export const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
  { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
];

export function dayLabel(day: DayOfWeek): string {
  return DAYS_OF_WEEK.find((d) => d.value === day)?.label ?? day;
}

/** Día de la semana (MONDAY..SUNDAY) de una fecha ISO. */
export function dayOfWeekOf(iso: string): DayOfWeek {
  const index = isoToUtcDate(iso).getUTCDay(); // 0 = domingo
  return DAYS_OF_WEEK[(index + 6) % 7].value;
}

export const STATUS_LABELS: Record<ReservationStatus, string> = {
  CONFIRMED: 'Confirmada',
  CANCELLED_BY_CLIENT: 'Cancelada por el cliente',
  CANCELLED_BY_PROVIDER: 'Cancelada por el proveedor',
};

export const LIFECYCLE_LABELS: Record<ReservationLifecycle, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'En curso',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};

/** ¿La reserva aún no empieza? (se compara en la zona del negocio). */
export function isFutureSlot(date: string, startTime: string): boolean {
  const now = new Intl.DateTimeFormat('sv-SE', {
    timeZone: BUSINESS_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  })
    .format(new Date())
    .replace(' ', 'T');
  const start = `${date}T${startTime.length === 5 ? `${startTime}:00` : startTime}`;
  return start > now;
}
