import { createContext, useContext, useState, useCallback } from 'react'
import * as authApi from '@/api/auth'

const AuthContext = createContext(null)

function loadSession() {
  try {
    const token = sessionStorage.getItem('token')
    const user = JSON.parse(sessionStorage.getItem('user') ?? 'null')
    return { token, user }
  } catch {
    return { token: null, user: null }
  }
}

export function AuthProvider({ children }) {
  const [{ token, user }, setSession] = useState(loadSession)

  const saveSession = useCallback((authData) => {
    sessionStorage.setItem('token', authData.token)
    sessionStorage.setItem('user', JSON.stringify({
      id: authData.userId,
      email: authData.email,
      role: authData.role,
    }))
    setSession({ token: authData.token, user: { id: authData.userId, email: authData.email, role: authData.role } })
  }, [])

  const login = useCallback(async (credentials) => {
    const data = await authApi.login(credentials)
    saveSession(data)
    return data
  }, [saveSession])

  const register = useCallback(async (payload) => {
    const data = await authApi.register(payload)
    saveSession(data)
    return data
  }, [saveSession])

  const logout = useCallback(() => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    setSession({ token: null, user: null })
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
