import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}

export function RequireRole({ role, children }) {
  const { auth } = useAuth()

  if (auth?.role !== role) {
    return <Navigate to="/" replace />
  }

  return children
}
