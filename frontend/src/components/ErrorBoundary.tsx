import { Component, ErrorInfo, ReactNode } from 'react'

interface Props { children: ReactNode; fallback?: ReactNode }
interface State { hasError: boolean; error: Error | null }

/**
 * React Error Boundary — catches any render/lifecycle errors in the subtree
 * and displays a graceful fallback UI instead of crashing the whole app.
 */
export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary] Uncaught error:', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', minHeight: '400px', gap: '16px',
          color: 'var(--text)', textAlign: 'center', padding: '40px'
        }}>
          <div style={{ fontSize: '48px' }}>⚠️</div>
          <h2 style={{ fontSize: '20px', fontWeight: 700 }}>Something went wrong</h2>
          <p style={{ color: 'var(--text-muted)', maxWidth: '400px' }}>
            {this.state.error?.message ?? 'An unexpected error occurred'}
          </p>
          <button
            className="btn btn-primary"
            onClick={() => {
              this.setState({ hasError: false, error: null })
              window.location.href = '/dashboard'
            }}
          >
            Return to Dashboard
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
