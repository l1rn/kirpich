import { Spin } from 'antd';
import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router';
import { isClient, useAuth } from './auth';
import { AppLayout } from './components/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { BankApplicationPage } from './pages/bank/BankApplicationPage';
import { QueuePage } from './pages/bank/QueuePage';
import { ApplicationPage } from './pages/client/ApplicationPage';
import { ApplicationsPage } from './pages/client/ApplicationsPage';
import { NewApplicationPage } from './pages/client/NewApplicationPage';

function RequireRole({ bank, children }: { bank: boolean; children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <Spin fullscreen />;
  if (!user) return <Navigate to="/login" replace />;
  const allowed = bank ? user.role === 'UNDERWRITER' : isClient(user);
  if (!allowed) return <Navigate to={user.role === 'UNDERWRITER' ? '/bank' : '/applications'} replace />;
  return children;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AppLayout />}>
        <Route path="/applications" element={<RequireRole bank={false}><ApplicationsPage /></RequireRole>} />
        <Route path="/applications/new" element={<RequireRole bank={false}><NewApplicationPage /></RequireRole>} />
        <Route path="/applications/:id" element={<RequireRole bank={false}><ApplicationPage /></RequireRole>} />
        <Route path="/bank" element={<RequireRole bank><QueuePage /></RequireRole>} />
        <Route path="/bank/applications/:id" element={<RequireRole bank><BankApplicationPage /></RequireRole>} />
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
