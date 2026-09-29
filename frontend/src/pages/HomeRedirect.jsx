import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { hasSeenOnboarding } from '../utils/onboarding'

export default function HomeRedirect() {
  const { auth } = useAuth()

  if (auth?.role === 'CUIDADOR') {
    return <Navigate to="/cuidador" replace />
  }
  if (auth?.role === 'PACIENTE') {
    return <Navigate to="/paciente" replace />
  }
  if (!hasSeenOnboarding()) {
    return <Navigate to="/bem-vindo" replace />
  }
  return <Navigate to="/login" replace />
}
