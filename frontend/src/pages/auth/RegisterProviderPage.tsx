import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { describeError } from '../../api/client';
import { providersApi } from '../../api/endpoints';
import { Alert, Button, TextAreaField, TextField } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PASSWORD_HINT, validatePassword } from './passwordRules';

const EMPTY = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  password: '',
  confirm: '',
  businessName: '',
  serviceType: '',
  description: '',
  address: '',
};

/** HU-02: registro de proveedor (usuario con rol PROVIDER + perfil de negocio). */
export function RegisterProviderPage() {
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (key: keyof typeof EMPTY) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const passwordProblem = validatePassword(form.password);
    if (passwordProblem) return setError(passwordProblem);
    if (form.password !== form.confirm) return setError('Las contraseñas no coinciden.');
    setError(null);
    setSubmitting(true);
    try {
      const email = form.email.trim();
      await providersApi.register({
        email,
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim() || undefined,
        businessName: form.businessName.trim(),
        serviceType: form.serviceType.trim(),
        description: form.description.trim() || undefined,
        address: form.address.trim() || undefined,
      });
      await login({ email, password: form.password });
      toast.success('Negocio registrado. Configura tus servicios y horarios.');
      navigate('/provider', { replace: true });
    } catch (err) {
      setError(describeError(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card auth-card--wide">
        <div className="segmented">
          <Link className="segmented__item" to="/register">
            Cliente
          </Link>
          <span className="segmented__item segmented__item--active">Proveedor</span>
        </div>
        <h1 className="auth-card__title">Registrar mi negocio</h1>
        <p className="auth-card__subtitle">Publica tus servicios y gestiona tu agenda.</p>
        <form className="stack" onSubmit={onSubmit}>
          <h3 className="subheading">Datos del responsable</h3>
          <div className="grid-2">
            <TextField label="Nombre" required maxLength={100} value={form.firstName} name="firstName" onChange={set('firstName')} />
            <TextField label="Apellido" required maxLength={100} value={form.lastName} name="lastName" onChange={set('lastName')} />
          </div>
          <div className="grid-2">
            <TextField
              label="Correo electrónico"
              type="email"
              required
              maxLength={255}
              autoComplete="email"
              value={form.email}
              name="email" onChange={set('email')}
            />
            <TextField label="Teléfono" maxLength={30} value={form.phone} name="phone" onChange={set('phone')} />
          </div>
          <div className="grid-2">
            <TextField
              label="Contraseña"
              type="password"
              required
              autoComplete="new-password"
              value={form.password}
              name="password" onChange={set('password')}
              hint={PASSWORD_HINT}
            />
            <TextField
              label="Confirmar contraseña"
              type="password"
              required
              autoComplete="new-password"
              value={form.confirm}
              name="confirm" onChange={set('confirm')}
            />
          </div>
          <h3 className="subheading">Datos del negocio</h3>
          <div className="grid-2">
            <TextField
              label="Nombre del negocio"
              required
              maxLength={200}
              value={form.businessName}
              name="businessName" onChange={set('businessName')}
            />
            <TextField
              label="Tipo de servicio"
              required
              maxLength={100}
              placeholder="Ej.: Barbería, Odontología, Spa"
              value={form.serviceType}
              name="serviceType" onChange={set('serviceType')}
            />
          </div>
          <TextField label="Dirección" maxLength={300} value={form.address} name="address" onChange={set('address')} />
          <TextAreaField
            label="Descripción"
            maxLength={1000}
            value={form.description}
            name="description" onChange={set('description')}
          />
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" loading={submitting} className="btn--block">
            Registrar negocio
          </Button>
        </form>
        <p className="auth-card__footer">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}
