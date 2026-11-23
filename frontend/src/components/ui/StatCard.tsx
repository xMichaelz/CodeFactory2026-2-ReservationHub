import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  tone?: 'default' | 'green' | 'red' | 'amber' | 'blue';
}

export function StatCard({ label, value, hint, tone = 'default' }: StatCardProps) {
  return (
    <div className={`stat-card stat-card--${tone}`}>
      <span className="stat-card__label">{label}</span>
      <strong className="stat-card__value">{value}</strong>
      {hint && <span className="stat-card__hint">{hint}</span>}
    </div>
  );
}
