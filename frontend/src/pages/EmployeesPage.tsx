import { useEffect, useState } from 'react'
import { employeeApi } from '../services/api'
import toast from 'react-hot-toast'
import { Users } from 'lucide-react'

export default function EmployeesPage() {
  const [employees, setEmployees] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    employeeApi.getAll()
      .then(res => setEmployees(res.data.items ?? []))
      .catch(() => toast.error('Failed to load employees'))
      .finally(() => setLoading(false))
  }, [])

  const roleBadge = (role: string) => {
    const colors: Record<string, string> = {
      ADMIN: 'var(--danger)', MANAGER: 'var(--primary)',
      HR: 'var(--secondary)', EMPLOYEE: 'var(--success)'
    }
    return (
      <span className="badge" style={{ background: `${colors[role] ?? '#ccc'}22`, color: colors[role] ?? '#ccc' }}>
        {role}
      </span>
    )
  }

  return (
    <div>
      <div className="page-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h1 className="page-title">Employees</h1>
            <p className="page-subtitle">View all employees and their details</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)' }}>
            <Users size={16} /> {employees.length} employees
          </div>
        </div>
      </div>

      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading...</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Employee</th><th>ID</th><th>Department</th>
                <th>Role</th><th>Annual Balance</th><th>Sick Balance</th>
              </tr></thead>
              <tbody>
                {employees.map((e: any) => (
                  <tr key={e.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div className="avatar" style={{ width: '32px', height: '32px', fontSize: '12px' }}>
                          {e.fullName?.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2)}
                        </div>
                        <div>
                          <div style={{ fontWeight: 600 }}>{e.fullName}</div>
                          <div style={{ color: 'var(--text-muted)', fontSize: '12px' }}>{e.email}</div>
                        </div>
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontFamily: 'monospace' }}>{e.employeeId}</td>
                    <td>{e.department}</td>
                    <td>{roleBadge(e.role)}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <div style={{ flex: 1, height: '6px', background: 'var(--border)', borderRadius: '3px' }}>
                          <div style={{ width: `${(e.annualLeaveBalance / 21) * 100}%`, height: '100%', background: 'var(--primary)', borderRadius: '3px' }} />
                        </div>
                        <span style={{ fontSize: '12px', color: 'var(--text-muted)', minWidth: '30px' }}>{e.annualLeaveBalance}</span>
                      </div>
                    </td>
                    <td>{e.sickLeaveBalance}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
