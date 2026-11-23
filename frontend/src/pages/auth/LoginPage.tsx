import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, TextField } from '../../components/ui';
import { homePathFor, useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useSubmit } from '../../hooks/useAsync';

/** HU-03: inicio de sesión con JWT. */
export function LoginPage() {
  const { login, sessionNotice, clearSessionNotice } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { submit, submitting, error } = useSubmit(login);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearSessionNotice();
    const session = await submit({ email: email.trim(), password });
    if (!session) return;
    toast.success('¡Bienvenido!');
    const home = homePathFor(session.role);
    navigate(from && from.startsWith(home) ? from : home, { replace: true });
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-card__title">Iniciar sesión</h1>
        <p className="auth-card__subtitle">Accede como cliente o proveedor.</p>
        {sessionNotice && <Alert kind="warning">{sessionNotice}</Alert>}
        <form className="stack" onSubmit={onSubmit}>
          <TextField
            label="Correo electrónico"
            type="email"
            name="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextField
            label="Contraseña"
            type="password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" loading={submitting} className="btn--block">
            Entrar
          </Button>
        </form>
        <p className="auth-card__footer">
          ¿No tienes cuenta? <Link to="/register">Regístrate como cliente</Link> o{' '}
          <Link to="/register/provider">como proveedor</Link>.
        </p>
      </div>
    </div>
  );
}
