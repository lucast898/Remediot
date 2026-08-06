import { createContext, useCallback, useContext, useState } from 'react'
import { authApi, TOKEN_KEY } from '../services/api'

const AUTH_KEY = 'remediot_auth'
const AuthContext = createContext(null)

function loadStoredAuth() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth)

  const persist = useCallback((data) => {
    localStorage.setItem(AUTH_KEY, JSON.stringify(data))
    localStorage.setItem(TOKEN_KEY, data.token)
    setAuth(data)
  }, [])

  const login = useCallback(
    async (email, password) => {
      const data = await authApi.login(email, password)
      persist(data)
      return data
    },
    [persist]
  )

  const register = useCallback(
    async (fullName, email, password, phone) => {
      const data = await authApi.register(fullName, email, password, phone)
      persist(data)
      return data
    },
    [persist]
  )

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_KEY)
    localStorage.removeItem(TOKEN_KEY)
    setAuth(null)
  }, [])

  const value = {
    auth,
    isAuthenticated: Boolean(auth),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return ctx
}
