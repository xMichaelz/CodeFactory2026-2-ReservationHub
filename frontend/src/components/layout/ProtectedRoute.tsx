import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../../api/types';
import { homePathFor, useAuth } from '../../context/AuthContext';

/** Exige sesión y, opcionalmente, un rol. Redirige al login o al inicio del rol correcto. */
export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { session } = useAuth();
  const location = useLocation();

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(session.role)) {
    return <Navigate to={homePathFor(session.role)} replace />;
  }
  return <Outlet />;
}

/** Para login/registro: si ya hay sesión, lleva al inicio del rol. */
export function GuestOnlyRoute() {
  const { session } = useAuth();
  if (session) return <Navigate to={homePathFor(session.role)} replace />;
  return <Outlet />;
}
