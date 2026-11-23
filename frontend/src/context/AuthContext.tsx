import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { configureApiClient } from '../api/client';
import { authApi } from '../api/endpoints';
import type { LoginRequest, Role, UUID } from '../api/types';

const STORAGE_KEY = 'reservahub.session';

export interface Session {
  token: string;
  userId: UUID;
  email: string;
  role: Role;
  expiresAt: number;
}

interface AuthContextValue {
  session: Session | null;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<Session>;
  logout: () => Promise<void>;
  /** Mensaje cuando la sesión terminó sola (expiró o fue revocada). */
  sessionNotice: string | null;
  clearSessionNotice: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    if (!session.token || session.expiresAt <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

function storeSession(session: Session | null) {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Almacenamiento no disponible (modo privado): la sesión vive solo en memoria.
  }
}

// Variable de módulo para que el cliente HTTP lea siempre el token vigente.
let currentToken: string | null = null;
// Lo que hace el cliente HTTP ante un 401 con token; el provider lo reemplaza al montarse.
let handleUnauthorized: () => void = () => {
  currentToken = null;
};

// Se configura al cargar el módulo, antes de cualquier efecto: React ejecuta los efectos de los
// hijos antes que los del provider, así que configurarlo en un useEffect dejaba sin token las
// primeras peticiones tras recargar la página.
configureApiClient(
  () => currentToken,
  () => handleUnauthorized(),
);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => {
    const stored = readStoredSession();
    currentToken = stored?.token ?? null;
    return stored;
  });
  const [sessionNotice, setSessionNotice] = useState<string | null>(null);

  const applySession = useCallback((next: Session | null) => {
    currentToken = next?.token ?? null;
    storeSession(next);
    setSession(next);
  }, []);

  handleUnauthorized = () => {
    applySession(null);
    setSessionNotice('Tu sesión expiró o fue cerrada. Inicia sesión de nuevo.');
  };

  // Cierra la sesión local cuando el JWT vence.
  useEffect(() => {
    if (!session) return;
    const remaining = session.expiresAt - Date.now();
    const timer = window.setTimeout(() => {
      applySession(null);
      setSessionNotice('Tu sesión expiró. Inicia sesión de nuevo.');
    }, Math.max(remaining, 0));
    return () => window.clearTimeout(timer);
  }, [session, applySession]);

  const login = useCallback(
    async (credentials: LoginRequest) => {
      const response = await authApi.login(credentials);
      const next: Session = {
        token: response.accessToken,
        userId: response.userId,
        email: response.email,
        role: response.role,
        expiresAt: Date.now() + response.expiresInMs,
      };
      applySession(next);
      setSessionNotice(null);
      return next;
    },
    [applySession],
  );

  const logout = useCallback(async () => {
    try {
      if (currentToken) await authApi.logout();
    } catch {
      // Aunque la API falle, la sesión local se cierra igual.
    } finally {
      applySession(null);
      // Un logout voluntario no debe mostrar el aviso de "sesión expirada" aunque la API respondiera 401.
      setSessionNotice(null);
    }
  }, [applySession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      login,
      logout,
      sessionNotice,
      clearSessionNotice: () => setSessionNotice(null),
    }),
    [session, login, logout, sessionNotice],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}

export function homePathFor(role: Role | undefined): string {
  if (role === 'PROVIDER') return '/provider';
  if (role === 'CLIENT') return '/client';
  return '/';
}
