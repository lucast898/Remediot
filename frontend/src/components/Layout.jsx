import { useNavigate } from 'react-router-dom'
import { HeartPulse, LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function Layout({ children }) {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div>
      <a href="#main-content" className="skip-link">
        Pular para o conteúdo
      </a>
      <header className="app-header">
        <span className="app-header-brand">
          <HeartPulse size={22} strokeWidth={2.3} />
          RemedIoT
        </span>
        {auth && (
          <div className="app-header-user">
            <span>Olá, {auth.fullName?.split(' ')[0] || auth.fullName}</span>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              <LogOut size={16} />
              Sair
            </button>
          </div>
        )}
      </header>
      <main id="main-content" tabIndex={-1} className="fade-in">
        {children}
      </main>
    </div>
  )
}
