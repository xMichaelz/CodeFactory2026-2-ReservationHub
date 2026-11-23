import { useState } from 'react';
import { describeError } from '../../api/client';
import { resourcesApi, servicesApi } from '../../api/endpoints';
import type { ProviderService } from '../../api/types';
import { LinkResourcesModal } from '../../components/services/LinkResourcesModal';
import { ServiceFormModal } from '../../components/services/ServiceFormModal';
import {
  Alert,
  Button,
  Card,
  EmptyState,
  EntityStatusBadge,
  Modal,
  PageHeader,
  PageLoader,
} from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync } from '../../hooks/useAsync';
import { formatDuration, formatPrice } from '../../utils/format';

/** HU-05 crear, HU-06 editar, HU-07 desactivar y asociación de recursos. */
export function ServicesPage() {
  const toast = useToast();
  const { data, error, loading, reload } = useAsync(async () => {
    const [services, resources] = await Promise.all([servicesApi.mine(), resourcesApi.mine()]);
    return { services, resources };
  }, []);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ProviderService | null>(null);
  const [linking, setLinking] = useState<ProviderService | null>(null);
  const [deactivating, setDeactivating] = useState<ProviderService | null>(null);
  const [busy, setBusy] = useState(false);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };
  const openEdit = (service: ProviderService) => {
    setEditing(service);
    setFormOpen(true);
  };

  const confirmDeactivate = async () => {
    if (!deactivating) return;
    setBusy(true);
    try {
      await servicesApi.deactivate(deactivating.id);
      toast.success(`“${deactivating.name}” quedó inactivo.`);
      setDeactivating(null);
      void reload();
    } catch (err) {
      toast.error(describeError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        tag="Proveedor"
        title="Servicios"
        description="Los servicios activos aparecen en el catálogo. La duración define el tamaño de cada hueco."
        actions={<Button onClick={openCreate}>+ Nuevo servicio</Button>}
      />
      {loading && <PageLoader />}
      {error && (
        <Alert kind="error" onRetry={reload}>
          {error}
        </Alert>
      )}
      {data && data.services.length === 0 && (
        <EmptyState icon="🧾" title="Aún no tienes servicios" action={<Button onClick={openCreate}>Crear el primero</Button>}>
          Crea un servicio para que los clientes puedan reservarlo.
        </EmptyState>
      )}
      {data && data.services.length > 0 && (
        <Card>
          <div className="table-wrap">
            <table className="table" data-testid="services-table">
              <thead>
                <tr>
                  <th>Servicio</th>
                  <th>Duración</th>
                  <th>Precio</th>
                  <th>Recursos</th>
                  <th>Estado</th>
                  <th className="table__actions">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.services.map((s) => {
                  const active = s.status === 'ACTIVE';
                  return (
                    <tr key={s.id} className={active ? '' : 'table__row--muted'}>
                      <td>
                        <strong>{s.name}</strong>
                        {s.description && <div className="muted small">{s.description}</div>}
                      </td>
                      <td className="nowrap">{formatDuration(s.durationMinutes)}</td>
                      <td className="nowrap">{formatPrice(s.price)}</td>
                      <td>
                        {s.resources.length === 0 ? (
                          <span className="muted">—</span>
                        ) : (
                          <div className="chips">
                            {s.resources.map((r) => (
                              <span key={r.id} className="chip chip--static">
                                {r.name}
                              </span>
                            ))}
                          </div>
                        )}
                      </td>
                      <td>
                        <EntityStatusBadge status={s.status} />
                      </td>
                      <td className="table__actions">
                        {active ? (
                          <div className="row-actions">
                            <Button size="sm" variant="secondary" onClick={() => openEdit(s)}>
                              Editar
                            </Button>
                            <Button size="sm" variant="secondary" onClick={() => setLinking(s)}>
                              Recursos
                            </Button>
                            <Button size="sm" variant="danger" onClick={() => setDeactivating(s)}>
                              Desactivar
                            </Button>
                          </div>
                        ) : (
                          <span className="muted small">Inactivo</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <ServiceFormModal
        open={formOpen}
        service={editing}
        onClose={() => setFormOpen(false)}
        onSaved={(saved) => {
          setFormOpen(false);
          toast.success(editing ? `“${saved.name}” actualizado.` : `“${saved.name}” creado.`);
          void reload();
        }}
      />
      <LinkResourcesModal
        service={linking}
        resources={data?.resources ?? []}
        onClose={() => setLinking(null)}
        onLinked={() => {
          setLinking(null);
          toast.success('Recursos asociados.');
          void reload();
        }}
      />
      <Modal
        open={deactivating !== null}
        title="Desactivar servicio"
        onClose={() => setDeactivating(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeactivating(null)}>
              Volver
            </Button>
            <Button variant="danger" loading={busy} onClick={confirmDeactivate}>
              Desactivar
            </Button>
          </>
        }
      >
        <p>
          <strong>{deactivating?.name}</strong> dejará de aparecer en el catálogo y no aceptará nuevas reservas. Esta
          acción no se puede deshacer desde la aplicación.
        </p>
      </Modal>
    </>
  );
}
