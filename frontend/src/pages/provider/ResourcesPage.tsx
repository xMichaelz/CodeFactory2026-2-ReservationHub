import { useState, type FormEvent } from 'react';
import { resourcesApi } from '../../api/endpoints';
import {
  Alert,
  Button,
  Card,
  EmptyState,
  EntityStatusBadge,
  PageHeader,
  PageLoader,
  TextAreaField,
  TextField,
} from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { useAsync, useSubmit } from '../../hooks/useAsync';
import { formatDateTime } from '../../utils/format';

const EMPTY = { name: '', resourceType: '', quantity: '1', description: '' };

/** HU-10: registro y listado de recursos del proveedor. */
export function ResourcesPage() {
  const toast = useToast();
  const { data, error, loading, reload } = useAsync(() => resourcesApi.mine(), []);
  const [form, setForm] = useState(EMPTY);
  const create = useSubmit(resourcesApi.create);

  const set = (key: keyof typeof EMPTY) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const quantity = Number(form.quantity);
    if (!Number.isInteger(quantity) || quantity < 1) {
      create.setError('La cantidad debe ser un entero mayor o igual a 1.');
      return;
    }
    const created = await create.submit({
      name: form.name.trim(),
      resourceType: form.resourceType.trim(),
      quantity,
      description: form.description.trim() || undefined,
    });
    if (created) {
      toast.success(`Recurso “${created.name}” registrado.`);
      setForm(EMPTY);
      void reload();
    }
  };

  return (
    <>
      <PageHeader
        tag="Proveedor"
        title="Recursos"
        description="Equipos, salas o profesionales que tus servicios necesitan. Asócialos desde la página de servicios."
      />
      <div className="split-layout">
        <Card title="Nuevo recurso">
          <form className="stack" onSubmit={onSubmit}>
            <TextField label="Nombre" required maxLength={150} value={form.name} name="name" onChange={set('name')} />
            <div className="grid-2">
              <TextField
                label="Tipo"
                required
                maxLength={50}
                placeholder="Ej.: Sala, Equipo, Personal"
                value={form.resourceType}
                name="resourceType" onChange={set('resourceType')}
              />
              <TextField
                label="Cantidad"
                type="number"
                min={1}
                step={1}
                required
                value={form.quantity}
                name="quantity" onChange={set('quantity')}
              />
            </div>
            <TextAreaField label="Descripción" maxLength={1000} value={form.description} name="description" onChange={set('description')} />
            {create.error && <Alert kind="error">{create.error}</Alert>}
            <Button type="submit" loading={create.submitting}>
              Registrar recurso
            </Button>
          </form>
        </Card>
        <Card title="Mis recursos">
          {loading && <PageLoader />}
          {error && (
            <Alert kind="error" onRetry={reload}>
              {error}
            </Alert>
          )}
          {data && data.length === 0 && <EmptyState icon="🧰" title="Aún no tienes recursos" />}
          {data && data.length > 0 && (
            <div className="table-wrap">
              <table className="table" data-testid="resources-table">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tipo</th>
                    <th>Cantidad</th>
                    <th>Estado</th>
                    <th>Creado</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <strong>{r.name}</strong>
                        {r.description && <div className="muted small">{r.description}</div>}
                      </td>
                      <td>{r.resourceType}</td>
                      <td>{r.quantity}</td>
                      <td>
                        <EntityStatusBadge status={r.status} />
                      </td>
                      <td className="nowrap small">{formatDateTime(r.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
