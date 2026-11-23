import type { ApiErrorResponse } from './types';

/**
 * Base de la API. Vacía por defecto: el frontend llama a rutas relativas (/api/v1/...) y
 * nginx (Docker) o el proxy de Vite (desarrollo) las reenvían a Spring Boot.
 * Con VITE_API_URL se puede apuntar a otra API (p. ej. Render).
 */
export const API_BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string;
  readonly details: string[];
  readonly traceId?: string;
  readonly retryAfterSeconds?: number;

  constructor(status: number, body: Partial<ApiErrorResponse>, retryAfterSeconds?: number) {
    super(body.message || `Error HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = body.errorCode ?? 'UNKNOWN_ERROR';
    this.details = body.details ?? [];
    this.traceId = body.traceId;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

type TokenProvider = () => string | null;
type UnauthorizedHandler = () => void;

let getToken: TokenProvider = () => null;
let onUnauthorized: UnauthorizedHandler = () => {};

/** El AuthContext registra aquí cómo leer el token y qué hacer si la sesión deja de ser válida. */
export function configureApiClient(tokenProvider: TokenProvider, unauthorizedHandler: UnauthorizedHandler) {
  getToken = tokenProvider;
  onUnauthorized = unauthorizedHandler;
}

type QueryValue = string | number | boolean | null | undefined;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, QueryValue>;
  /** Si es false no se envía el JWT aunque exista (registro, login). */
  auth?: boolean;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = `${API_BASE_URL}${path}`;
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value));
  });
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, auth = true } = options;
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = auth ? getToken() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, {
      errorCode: 'NETWORK_ERROR',
      message: 'No se pudo conectar con la API. Verifica que el backend esté en ejecución.',
    });
  }

  const text = await response.text();
  let data: unknown = undefined;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      // Cuerpo no JSON (p. ej. la página HTML de un 502 de nginx): no se muestra tal cual.
      data = {
        message:
          response.status >= 502 && response.status <= 504
            ? 'La API no está disponible en este momento. Intenta de nuevo en unos segundos.'
            : `Error HTTP ${response.status}`,
      };
    }
  }

  if (!response.ok) {
    const retryAfter = response.headers.get('Retry-After');
    const error = new ApiError(
      response.status,
      (data as Partial<ApiErrorResponse>) ?? {},
      retryAfter ? Number(retryAfter) : undefined,
    );
    // Un 401 con token significa que la sesión expiró o fue revocada.
    if (response.status === 401 && token) onUnauthorized();
    throw error;
  }

  return data as T;
}

const VALIDATION_FIELD_LABELS: Record<string, string> = {
  email: 'Correo',
  password: 'Contraseña',
  firstName: 'Nombre',
  lastName: 'Apellido',
  phone: 'Teléfono',
  businessName: 'Nombre del negocio',
  serviceType: 'Tipo de servicio',
  name: 'Nombre',
  durationMinutes: 'Duración',
  price: 'Precio',
  quantity: 'Cantidad',
  resourceType: 'Tipo de recurso',
  startTime: 'Hora de inicio',
  endTime: 'Hora de fin',
  blockDate: 'Fecha',
  dayOfWeek: 'Día',
  from: 'Desde',
  to: 'Hasta',
};

const MESSAGE_TRANSLATIONS: [RegExp, string][] = [
  [/^Invalid credentials$/i, 'Correo o contraseña incorrectos.'],
  [/^Email is already registered$/i, 'Ese correo ya está registrado.'],
  [/^Business name is already registered$/i, 'Ese nombre de negocio ya está registrado.'],
  [/^Authentication required$/i, 'Debes iniciar sesión.'],
  [/^Access denied$/i, 'No tienes permiso para realizar esta acción.'],
  [/^The requested time slot is not available$/i, 'Ese horario ya no está disponible.'],
  [/^The client already has a reservation at that time$/i, 'El cliente ya tiene una reserva a esa hora.'],
  [/^The reservation must start in the future$/i, 'La reserva debe ser en el futuro.'],
  [/^The reservation is already cancelled$/i, 'La reserva ya estaba cancelada.'],
  [/^Only future reservations can be cancelled$/i, 'Solo se pueden cancelar reservas futuras.'],
  [/^Only future reservations can be rescheduled$/i, 'Solo se pueden reprogramar reservas futuras.'],
  [/^Only confirmed reservations can be rescheduled$/i, 'Solo se pueden reprogramar reservas confirmadas.'],
  [/^The new time must be in the future$/i, 'El nuevo horario debe ser en el futuro.'],
  [/^A reason is required when a provider cancels a reservation$/i, 'Como proveedor debes indicar el motivo de la cancelación.'],
  [/^The range cannot exceed (\d+) days$/i, 'El rango no puede superar $1 días.'],
  [/^'from' must not be after 'to'$/i, 'La fecha "desde" no puede ser posterior a "hasta".'],
  [/^Client not found$/i, 'No se encontró un cliente activo con ese correo.'],
  [/^Service not found$/i, 'Servicio no encontrado.'],
  [/^Reservation not found$/i, 'Reserva no encontrada.'],
  [/^The service is not active$/i, 'El servicio no está activo.'],
  [/^Service is already inactive$/i, 'El servicio ya estaba inactivo.'],
  [/^A service with this name already exists for the provider$/i, 'Ya tienes un servicio con ese nombre.'],
  [/^A resource with this name already exists for the provider$/i, 'Ya tienes un recurso con ese nombre.'],
  [/^Resource already associated with this service$/i, 'Ese recurso ya está asociado al servicio.'],
  [/^Resource is inactive and cannot be used$/i, 'El recurso está inactivo.'],
  [/^Service is inactive and cannot be used$/i, 'El servicio está inactivo y no se puede modificar.'],
  [/^Schedule overlaps an existing attention window for the same day$/i, 'El horario se cruza con otra franja del mismo día.'],
  [/^Block overlaps an existing block for the same schedule and date$/i, 'El bloqueo se cruza con otro bloqueo de esa fecha.'],
  [/^startTime must be before endTime$/i, 'La hora de inicio debe ser anterior a la hora de fin.'],
  [/^Provider not found$/i, 'Perfil de proveedor no encontrado.'],
  [/^Block date weekday does not match the schedule day of week$/i, 'La fecha del bloqueo no cae en el día de la franja.'],
  [/^Block must be within the schedule attention window$/i, 'El bloqueo debe estar dentro del horario de la franja.'],
  [/^clientId is required when a provider creates a reservation$/i, 'Debes indicar el cliente.'],
  [/^The user is not an active client$/i, 'El usuario no es un cliente activo.'],
  [/^The service does not belong to the provider$/i, 'El servicio no pertenece a tu negocio.'],
];

/** Mensaje legible (en español cuando se conoce) para mostrar al usuario. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'ACCOUNT_LOCKED') {
      const minutes = error.retryAfterSeconds ? Math.ceil(error.retryAfterSeconds / 60) : null;
      return minutes
        ? `Cuenta bloqueada por intentos fallidos. Intenta de nuevo en ${minutes} min.`
        : 'Cuenta bloqueada temporalmente por intentos fallidos.';
    }
    let message = error.message;
    for (const [pattern, translation] of MESSAGE_TRANSLATIONS) {
      if (pattern.test(message)) {
        message = message.replace(pattern, translation);
        break;
      }
    }
    if (error.errorCode === 'VALIDATION_ERROR' && error.details.length > 0) {
      const details = error.details.map((detail) => {
        const [field, ...rest] = detail.split(':');
        const label = VALIDATION_FIELD_LABELS[field.trim()];
        return label ? `${label}:${rest.join(':')}` : detail;
      });
      return `Datos inválidos — ${details.join('; ')}`;
    }
    if (error.status === 403 && message === error.message && !/^No tienes/.test(message)) {
      return `No autorizado: ${message}`;
    }
    return message;
  }
  if (error instanceof Error) return error.message;
  return 'Ocurrió un error inesperado.';
}
