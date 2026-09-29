import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowLeft, HeartPulse, Lock, LogIn, Mail } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const PROFILE_COPY = {
  CUIDADOR: {
    title: 'Entrar como Cuidador',
    subtitle: 'Acesse com o e-mail e senha da sua conta de cuidador.',
  },
  PACIENTE: {
    title: 'Entrar como Paciente',
    subtitle: 'Acesse com o e-mail e senha que seu cuidador cadastrou.',
  },
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const profile = location.state?.profile
  const copy = PROFILE_COPY[profile] || {
    title: 'Entrar',
    subtitle: 'Acesse com o e-mail e senha do cuidador ou do paciente.',
  }

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      const redirectTo = location.state?.from?.pathname || '/'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
        <Link
          to="/perfil"
          className="btn-ghost btn"
          style={{ padding: '0.4rem 0.6rem', marginBottom: '1rem', display: 'inline-flex' }}
        >
          <ArrowLeft size={18} /> Trocar perfil
        </Link>

        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 'var(--radius-sm)',
            background: 'var(--color-primary-light)',
            color: 'var(--color-primary-dark)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1.25rem',
          }}
        >
          <HeartPulse size={28} />
        </div>

        <h1>{copy.title}</h1>
        <p style={{ color: 'var(--color-text-muted)', marginBottom: '1.75rem' }}>{copy.subtitle}</p>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="email">
              <Mail size={15} style={{ verticalAlign: '-2px', marginRight: '0.3rem' }} />
              E-mail
            </label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="field">
            <label htmlFor="password">
              <Lock size={15} style={{ verticalAlign: '-2px', marginRight: '0.3rem' }} />
              Senha
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-block" disabled={loading}>
            <LogIn size={18} />
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        {profile !== 'PACIENTE' && (
          <p style={{ marginTop: '1.5rem' }}>
            É cuidador e ainda não tem conta? <Link to="/cadastro">Cadastre-se</Link>
          </p>
        )}
      </motion.div>
    </div>
  )
}
