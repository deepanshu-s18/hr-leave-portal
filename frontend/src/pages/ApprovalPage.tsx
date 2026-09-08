import { useEffect, useState } from 'react'
import { leaveApi } from '../services/api'
import toast from 'react-hot-toast'
import { Check, X } from 'lucide-react'

export default function ApprovalPage() {
  const [leaves, setLeaves] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [commentModal, setCommentModal] = useState<{ id: number; type: 'approve' | 'reject' } | null>(null)
  const [comment, setComment] = useState('')

  const fetchPending = async () => {
    setLoading(true)
    try {
      const res = await leaveApi.getPendingForManager()
      setLeaves(res.data.items ?? [])
    } catch { toast.error('Failed to load pending requests') }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchPending() }, [])

  const handleAction = async () => {
    if (!commentModal) return
    try {
      if (commentModal.type === 'approve') {
        await leaveApi.approve(commentModal.id, comment)
        toast.success('Leave approved ✓')
      } else {
        if (!comment.trim()) { toast.error('Please provide a rejection reason'); return }
        await leaveApi.reject(commentModal.id, comment)
        toast.success('Leave rejected')
      }
      setCommentModal(null)
      setComment('')
      fetchPending()
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Action failed')
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Pending Approvals</h1>
        <p className="page-subtitle">Review and action your team's leave requests</p>
      </div>

      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading...</div>
        ) : leaves.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
            🎉 No pending requests — you're all caught up!
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Employee</th><th>Type</th><th>Start</th><th>End</th>
                <th>Days</th><th>Reason</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {leaves.map((l: any) => (
                  <tr key={l.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{l.employee?.fullName}</div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '12px' }}>{l.employee?.department}</div>
                    </td>
                    <td>{l.leaveType}</td>
                    <td>{l.startDate}</td>
                    <td>{l.endDate}</td>
                    <td>{l.workingDays}</td>
                    <td style={{ maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>{l.reason}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button id={`approve-${l.id}`} className="btn btn-success btn-sm"
                          onClick={() => { setCommentModal({ id: l.id, type: 'approve' }); setComment('') }}>
                          <Check size={14} /> Approve
                        </button>
                        <button id={`reject-${l.id}`} className="btn btn-danger btn-sm"
                          onClick={() => { setCommentModal({ id: l.id, type: 'reject' }); setComment('') }}>
                          <X size={14} /> Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Comment Modal */}
      {commentModal && (
        <div className="modal-overlay" onClick={() => setCommentModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {commentModal.type === 'approve' ? '✅ Approve Leave' : '❌ Reject Leave'}
              </h2>
              <button className="btn btn-ghost btn-sm" onClick={() => setCommentModal(null)}>✕</button>
            </div>
            <div className="form-group">
              <label className="form-label">
                {commentModal.type === 'approve' ? 'Comment (optional)' : 'Rejection Reason (required)'}
              </label>
              <textarea
                id="approval-comment"
                className="form-control"
                placeholder={commentModal.type === 'approve' ? 'Any notes for the employee...' : 'Please explain why this leave is rejected...'}
                value={comment}
                onChange={e => setComment(e.target.value)}
              />
            </div>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
              <button className="btn btn-ghost" onClick={() => setCommentModal(null)}>Cancel</button>
              <button
                id="confirm-action"
                className={`btn ${commentModal.type === 'approve' ? 'btn-success' : 'btn-danger'}`}
                onClick={handleAction}
              >
                {commentModal.type === 'approve' ? 'Confirm Approval' : 'Confirm Rejection'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
