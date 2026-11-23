import { useEffect, useState, type FormEvent } from 'react';
import { servicesApi } from '../../api/endpoints';
import type { ProviderService, ServiceResponse } from '../../api/types';
import { useSubmit } from '../../hooks/useAsync';
import { Alert, Button, Modal, TextAreaField, TextField } from '../ui';

interface Props {
  open: boolean;
  /** Si viene, el formulario edita (HU-06); si no, crea (HU-05). */
  service: ProviderService | null;
  onClose: () => void;
  onSaved: (saved: ServiceResponse) => void;
}

const EMPTY = { name: '', description: '', durationMinutes: '30', price: '0' };

export function ServiceFormModal({ open, service, onClose, onSaved }: Props) {
  const [form, setForm] = useState(EMPTY);
  const save = (payload: Parameters<typeof servicesApi.create>[0]) =>
    service ? servicesApi.update(service.id, payload) : servicesApi.create(payload);
  const { submit, submitting, error, setError } = useSubmit(save);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setForm(
      service
        ? {
            name: service.name,
            description: service.description ?? '',
            durationMinutes: String(service.durationMinutes),
            price: String(service.price),
          }
        : EMPTY,
    );
  }, [open, service, setError]);

  const set = (key: keyof typeof EMPTY) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const duration = Number(form.durationMinutes);
    const price = Number(form.price);
    if (!Number.isInteger(duration) || duration <= 0) {
      setError('La duración debe ser un número entero de minutos mayor que 0.');
      return;
    }
    if (Number.isNaN(price) || price < 0) {
      setError('El precio debe ser un número mayor o igual a 0.');
      return;
    }
    const saved = await submit({
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      durationMinutes: duration,
      price,
    });
    if (saved) onSaved(saved);
  };

  return (
    <Modal
      open={open}
      title={service ? 'Editar servicio' : 'Nuevo servicio'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" form="service-form" loading={submitting}>
            {service ? 'Guardar cambios' : 'Crear servicio'}
          </Button>
        </>
      }
    >
      <form id="service-form" className="stack" onSubmit={onSubmit}>
        <TextField label="Nombre" required maxLength={150} value={form.name} name="name" onChange={set('name')} />
        <TextAreaField
          label="Descripción"
          maxLength={1000}
          value={form.description}
          name="description" onChange={set('description')}
        />
        <div className="grid-2">
          <TextField
            label="Duración (minutos)"
            type="number"
            min={1}
            step={1}
            required
            value={form.durationMinutes}
            name="durationMinutes" onChange={set('durationMinutes')}
            hint="Define el tamaño de cada hueco de la agenda."
          />
          <TextField
            label="Precio (COP)"
            type="number"
            min={0}
            step="0.01"
            required
            value={form.price}
            name="price" onChange={set('price')}
          />
        </div>
        {error && <Alert kind="error">{error}</Alert>}
      </form>
    </Modal>
  );
}
