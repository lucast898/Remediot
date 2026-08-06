import { useNavigate } from 'react-router-dom'
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
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '1rem 1.25rem',
          background: 'var(--color-surface)',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <strong style={{ fontSize: '1.25rem', color: 'var(--color-primary-dark)' }}>RemedIoT</strong>
        {auth && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span>Olá, {auth.fullName}</span>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              Sair
            </button>
          </div>
        )}
      </header>
      <main id="main-content" tabIndex={-1}>
        {children}
      </main>
    </div>
  )
}
