import type { Role } from '../../api/types';

export interface NavItem {
  to: string;
  label: string;
  icon: string;
  end?: boolean;
}

export const NAVIGATION: Record<Exclude<Role, 'ADMIN'>, NavItem[]> = {
  CLIENT: [
    { to: '/client', label: 'Inicio', icon: '🏠', end: true },
    { to: '/client/catalog', label: 'Catálogo', icon: '🛍️' },
    { to: '/client/reservations', label: 'Mis reservas', icon: '📅' },
    { to: '/client/history', label: 'Historial', icon: '🕘' },
  ],
  PROVIDER: [
    { to: '/provider', label: 'Panel', icon: '📊', end: true },
    { to: '/provider/services', label: 'Servicios', icon: '🧾' },
    { to: '/provider/resources', label: 'Recursos', icon: '🧰' },
    { to: '/provider/schedules', label: 'Horarios', icon: '⏰' },
    { to: '/provider/agenda', label: 'Agenda', icon: '📅' },
    { to: '/provider/book', label: 'Reservar a cliente', icon: '➕' },
    { to: '/provider/history', label: 'Historial', icon: '🕘' },
    { to: '/provider/reports', label: 'Reportes', icon: '📈' },
  ],
};

export const ROLE_LABELS: Record<Role, string> = {
  CLIENT: 'Cliente',
  PROVIDER: 'Proveedor',
  ADMIN: 'Administrador',
};
