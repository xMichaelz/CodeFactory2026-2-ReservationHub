import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { describeError } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import { Alert, Button, TextField } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PASSWORD_HINT, validatePassword } from './passwordRules';

const EMPTY = { firstName: '', lastName: '', email: '', phone: '', password: '', confirm: '' };

/** HU-01: registro de cliente. Al terminar inicia sesión automáticamente. */
export function RegisterClientPage() {
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
      await usersApi.register({
        email,
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim() || undefined,
      });
      await login({ email, password: form.password });
      toast.success('Cuenta creada. ¡Ya puedes reservar!');
      navigate('/client/catalog', { replace: true });
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
          <span className="segmented__item segmented__item--active">Cliente</span>
          <Link className="segmented__item" to="/register/provider">
            Proveedor
          </Link>
        </div>
        <h1 className="auth-card__title">Crear cuenta de cliente</h1>
        <p className="auth-card__subtitle">Reserva servicios de cualquier proveedor.</p>
        <form className="stack" onSubmit={onSubmit}>
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
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" loading={submitting} className="btn--block">
            Crear cuenta
          </Button>
        </form>
        <p className="auth-card__footer">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}
