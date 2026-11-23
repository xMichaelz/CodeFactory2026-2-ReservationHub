import type { ReactNode } from 'react';
import type { EntityStatus, ReservationLifecycle, ReservationStatus } from '../../api/types';
import { LIFECYCLE_LABELS, STATUS_LABELS } from '../../utils/format';

type Tone = 'green' | 'red' | 'amber' | 'blue' | 'gray' | 'violet';

export function Badge({ tone = 'gray', children }: { tone?: Tone; children: ReactNode }) {
  return <span className={`badge badge--${tone}`}>{children}</span>;
}

export function EntityStatusBadge({ status }: { status: EntityStatus }) {
  return status === 'ACTIVE' ? <Badge tone="green">Activo</Badge> : <Badge tone="gray">Inactivo</Badge>;
}

const STATUS_TONES: Record<ReservationStatus, Tone> = {
  CONFIRMED: 'green',
  CANCELLED_BY_CLIENT: 'amber',
  CANCELLED_BY_PROVIDER: 'red',
};

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return <Badge tone={STATUS_TONES[status]}>{STATUS_LABELS[status]}</Badge>;
}

const LIFECYCLE_TONES: Record<ReservationLifecycle, Tone> = {
  PENDING: 'blue',
  CONFIRMED: 'violet',
  COMPLETED: 'green',
  CANCELLED: 'red',
};

export function LifecycleBadge({ lifecycle }: { lifecycle: ReservationLifecycle }) {
  return <Badge tone={LIFECYCLE_TONES[lifecycle]}>{LIFECYCLE_LABELS[lifecycle]}</Badge>;
}
