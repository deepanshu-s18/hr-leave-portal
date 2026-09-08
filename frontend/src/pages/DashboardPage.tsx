import { useEffect, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { leaveApi } from '../services/api'
import { Calendar, Clock, CheckCircle, XCircle } from 'lucide-react'

interface LeaveSummary {
  pending: number
  approved: number
  rejected: number
  annualBalance: number
  sickBalance: number
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [summary, setSummary] = useState<LeaveSummary | null>(null)
  const [recentLeaves, setRecentLeaves] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await leaveApi.getMyLeaves(0, 5)
        const leaves = res.data.items ?? []
        setRecentLeaves(leaves)
        setSummary({
          pending: leaves.filter((l: any) => l.status === 'PENDING').length,
          approved: leaves.filter((l: any) => l.status === 'APPROVED').length,
          rejected: leaves.filter((l: any) => l.status === 'REJECTED').length,
          annualBalance: 21,
          sickBalance: 10,
        })
      } catch {
        setSummary({ pending: 0, approved: 0, rejected: 0, annualBalance: 21, sickBalance: 10 })
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  const statusBadge = (status: string) => (
    <span className={`badge badge-${status.toLowerCase()}`}>{status}</span>
  )

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Welcome back, {user?.fullName?.split(' ')[0]} 👋</h1>
        <p className="page-subtitle">Here's your leave summary for this year</p>
      </div>

      {!loading && (
        <>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(245,158,11,0.15)' }}><Clock size={22} color="var(--warning)" /></div>
              <div><div className="stat-value">{summary?.pending ?? 0}</div><div className="stat-label">Pending Requests</div></div>
            </div>
            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(16,185,129,0.15)' }}><CheckCircle size={22} color="var(--success)" /></div>
              <div><div className="stat-value">{summary?.approved ?? 0}</div><div className="stat-label">Approved</div></div>
            </div>
            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(239,68,68,0.15)' }}><XCircle size={22} color="var(--danger)" /></div>
              <div><div className="stat-value">{summary?.rejected ?? 0}</div><div className="stat-label">Rejected</div></div>
            </div>
            <div className="stat-card">
              <div className="stat-icon" style={{ background: 'rgba(99,102,241,0.15)' }}><Calendar size={22} color="var(--primary)" /></div>
              <div><div className="stat-value">{summary?.annualBalance ?? 0}</div><div className="stat-label">Annual Balance</div></div>
            </div>
          </div>

          <div className="card">
            <div className="card-title" style={{ marginBottom: '20px' }}>Recent Leave Requests</div>
            {recentLeaves.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '32px' }}>
                No leave requests yet. <a href="/apply-leave" style={{ color: 'var(--primary)' }}>Apply now</a>
              </div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Type</th><th>Start</th><th>End</th><th>Days</th><th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentLeaves.map((l: any) => (
                      <tr key={l.id}>
                        <td>{l.leaveType}</td>
                        <td>{l.startDate}</td>
                        <td>{l.endDate}</td>
                        <td>{l.workingDays}</td>
                        <td>{statusBadge(l.status)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
