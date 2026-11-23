// Contratos de la API de ReservaHub (espejo de los DTO de Spring Boot).
// Fechas: 'YYYY-MM-DD'. Horas: 'HH:mm' o 'HH:mm:ss'. Instantes: ISO-8601.

export type UUID = string;
export type Role = 'CLIENT' | 'PROVIDER' | 'ADMIN';
export type EntityStatus = 'ACTIVE' | 'INACTIVE';
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';
export type ReservationStatus = 'CONFIRMED' | 'CANCELLED_BY_CLIENT' | 'CANCELLED_BY_PROVIDER';
export type ReservationLifecycle = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface ApiErrorResponse {
  errorCode: string;
  message: string;
  details: string[];
  traceId: string;
}

// ---------- Auth / usuarios ----------
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: UUID;
  email: string;
  role: Role;
}

export interface MessageResponse {
  message: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UserResponse {
  id: UUID;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: Role;
  status: EntityStatus;
  createdAt: string;
}

export interface ClientLookup {
  id: UUID;
  name: string;
}

export interface CreateProviderRequest extends CreateUserRequest {
  businessName: string;
  serviceType: string;
  description?: string;
  address?: string;
}

export interface ProviderResponse {
  id: UUID;
  userId: UUID;
  email: string;
  firstName: string;
  lastName: string;
  businessName: string;
  serviceType: string;
  description: string | null;
  address: string | null;
  status: EntityStatus;
  createdAt: string;
}

// ---------- Servicios y catálogo ----------
export interface ServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
}

export interface ServiceResponse {
  id: UUID;
  providerId: UUID;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  status: EntityStatus;
  createdAt: string;
  updatedAt: string | null;
}

export interface ProviderService extends ServiceResponse {
  resources: ResourceResponse[];
}

export interface CatalogEntry {
  id: UUID;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  providerId: UUID;
  providerName: string;
}

// ---------- Recursos ----------
export interface CreateResourceRequest {
  name: string;
  resourceType: string;
  quantity: number;
  description?: string;
}

export interface ResourceResponse {
  id: UUID;
  providerId: UUID;
  name: string;
  resourceType: string;
  quantity: number;
  description: string | null;
  status: EntityStatus;
  createdAt: string;
}

// ---------- Horarios ----------
export interface CreateScheduleRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface ScheduleResponse {
  id: UUID;
  providerId: UUID;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  createdAt: string;
}

export interface CreateScheduleBlockRequest {
  blockDate: string;
  startTime: string;
  endTime: string;
  reason?: string;
}

export interface ScheduleBlockResponse {
  id: UUID;
  scheduleId: UUID;
  blockDate: string;
  startTime: string;
  endTime: string;
  reason: string | null;
  createdAt: string;
}

export interface ProviderSchedule extends ScheduleResponse {
  blocks: ScheduleBlockResponse[];
}

// ---------- Disponibilidad y reservas ----------
export interface Slot {
  start: string;
  end: string;
}

export interface AvailabilityDay {
  date: string;
  slots: Slot[];
}

export interface AvailabilityResponse {
  serviceId: UUID;
  days: AvailabilityDay[];
}

export interface CreateReservationRequest {
  serviceId: UUID;
  date: string;
  startTime: string;
  clientId?: UUID;
}

export interface RescheduleReservationRequest {
  date: string;
  startTime: string;
}

export interface CancelReservationRequest {
  reason?: string;
}

export interface ReservationResponse {
  id: UUID;
  serviceId: UUID;
  serviceName: string;
  providerId: UUID;
  providerName: string;
  clientId: UUID;
  date: string;
  startTime: string;
  endTime: string;
  status: ReservationStatus;
  createdBy: UUID;
  createdAt: string;
}

// ---------- Reportes ----------
export interface ReservationHistoryItem {
  id: UUID;
  serviceId: UUID;
  serviceName: string;
  providerId: UUID;
  providerName: string;
  clientId: UUID;
  clientName: string;
  date: string;
  startTime: string;
  endTime: string;
  status: ReservationStatus;
  lifecycle: ReservationLifecycle;
}

export interface OccupancyRow {
  offeredSlots: number;
  occupiedSlots: number;
  freeSlots: number;
  occupancyRate: number;
}

export interface OccupancyReport extends OccupancyRow {
  from: string;
  to: string;
  byService: (OccupancyRow & { serviceId: UUID; serviceName: string })[];
  byDate: (OccupancyRow & { date: string })[];
}

export interface DemandReport {
  from: string;
  to: string;
  totalDelivered: number;
  byService: { serviceId: UUID; serviceName: string; deliveredCount: number }[];
}

export interface CancellationReport {
  from: string;
  to: string;
  cancelledByClient: number;
  cancelledByProvider: number;
  total: number;
  byService: {
    serviceId: UUID;
    serviceName: string;
    cancelledByClient: number;
    cancelledByProvider: number;
  }[];
}

export interface DateRange {
  from: string;
  to: string;
}
