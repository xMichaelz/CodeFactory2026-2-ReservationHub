import { Link, Outlet } from 'react-router-dom';
import { homePathFor, useAuth } from '../../context/AuthContext';
import { Brand } from './Brand';

/** Encabezado simple para la portada, el login y los registros. */
export function PublicLayout() {
  const { session } = useAuth();
  return (
    <div className="public-shell">
      <header className="public-header">
        <Link to="/">
          <Brand />
        </Link>
        <nav className="public-header__nav">
          {session ? (
            <Link className="btn btn--primary btn--sm" to={homePathFor(session.role)}>
              Ir a mi panel
            </Link>
          ) : (
            <>
              <Link className="btn btn--ghost btn--sm" to="/login">
                Iniciar sesión
              </Link>
              <Link className="btn btn--primary btn--sm" to="/register">
                Crear cuenta
              </Link>
            </>
          )}
        </nav>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
      <footer className="public-footer">
        <span>ReservaHub · CodeF@ctory UdeA</span>
        <span className="public-footer__links">
          <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
            Swagger
          </a>
          <a href="/actuator/health" target="_blank" rel="noreferrer">
            Health
          </a>
        </span>
      </footer>
    </div>
  );
}
