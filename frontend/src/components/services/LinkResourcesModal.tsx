import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { servicesApi } from '../../api/endpoints';
import type { ProviderService, ResourceResponse } from '../../api/types';
import { useSubmit } from '../../hooks/useAsync';
import { Alert, Button, EmptyState, Modal } from '../ui';

interface Props {
  service: ProviderService | null;
  resources: ResourceResponse[];
  onClose: () => void;
  onLinked: () => void;
}

/** Asocia recursos activos del proveedor a un servicio (POST /services/{id}/resources). */
export function LinkResourcesModal({ service, resources, onClose, onLinked }: Props) {
  const [selected, setSelected] = useState<string[]>([]);
  const { submit, submitting, error, setError } = useSubmit(async (serviceId: string, ids: string[]) => {
    await servicesApi.linkResources(serviceId, ids);
    return true;
  });

  useEffect(() => {
    setSelected([]);
    setError(null);
  }, [service, setError]);

  if (!service) return null;

  const linkedIds = new Set(service.resources.map((r) => r.id));
  const available = resources.filter((r) => r.status === 'ACTIVE' && !linkedIds.has(r.id));

  const toggle = (id: string) =>
    setSelected((current) => (current.includes(id) ? current.filter((x) => x !== id) : [...current, id]));

  const onConfirm = async () => {
    if (selected.length === 0) return;
    // linkResources responde 201 sin cuerpo: el envoltorio devuelve true si no hubo error.
    const ok = await submit(service.id, selected);
    if (ok) onLinked();
  };

  return (
    <Modal
      open
      title={`Recursos de “${service.name}”`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cerrar
          </Button>
          <Button onClick={onConfirm} disabled={selected.length === 0} loading={submitting}>
            Asociar {selected.length > 0 ? `(${selected.length})` : ''}
          </Button>
        </>
      }
    >
      <div className="stack">
        {service.resources.length > 0 && (
          <div>
            <h4 className="subheading">Ya asociados</h4>
            <div className="chips">
              {service.resources.map((r) => (
                <span key={r.id} className="chip chip--static">
                  {r.name} · {r.quantity}
                </span>
              ))}
            </div>
          </div>
        )}
        <div>
          <h4 className="subheading">Disponibles para asociar</h4>
          {available.length === 0 ? (
            <EmptyState icon="🧰" title="No hay recursos activos sin asociar">
              <Link to="/provider/resources">Registra un recurso</Link> para poder asociarlo.
            </EmptyState>
          ) : (
            <ul className="check-list">
              {available.map((r) => (
                <li key={r.id}>
                  <label className="check">
                    <input type="checkbox" checked={selected.includes(r.id)} onChange={() => toggle(r.id)} />
                    <span>
                      <strong>{r.name}</strong>
                      <span className="muted">
                        {' '}
                        · {r.resourceType} · cantidad {r.quantity}
                      </span>
                    </span>
                  </label>
                </li>
              ))}
            </ul>
          )}
        </div>
        {error && <Alert kind="error">{error}</Alert>}
      </div>
    </Modal>
  );
}
