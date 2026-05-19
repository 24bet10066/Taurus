import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { lazy, Suspense } from 'react';
import { Loader2 } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';

// Eager — the public landing must load fast (it's the customer entry point).
import CustomerBooking from './pages/CustomerBooking';

// Lazy — admin / tech surfaces are gated behind auth. Customers never
// download this code, so first-time landing on /? stays small.
const LoginPage     = lazy(() => import('./pages/LoginPage'));
const ServiceOS     = lazy(() => import('./pages/ServiceOS'));
const TechnicianPWA = lazy(() => import('./pages/TechnicianPWA'));
const Unauthorized  = lazy(() => import('./pages/Unauthorized'));

const qc = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 20_000,
      refetchOnWindowFocus: false,
    },
  },
});

function ProtectedRoute({ roles, children }) {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(role)) return <Navigate to="/unauthorized" replace />;
  return children;
}

function RootRedirect() {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/" replace />;
  if (role === 'ADMIN') return <Navigate to="/admin" replace />;
  if (role === 'TECHNICIAN_HIRED') return <Navigate to="/tech" replace />;
  return <Navigate to="/unauthorized" replace />;
}

// Minimal fallback for lazy-loaded routes (also reads in Banda 4G).
function RouteSpinner() {
  return (
    <div className="min-h-dvh flex items-center justify-center bg-canvas">
      <div className="flex flex-col items-center gap-3">
        <div className="w-12 h-12 rounded-2xl bg-brand text-white font-display font-extrabold flex items-center justify-center text-base">
          SK
        </div>
        <Loader2 size={22} className="text-brand animate-spin" />
      </div>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<RouteSpinner />}>
            <Routes>
              {/* Public — landing is eager-loaded above */}
              <Route path="/"             element={<CustomerBooking />} />
              <Route path="/login"        element={<LoginPage />} />
              <Route path="/unauthorized" element={<Unauthorized />} />

              {/* Admin */}
              <Route path="/admin" element={
                <ProtectedRoute roles={['ADMIN']}>
                  <ServiceOS />
                </ProtectedRoute>
              } />

              {/* Technician */}
              <Route path="/tech" element={
                <ProtectedRoute roles={['TECHNICIAN_HIRED', 'TECHNICIAN_FREE']}>
                  <TechnicianPWA />
                </ProtectedRoute>
              } />

              {/* Catch-all */}
              <Route path="*" element={<RootRedirect />} />
            </Routes>
          </Suspense>
        </BrowserRouter>

        <Toaster
          position="top-center"
          toastOptions={{
            duration: 3200,
            style: {
              background: '#FFFFFF',
              color: '#1A1209',
              border: '1px solid #EDE6D9',
              borderRadius: '14px',
              fontSize: '14px',
              fontWeight: 500,
              fontFamily: '"Plus Jakarta Sans", system-ui, sans-serif',
              boxShadow: '0 8px 24px rgba(26,18,9,0.10), 0 2px 6px rgba(26,18,9,0.04)',
              padding: '12px 14px',
            },
            success: { iconTheme: { primary: '#047857', secondary: '#FFFFFF' } },
            error:   { iconTheme: { primary: '#B91C1C', secondary: '#FFFFFF' } },
          }}
        />
      </AuthProvider>
    </QueryClientProvider>
  );
}
