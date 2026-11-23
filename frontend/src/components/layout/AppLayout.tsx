import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { NAVIGATION, ROLE_LABELS } from './navigation';
import { Brand } from './Brand';

/** Estructura de las páginas autenticadas: barra lateral con la navegación del rol y contenido. */
export function AppLayout() {
  const { session, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  if (!session) return null;
  const items = session.role === 'ADMIN' ? [] : NAVIGATION[session.role];

  const onLogout = async () => {
    setLoggingOut(true);
    await logout();
    toast.success('Sesión cerrada.');
    navigate('/login', { replace: true });
  };

  return (
    <div className={`app-shell ${menuOpen ? 'app-shell--menu-open' : ''}`}>
      <aside className="sidebar">
        <div className="sidebar__brand">
          <Link to="/">
            <Brand />
          </Link>
        </div>
        <nav className="sidebar__nav" aria-label="Principal">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-link ${isActive ? 'nav-link--active' : ''}`}
            >
              <span className="nav-link__icon" aria-hidden="true">
                {item.icon}
              </span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__footer">
          <a className="nav-link nav-link--muted" href="/swagger-ui.html" target="_blank" rel="noreferrer">
            <span className="nav-link__icon" aria-hidden="true">
              🧪
            </span>
            Swagger API
          </a>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <button
            type="button"
            className="topbar__menu"
            aria-label="Abrir menú"
            onClick={() => setMenuOpen((open) => !open)}
          >
            ☰
          </button>
          <div className="topbar__spacer" />
          <div className="user-chip" data-testid="user-chip">
            <span className="user-chip__avatar" aria-hidden="true">
              {session.email.charAt(0).toUpperCase()}
            </span>
            <span className="user-chip__text">
              <span className="user-chip__email">{session.email}</span>
              <span className="user-chip__role">{ROLE_LABELS[session.role]}</span>
            </span>
          </div>
          <button type="button" className="btn btn--secondary btn--sm" onClick={onLogout} disabled={loggingOut}>
            Cerrar sesión
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
      <div className="sidebar-overlay" onClick={() => setMenuOpen(false)} aria-hidden="true" />
    </div>
  );
}
