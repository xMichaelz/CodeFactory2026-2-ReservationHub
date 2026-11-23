import { Link } from 'react-router-dom';
import { EmptyState } from '../components/ui';

export function NotFoundPage() {
  return (
    <div className="centered-page">
      <EmptyState
        icon="🧭"
        title="Página no encontrada"
        action={
          <Link className="btn btn--primary" to="/">
            Volver al inicio
          </Link>
        }
      >
        La dirección que buscas no existe.
      </EmptyState>
    </div>
  );
}
