// Un objeto por dominio del backend. Cada función corresponde a un endpoint de Swagger.
import { request } from './client';
import type {
  AvailabilityResponse,
  CancellationReport,
  CancelReservationRequest,
  CatalogEntry,
  ClientLookup,
  CreateProviderRequest,
  CreateReservationRequest,
  CreateResourceRequest,
  CreateScheduleBlockRequest,
  CreateScheduleRequest,
  CreateUserRequest,
  DateRange,
  DemandReport,
  LoginRequest,
  LoginResponse,
  MessageResponse,
  OccupancyReport,
  ProviderResponse,
  ProviderSchedule,
  ProviderService,
  RescheduleReservationRequest,
  ReservationHistoryItem,
  ReservationResponse,
  ResourceResponse,
  ScheduleBlockResponse,
  ScheduleResponse,
  ServiceRequest,
  ServiceResponse,
  UserResponse,
  UUID,
} from './types';

const V1 = '/api/v1';

/** HU-03, HU-04 */
export const authApi = {
  login: (body: LoginRequest) =>
    request<LoginResponse>(`${V1}/auth/login`, { method: 'POST', body, auth: false }),
  logout: () => request<MessageResponse>(`${V1}/auth/logout`, { method: 'POST' }),
};

/** HU-01 + búsqueda de clientes para el proveedor */
export const usersApi = {
  register: (body: CreateUserRequest) =>
    request<UserResponse>(`${V1}/users`, { method: 'POST', body, auth: false }),
  lookup: (email: string) => request<ClientLookup>(`${V1}/users/lookup`, { query: { email } }),
};

/** HU-02 */
export const providersApi = {
  register: (body: CreateProviderRequest) =>
    request<ProviderResponse>(`${V1}/providers`, { method: 'POST', body, auth: false }),
};

/** HU-05, HU-06, HU-07 + asociación de recursos */
export const servicesApi = {
  mine: () => request<ProviderService[]>(`${V1}/services/mine`),
  create: (body: ServiceRequest) =>
    request<ServiceResponse>(`${V1}/services`, { method: 'POST', body }),
  update: (id: UUID, body: ServiceRequest) =>
    request<ServiceResponse>(`${V1}/services/${id}`, { method: 'PUT', body }),
  deactivate: (id: UUID) =>
    request<ServiceResponse>(`${V1}/services/${id}/deactivate`, { method: 'PATCH' }),
  linkResources: (id: UUID, resourceIds: UUID[]) =>
    request<void>(`${V1}/services/${id}/resources`, { method: 'POST', body: { resourceIds } }),
  /** HU-11 */
  availability: (serviceId: UUID, range: DateRange) =>
    request<AvailabilityResponse>(`${V1}/services/${serviceId}/availability`, { query: { ...range } }),
};

export const catalogApi = {
  services: () => request<CatalogEntry[]>(`${V1}/catalog/services`),
};

/** HU-10 */
export const resourcesApi = {
  mine: () => request<ResourceResponse[]>(`${V1}/resources/mine`),
  create: (body: CreateResourceRequest) =>
    request<ResourceResponse>(`${V1}/resources`, { method: 'POST', body }),
};

/** HU-08, HU-09 */
export const schedulesApi = {
  mine: () => request<ProviderSchedule[]>(`${V1}/schedules/mine`),
  create: (body: CreateScheduleRequest) =>
    request<ScheduleResponse>(`${V1}/schedules`, { method: 'POST', body }),
  createBlock: (scheduleId: UUID, body: CreateScheduleBlockRequest) =>
    request<ScheduleBlockResponse>(`${V1}/schedules/${scheduleId}/blocks`, { method: 'POST', body }),
};

/** HU-12 .. HU-16 */
export const reservationsApi = {
  create: (body: CreateReservationRequest) =>
    request<ReservationResponse>(`${V1}/reservations`, { method: 'POST', body }),
  mine: (range: DateRange) =>
    request<ReservationResponse[]>(`${V1}/reservations/mine`, { query: { ...range } }),
  cancel: (id: UUID, body: CancelReservationRequest) =>
    request<ReservationResponse>(`${V1}/reservations/${id}/cancel`, { method: 'PATCH', body }),
  reschedule: (id: UUID, body: RescheduleReservationRequest) =>
    request<ReservationResponse>(`${V1}/reservations/${id}/reschedule`, { method: 'PATCH', body }),
};

/** HU-17 .. HU-21 */
export const reportsApi = {
  myReservations: (range: DateRange) =>
    request<ReservationHistoryItem[]>(`${V1}/reports/my-reservations`, { query: { ...range } }),
  businessReservations: (range: DateRange) =>
    request<ReservationHistoryItem[]>(`${V1}/reports/business-reservations`, { query: { ...range } }),
  occupancy: (range: DateRange, serviceId?: UUID) =>
    request<OccupancyReport>(`${V1}/reports/occupancy`, { query: { ...range, serviceId } }),
  demand: (range: DateRange) =>
    request<DemandReport>(`${V1}/reports/demand`, { query: { ...range } }),
  cancellations: (range: DateRange, serviceId?: UUID) =>
    request<CancellationReport>(`${V1}/reports/cancellations`, { query: { ...range, serviceId } }),
};
