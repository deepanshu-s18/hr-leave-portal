import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authApi } from '../services/api'

interface User {
  id: number
  username: string
  email: string
  fullName: string
  role: 'EMPLOYEE' | 'MANAGER' | 'ADMIN' | 'HR'
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (usernameOrEmail: string, password: string) => Promise<void>
  logout: () => void
  isManager: boolean
  isAdmin: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem('user')
    if (stored) {
      try { setUser(JSON.parse(stored)) } catch { localStorage.clear() }
    }
    setIsLoading(false)
  }, [])

  const login = async (usernameOrEmail: string, password: string) => {
    const res = await authApi.login(usernameOrEmail, password)
    const { accessToken, refreshToken, user: userData } = res.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
  }

  const logout = () => {
    localStorage.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{
      user,
      isLoading,
      login,
      logout,
      isManager: user?.role === 'MANAGER' || user?.role === 'ADMIN' || user?.role === 'HR',
      isAdmin: user?.role === 'ADMIN',
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
