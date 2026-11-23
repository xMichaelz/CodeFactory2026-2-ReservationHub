import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { catalogApi } from '../../api/endpoints';
import { Alert, EmptyState, PageHeader, PageLoader } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { formatDuration, formatPrice } from '../../utils/format';

/** Catálogo: servicios activos de proveedores activos (GET /catalog/services). */
export function CatalogPage() {
  const { data, error, loading, reload } = useAsync(() => catalogApi.services(), []);
  const [query, setQuery] = useState('');
  const [provider, setProvider] = useState('');

  const providers = useMemo(
    () => Array.from(new Map((data ?? []).map((s) => [s.providerId, s.providerName])).entries()),
    [data],
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data ?? []).filter(
      (s) =>
        (!provider || s.providerId === provider) &&
        (!q ||
          s.name.toLowerCase().includes(q) ||
          s.providerName.toLowerCase().includes(q) ||
          (s.description ?? '').toLowerCase().includes(q)),
    );
  }, [data, query, provider]);

  return (
    <>
      <PageHeader
        tag="Cliente"
        title="Catálogo de servicios"
        description="Elige un servicio y reserva uno de sus horarios libres."
      />
      <div className="toolbar">
        <input
          className="input toolbar__search"
          type="search"
          placeholder="Buscar por servicio, proveedor o descripción…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Buscar servicios"
          name="search"
        />
        <select
          className="input toolbar__select"
          value={provider}
          onChange={(e) => setProvider(e.target.value)}
          aria-label="Filtrar por proveedor"
          name="provider"
        >
          <option value="">Todos los proveedores</option>
          {providers.map(([id, name]) => (
            <option key={id} value={id}>
              {name}
            </option>
          ))}
        </select>
      </div>
      {loading && <PageLoader label="Cargando catálogo…" />}
      {error && (
        <Alert kind="error" onRetry={reload}>
          {error}
        </Alert>
      )}
      {data && filtered.length === 0 && (
        <EmptyState icon="🔎" title={data.length === 0 ? 'Aún no hay servicios publicados' : 'Sin resultados'}>
          {data.length === 0
            ? 'Cuando los proveedores publiquen servicios aparecerán aquí.'
            : 'Prueba con otra búsqueda o proveedor.'}
        </EmptyState>
      )}
      <div className="service-grid">
        {filtered.map((s) => (
          <article key={s.id} className="service-card" data-testid="catalog-item">
            <div className="service-card__provider">{s.providerName}</div>
            <h3 className="service-card__name">{s.name}</h3>
            {s.description && <p className="service-card__description">{s.description}</p>}
            <div className="service-card__meta">
              <span>⏱ {formatDuration(s.durationMinutes)}</span>
              <span className="service-card__price">{formatPrice(s.price)}</span>
            </div>
            <Link className="btn btn--primary btn--block" to={`/client/book/${s.id}`}>
              Reservar
            </Link>
          </article>
        ))}
      </div>
    </>
  );
}
