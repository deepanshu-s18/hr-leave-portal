import { useEffect, useState } from 'react'
import { leaveApi } from '../services/api'
import toast from 'react-hot-toast'

export default function MyLeavesPage() {
  const [leaves, setLeaves] = useState<any[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)

  const fetchLeaves = async () => {
    setLoading(true)
    try {
      const res = await leaveApi.getMyLeaves(page, 10, statusFilter || undefined)
      setLeaves(res.data.items ?? [])
      setTotal(res.data.total ?? 0)
    } catch { toast.error('Failed to load leaves') }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchLeaves() }, [page, statusFilter])

  const handleCancel = async (id: number) => {
    if (!confirm('Cancel this leave request?')) return
    try {
      await leaveApi.cancel(id)
      toast.success('Leave request cancelled')
      fetchLeaves()
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Cannot cancel this request')
    }
  }

  const statusBadge = (s: string) => <span className={`badge badge-${s.toLowerCase()}`}>{s}</span>

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Leave Requests</h1>
        <p className="page-subtitle">Track all your leave applications</p>
      </div>

      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px', alignItems: 'center' }}>
        <select id="statusFilter" className="form-control" style={{ width: '160px' }}
          value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">All Status</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <span style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{total} total</span>
      </div>

      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading...</div>
        ) : leaves.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
            No leave requests found. <a href="/apply-leave" style={{ color: 'var(--primary)' }}>Apply now →</a>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Type</th><th>Start</th><th>End</th><th>Days</th>
                <th>Status</th><th>Reason</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {leaves.map((l: any) => (
                  <tr key={l.id}>
                    <td>{l.leaveType}</td>
                    <td>{l.startDate}</td>
                    <td>{l.endDate}</td>
                    <td>{l.workingDays}</td>
                    <td>{statusBadge(l.status)}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>{l.reason}</td>
                    <td>
                      {(l.status === 'PENDING' || l.status === 'APPROVED') && (
                        <button className="btn btn-danger btn-sm" onClick={() => handleCancel(l.id)}>
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {total > 10 && (
        <div style={{ display: 'flex', gap: '8px', marginTop: '16px', justifyContent: 'center' }}>
          <button className="btn btn-ghost btn-sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>← Prev</button>
          <span style={{ color: 'var(--text-muted)', alignSelf: 'center', fontSize: '13px' }}>Page {page + 1}</span>
          <button className="btn btn-ghost btn-sm" onClick={() => setPage(p => p + 1)} disabled={(page + 1) * 10 >= total}>Next →</button>
        </div>
      )}
    </div>
  )
}
