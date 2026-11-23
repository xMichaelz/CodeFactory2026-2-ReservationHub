import { Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { GuestOnlyRoute, ProtectedRoute } from './components/layout/ProtectedRoute';
import { PublicLayout } from './components/layout/PublicLayout';
import { HomePage } from './pages/HomePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterClientPage } from './pages/auth/RegisterClientPage';
import { RegisterProviderPage } from './pages/auth/RegisterProviderPage';
import { BookServicePage } from './pages/client/BookServicePage';
import { CatalogPage } from './pages/client/CatalogPage';
import { ClientHomePage } from './pages/client/ClientHomePage';
import { ProviderBookPage } from './pages/provider/ProviderBookPage';
import { ProviderDashboardPage } from './pages/provider/ProviderDashboardPage';
import { ReportsPage } from './pages/provider/ReportsPage';
import { ResourcesPage } from './pages/provider/ResourcesPage';
import { SchedulesPage } from './pages/provider/SchedulesPage';
import { ServicesPage } from './pages/provider/ServicesPage';
import { HistoryPage } from './pages/shared/HistoryPage';
import { ReservationsPage } from './pages/shared/ReservationsPage';

export function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route element={<GuestOnlyRoute />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterClientPage />} />
          <Route path="register/provider" element={<RegisterProviderPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['CLIENT']} />}>
        <Route path="client" element={<AppLayout />}>
          <Route index element={<ClientHomePage />} />
          <Route path="catalog" element={<CatalogPage />} />
          <Route path="book/:serviceId" element={<BookServicePage />} />
          <Route path="reservations" element={<ReservationsPage key="client" perspective="client" />} />
          <Route path="history" element={<HistoryPage key="client" perspective="client" />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['PROVIDER']} />}>
        <Route path="provider" element={<AppLayout />}>
          <Route index element={<ProviderDashboardPage />} />
          <Route path="services" element={<ServicesPage />} />
          <Route path="resources" element={<ResourcesPage />} />
          <Route path="schedules" element={<SchedulesPage />} />
          <Route path="agenda" element={<ReservationsPage key="provider" perspective="provider" />} />
          <Route path="book" element={<ProviderBookPage />} />
          <Route path="history" element={<HistoryPage key="provider" perspective="provider" />} />
          <Route path="reports" element={<ReportsPage />} />
        </Route>
      </Route>

      <Route element={<PublicLayout />}>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
