import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { AuthProvider, useAuth } from './hooks/useAuth'
import ErrorBoundary from './components/ErrorBoundary'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import LeaveRequestPage from './pages/LeaveRequestPage'
import MyLeavesPage from './pages/MyLeavesPage'
import ApprovalPage from './pages/ApprovalPage'
import EmployeesPage from './pages/EmployeesPage'
import Layout from './components/Layout'
import './index.css'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  if (isLoading) return <div style={{ color: 'var(--text-muted)', padding: '40px', textAlign: 'center' }}>Loading...</div>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function ManagerRoute({ children }: { children: React.ReactNode }) {
  const { user, isManager } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!isManager) return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: 'var(--bg-card)', color: 'var(--text)',
              border: '1px solid var(--border)', borderRadius: '10px',
            }
          }}
        />
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="apply-leave" element={<LeaveRequestPage />} />
              <Route path="my-leaves" element={<MyLeavesPage />} />
              <Route path="approvals" element={<ManagerRoute><ApprovalPage /></ManagerRoute>} />
              <Route path="employees" element={<ManagerRoute><EmployeesPage /></ManagerRoute>} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  )
}
