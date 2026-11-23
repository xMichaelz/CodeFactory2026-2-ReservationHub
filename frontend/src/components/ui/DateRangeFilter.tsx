import { useState, type FormEvent, type ReactNode } from 'react';
import type { DateRange } from '../../api/types';
import { addDays, MAX_RANGE_DAYS, todayIso, validateRange } from '../../utils/format';
import { Button } from './Button';

interface Props {
  value: DateRange;
  onChange: (range: DateRange) => void;
  /** Controles extra (por ejemplo, un selector de servicio). */
  children?: ReactNode;
  submitLabel?: string;
  loading?: boolean;
}

const PRESETS: { label: string; from: number; to: number }[] = [
  { label: 'Próximos 7 días', from: 0, to: 6 },
  { label: 'Próximos 31 días', from: 0, to: MAX_RANGE_DAYS - 1 },
  { label: 'Últimos 7 días', from: -6, to: 0 },
  { label: 'Últimos 31 días', from: -(MAX_RANGE_DAYS - 1), to: 0 },
  { label: '±15 días', from: -15, to: 15 },
];

/** Selector de rango con atajos. Valida igual que la API (máximo 31 días). */
export function DateRangeFilter({ value, onChange, children, submitLabel = 'Consultar', loading }: Props) {
  const [from, setFrom] = useState(value.from);
  const [to, setTo] = useState(value.to);
  const [error, setError] = useState<string | null>(null);

  const apply = (range: DateRange) => {
    const problem = validateRange(range.from, range.to);
    setError(problem);
    if (!problem) onChange(range);
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    apply({ from, to });
  };

  const preset = (fromOffset: number, toOffset: number) => {
    const today = todayIso();
    const range = { from: addDays(today, fromOffset), to: addDays(today, toOffset) };
    setFrom(range.from);
    setTo(range.to);
    apply(range);
  };

  return (
    <form className="range-filter" onSubmit={onSubmit}>
      <div className="range-filter__fields">
        <label className="field field--inline">
          <span className="field__label">Desde</span>
          <input type="date" name="from" className="input" value={from} onChange={(e) => setFrom(e.target.value)} required />
        </label>
        <label className="field field--inline">
          <span className="field__label">Hasta</span>
          <input type="date" name="to" className="input" value={to} onChange={(e) => setTo(e.target.value)} required />
        </label>
        {children}
        <Button type="submit" loading={loading}>
          {submitLabel}
        </Button>
      </div>
      <div className="range-filter__presets">
        <span className="muted">Atajos:</span>
        {PRESETS.map((p) => (
          <button key={p.label} type="button" className="chip" onClick={() => preset(p.from, p.to)}>
            {p.label}
          </button>
        ))}
      </div>
      {error && <p className="field__error">{error}</p>}
    </form>
  );
}
