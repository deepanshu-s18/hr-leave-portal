import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { LayoutDashboard, CalendarPlus, Calendar, CheckSquare, Users, LogOut } from 'lucide-react'

export default function Layout() {
  const { user, logout, isManager } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = user?.fullName
    ?.split(' ')
    .map(n => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase() ?? '?'

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span>🏢</span> HR Portal
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <LayoutDashboard size={18} /> Dashboard
          </NavLink>
          <NavLink to="/apply-leave" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <CalendarPlus size={18} /> Apply Leave
          </NavLink>
          <NavLink to="/my-leaves" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <Calendar size={18} /> My Leaves
          </NavLink>
          {isManager && (
            <>
              <NavLink to="/approvals" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                <CheckSquare size={18} /> Approvals
              </NavLink>
              <NavLink to="/employees" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                <Users size={18} /> Employees
              </NavLink>
            </>
          )}
        </nav>
        <div className="sidebar-user">
          <div className="avatar">{initials}</div>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <div style={{ fontWeight: 600, fontSize: '13px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {user?.fullName}
            </div>
            <div style={{ color: 'var(--text-muted)', fontSize: '12px' }}>{user?.role}</div>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost btn-sm" title="Logout">
            <LogOut size={14} />
          </button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
