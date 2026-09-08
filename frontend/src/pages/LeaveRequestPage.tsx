import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { leaveApi } from '../services/api'
import toast from 'react-hot-toast'
import { CalendarPlus } from 'lucide-react'

const LEAVE_TYPES = ['ANNUAL', 'SICK', 'CASUAL', 'MATERNITY', 'PATERNITY', 'UNPAID', 'COMPENSATORY']

export default function LeaveRequestPage() {
  const [form, setForm] = useState({
    leaveType: 'ANNUAL',
    startDate: '',
    endDate: '',
    reason: '',
  })
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.startDate || !form.endDate) { toast.error('Please select dates'); return }
    if (form.endDate < form.startDate) { toast.error('End date must be after start date'); return }
    if (!form.reason.trim()) { toast.error('Please provide a reason'); return }

    setLoading(true)
    try {
      await leaveApi.create(form)
      toast.success('Leave request submitted successfully!')
      navigate('/my-leaves')
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Failed to submit request')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Apply for Leave</h1>
        <p className="page-subtitle">Submit a new leave request for manager approval</p>
      </div>

      <div className="card" style={{ maxWidth: '560px' }}>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Leave Type</label>
            <select id="leaveType" name="leaveType" className="form-control" value={form.leaveType} onChange={handleChange}>
              {LEAVE_TYPES.map(t => <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()} Leave</option>)}
            </select>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Start Date</label>
              <input id="startDate" name="startDate" type="date" className="form-control"
                value={form.startDate} onChange={handleChange}
                min={new Date().toISOString().split('T')[0]} />
            </div>
            <div className="form-group">
              <label className="form-label">End Date</label>
              <input id="endDate" name="endDate" type="date" className="form-control"
                value={form.endDate} onChange={handleChange}
                min={form.startDate || new Date().toISOString().split('T')[0]} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Reason</label>
            <textarea id="reason" name="reason" className="form-control"
              placeholder="Please provide a reason for your leave request..."
              value={form.reason} onChange={handleChange} />
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button id="submit-leave" type="submit" className="btn btn-primary" disabled={loading}>
              <CalendarPlus size={16} />
              {loading ? 'Submitting...' : 'Submit Request'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => navigate('/my-leaves')}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
