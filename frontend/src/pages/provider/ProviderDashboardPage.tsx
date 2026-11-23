import { Link } from 'react-router-dom';
import { reservationsApi, resourcesApi, schedulesApi, servicesApi } from '../../api/endpoints';
import { Alert, Card, EmptyState, PageHeader, PageLoader, StatCard } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { addDays, formatDateLong, formatTime, isFutureSlot, todayIso } from '../../utils/format';

/** Panel del proveedor: estado de la configuración y próximas citas de la semana. */
export function ProviderDashboardPage() {
  const today = todayIso();
  const { data, error, loading, reload } = useAsync(async () => {
    const [services, resources, schedules, agenda] = await Promise.all([
      servicesApi.mine(),
      resourcesApi.mine(),
      schedulesApi.mine(),
      reservationsApi.mine({ from: today, to: addDays(today, 6) }),
    ]);
    return { services, resources, schedules, agenda };
  }, [today]);

  if (loading) return <PageLoader />;
  if (error || !data) {
    return (
      <Alert kind="error" onRetry={reload}>
        {error}
      </Alert>
    );
  }

  const activeServices = data.services.filter((s) => s.status === 'ACTIVE');
  const upcoming = data.agenda.filter((r) => r.status === 'CONFIRMED' && isFutureSlot(r.date, r.startTime));
  const steps = [
    { done: activeServices.length > 0, label: 'Crea al menos un servicio activo', to: '/provider/services' },
    { done: data.schedules.length > 0, label: 'Define tus horarios de atención', to: '/provider/schedules' },
    { done: data.resources.length > 0, label: 'Registra tus recursos (opcional)', to: '/provider/resources' },
  ];
  const pending = steps.filter((s) => !s.done);

  return (
    <>
      <PageHeader
        tag="Proveedor"
        title="Panel del negocio"
        description="Resumen de tu configuración y de las citas de los próximos 7 días."
        actions={
          <Link className="btn btn--primary" to="/provider/book">
            Reservar a un cliente
          </Link>
        }
      />
      <div className="stats-grid">
        <StatCard label="Servicios activos" value={activeServices.length} hint={`${data.services.length} en total`} tone="blue" />
        <StatCard label="Recursos" value={data.resources.length} />
        <StatCard label="Franjas de atención" value={data.schedules.length} />
        <StatCard label="Citas próximas (7 días)" value={upcoming.length} tone="green" />
      </div>

      {pending.length > 0 && (
        <Card title="Completa tu configuración" subtitle="Los clientes solo pueden reservar si tienes servicios activos y horarios.">
          <ul className="checklist">
            {steps.map((step) => (
              <li key={step.label} className={step.done ? 'checklist__item checklist__item--done' : 'checklist__item'}>
                <span aria-hidden="true">{step.done ? '✅' : '⬜'}</span>
                <Link to={step.to}>{step.label}</Link>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Card
        title="Próximas citas"
        actions={
          <Link className="btn btn--ghost btn--sm" to="/provider/agenda">
            Ver agenda →
          </Link>
        }
      >
        {upcoming.length === 0 ? (
          <EmptyState icon="📅" title="Sin citas en los próximos 7 días" />
        ) : (
          <ul className="timeline">
            {upcoming.slice(0, 8).map((r) => (
              <li key={r.id} className="timeline__item">
                <div className="timeline__date">
                  <strong>{formatTime(r.startTime)}</strong>
                  <span>{formatDateLong(r.date)}</span>
                </div>
                <div>
                  <strong>{r.serviceName}</strong>
                  <div className="muted">Hasta las {formatTime(r.endTime)}</div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </>
  );
}
