import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setLoading(true)
    try {
      await register(fullName, email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message)
      setFieldErrors(err.fields || {})
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container">
      <h1>Criar conta de cuidador</h1>
      <p>
        O cadastro aqui é para cuidadores. Cada paciente ganha um acesso próprio quando o
        cuidador o adiciona à carteira.
      </p>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="fullName">Nome completo</label>
          <input
            id="fullName"
            type="text"
            autoComplete="name"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
          {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
        </div>

        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </div>

        <div className="field">
          <label htmlFor="password">Senha</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            minLength={6}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </div>

        <button type="submit" className="btn btn-block" disabled={loading}>
          {loading ? 'Criando conta...' : 'Criar conta'}
        </button>
      </form>

      <p style={{ marginTop: '1.5rem' }}>
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </div>
  )
}
