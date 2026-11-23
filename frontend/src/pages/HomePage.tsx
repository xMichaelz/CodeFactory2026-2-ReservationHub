import { Link } from 'react-router-dom';
import { homePathFor, useAuth } from '../context/AuthContext';

const FEATURES = [
  { icon: '🛍️', title: 'Catálogo de servicios', text: 'Explora los servicios activos de todos los proveedores.' },
  { icon: '🗓️', title: 'Disponibilidad real', text: 'Elige entre los huecos libres calculados según la agenda.' },
  { icon: '🔁', title: 'Cancela o reprograma', text: 'Gestiona tus reservas futuras en segundos.' },
  { icon: '🧾', title: 'Gestión del negocio', text: 'Servicios, recursos, horarios y bloqueos para proveedores.' },
  { icon: '📈', title: 'Reportes', text: 'Ocupación, demanda y cancelaciones de tu agenda.' },
  { icon: '🔐', title: 'Seguro', text: 'Autenticación con JWT y bloqueo por intentos fallidos.' },
];

export function HomePage() {
  const { session } = useAuth();
  return (
    <div className="landing">
      <section className="hero">
        <span className="hero__tag">Plataforma de reservas · CodeF@ctory UdeA</span>
        <h1 className="hero__title">
          Reserva servicios <span className="hero__accent">sin llamadas ni esperas</span>
        </h1>
        <p className="hero__text">
          ReservaHub conecta a clientes con proveedores de servicios. Los clientes reservan en los horarios libres y
          los proveedores administran su agenda, recursos y reportes desde un solo lugar.
        </p>
        <div className="hero__actions">
          {session ? (
            <Link className="btn btn--primary" to={homePathFor(session.role)}>
              Ir a mi panel
            </Link>
          ) : (
            <>
              <Link className="btn btn--primary" to="/register">
                Soy cliente
              </Link>
              <Link className="btn btn--secondary" to="/register/provider">
                Soy proveedor
              </Link>
              <Link className="btn btn--ghost" to="/login">
                Ya tengo cuenta →
              </Link>
            </>
          )}
        </div>
      </section>
      <section className="feature-grid">
        {FEATURES.map((f) => (
          <article key={f.title} className="feature">
            <span className="feature__icon" aria-hidden="true">
              {f.icon}
            </span>
            <h3>{f.title}</h3>
            <p>{f.text}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
