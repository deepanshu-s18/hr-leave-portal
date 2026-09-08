import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const [usernameOrEmail, setUsernameOrEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!usernameOrEmail || !password) {
      toast.error('Please enter your credentials')
      return
    }
    setLoading(true)
    try {
      await login(usernameOrEmail, password)
      toast.success('Welcome back!')
      navigate('/dashboard')
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">🏢 HR Leave Portal</div>
        <p className="login-subtitle">Sign in to manage your leave requests</p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Username or Email</label>
            <input
              id="usernameOrEmail"
              className="form-control"
              type="text"
              placeholder="deepanshu or deepanshu@company.com"
              value={usernameOrEmail}
              onChange={e => setUsernameOrEmail(e.target.value)}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              id="password"
              className="form-control"
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>

          <button id="login-btn" type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%', justifyContent: 'center', marginTop: '8px' }}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div style={{ marginTop: '32px', padding: '16px', background: 'rgba(99,102,241,0.08)', borderRadius: '10px', fontSize: '13px', color: 'var(--text-muted)' }}>
          <div style={{ marginBottom: '8px', fontWeight: 600, color: 'var(--text)' }}>Demo Credentials</div>
          <div>Employee: <code>deepanshu</code> / <code>User@1234</code></div>
          <div>Admin: <code>admin</code> / <code>Admin@123</code></div>
        </div>
      </div>
    </div>
  )
}
