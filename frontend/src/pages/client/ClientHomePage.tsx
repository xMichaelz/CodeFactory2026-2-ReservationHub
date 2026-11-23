import { Link } from 'react-router-dom';
import { reservationsApi } from '../../api/endpoints';
import { Alert, Card, EmptyState, PageHeader, PageLoader, StatCard } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { addDays, formatDateLong, formatTime, isFutureSlot, MAX_RANGE_DAYS, todayIso } from '../../utils/format';

/** Resumen del cliente: próximas reservas de los siguientes 31 días. */
export function ClientHomePage() {
  const { session } = useAuth();
  const today = todayIso();
  const { data, error, loading, reload } = useAsync(
    () => reservationsApi.mine({ from: today, to: addDays(today, MAX_RANGE_DAYS - 1) }),
    [today],
  );

  const upcoming = (data ?? []).filter((r) => r.status === 'CONFIRMED' && isFutureSlot(r.date, r.startTime));
  const cancelled = (data ?? []).filter((r) => r.status !== 'CONFIRMED');

  return (
    <>
      <PageHeader
        tag="Cliente"
        title={`Hola, ${session?.email.split('@')[0]}`}
        description="Este es el resumen de tus reservas para los próximos 31 días."
        actions={
          <Link className="btn btn--primary" to="/client/catalog">
            Reservar un servicio
          </Link>
        }
      />
      {loading && <PageLoader />}
      {error && (
        <Alert kind="error" onRetry={reload}>
          {error}
        </Alert>
      )}
      {data && (
        <>
          <div className="stats-grid">
            <StatCard label="Próximas reservas" value={upcoming.length} tone="blue" />
            <StatCard label="Canceladas (31 días)" value={cancelled.length} tone="red" />
            <StatCard
              label="Siguiente cita"
              value={upcoming[0] ? formatTime(upcoming[0].startTime) : '—'}
              hint={upcoming[0] ? formatDateLong(upcoming[0].date) : 'Sin citas próximas'}
            />
          </div>
          <Card
            title="Próximas citas"
            actions={
              <Link className="btn btn--ghost btn--sm" to="/client/reservations">
                Ver todas →
              </Link>
            }
          >
            {upcoming.length === 0 ? (
              <EmptyState
                icon="📅"
                title="No tienes citas próximas"
                action={
                  <Link className="btn btn--primary" to="/client/catalog">
                    Explorar catálogo
                  </Link>
                }
              />
            ) : (
              <ul className="timeline">
                {upcoming.slice(0, 5).map((r) => (
                  <li key={r.id} className="timeline__item">
                    <div className="timeline__date">
                      <strong>{formatTime(r.startTime)}</strong>
                      <span>{formatDateLong(r.date)}</span>
                    </div>
                    <div>
                      <strong>{r.serviceName}</strong>
                      <div className="muted">{r.providerName}</div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </>
  );
}
