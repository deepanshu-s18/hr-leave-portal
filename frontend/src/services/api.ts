import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor — handle 401 (token expired) → refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken })
          const { accessToken } = res.data
          localStorage.setItem('accessToken', accessToken)
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

// ── Auth ────────────────────────────────────────────────────────────────────
export const authApi = {
  login: (usernameOrEmail: string, password: string) =>
    api.post('/api/v1/auth/login', { usernameOrEmail, password }),
  register: (data: { username: string; email: string; password: string; fullName: string }) =>
    api.post('/api/v1/auth/register', data),
}

// ── Leave Requests ───────────────────────────────────────────────────────────
export const leaveApi = {
  create: (data: {
    leaveType: string
    startDate: string
    endDate: string
    reason: string
  }) => api.post('/api/v1/leaves', data),

  getMyLeaves: (page = 0, size = 10, status?: string) =>
    api.get('/api/v1/leaves/my', { params: { page, size, status } }),

  getPendingForManager: (page = 0, size = 10) =>
    api.get('/api/v1/leaves/pending', { params: { page, size } }),

  approve: (id: number, comment?: string) =>
    api.patch(`/api/v1/leaves/${id}/approve`, { comment }),

  reject: (id: number, comment: string) =>
    api.patch(`/api/v1/leaves/${id}/reject`, { comment }),

  cancel: (id: number) =>
    api.patch(`/api/v1/leaves/${id}/cancel`),

  getAll: (page = 0, size = 20, status?: string, employeeId?: number) =>
    api.get('/api/v1/leaves', { params: { page, size, status, employeeId } }),
}

// ── Employees ────────────────────────────────────────────────────────────────
export const employeeApi = {
  getProfile: () => api.get('/api/v1/employees/me'),
  getAll: (page = 0, size = 20) => api.get('/api/v1/employees', { params: { page, size } }),
  getById: (id: number) => api.get(`/api/v1/employees/${id}`),
}
