import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ArrowLeft, TrendingUp, Users, XCircle } from 'lucide-react'
import { dashboardApi } from '../../services/api'

function categorize(patient) {
  if (patient.missed > 0) return 'Não está tomando'
  if (patient.takenLate > 0) return 'Com atraso'
  if (patient.takenOnTime > 0) return 'No horário'
  return 'Pendente'
}

function CategoryList({ title, patients }) {
  return (
    <div className="card">
      <h2>
        {title} ({patients.length})
      </h2>
      {patients.length === 0 && (
        <p style={{ color: 'var(--color-text-muted)' }}>Ninguém nesta categoria.</p>
      )}
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {patients.map((patient) => (
          <li
            key={patient.patientId}
            style={{
              padding: '0.5rem 0',
              borderBottom: '1px solid var(--color-border)',
              color: patient.weightThreeLateAlert ? 'var(--color-weight-3)' : 'inherit',
              fontWeight: patient.weightThreeLateAlert ? 700 : 400,
            }}
          >
            {patient.fullName} — {patient.adherencePercentage}%
            {patient.weightThreeLateAlert && ' · remédio de peso 3 atrasado'}
          </li>
        ))}
      </ul>
    </div>
  )
}

export default function DashboardPage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .get()
      .then((res) => !cancelled && setData(res))
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div className="container-wide">
        <p>Carregando...</p>
      </div>
    )
  }

  const chartData = data.map((patient) => ({
    name: patient.fullName,
    'No horário': patient.takenOnTime,
    'Com atraso': patient.takenLate,
    'Não retirado': patient.missed,
  }))

  const noHorario = data.filter((p) => categorize(p) === 'No horário')
  const comAtraso = data.filter((p) => categorize(p) === 'Com atraso')
  const naoTomando = data.filter((p) => categorize(p) === 'Não está tomando')
  const pendente = data.filter((p) => categorize(p) === 'Pendente')

  const mediaAdesao = data.length
    ? Math.round((data.reduce((sum, p) => sum + p.adherencePercentage, 0) / data.length) * 10) / 10
    : 0

  return (
    <div className="container-wide fade-in">
      <div className="page-header">
        <h1>Dashboard de assertividade</h1>
        <Link to="/cuidador" className="btn btn-secondary">
          <ArrowLeft size={18} />
          Carteira de pacientes
        </Link>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {data.length === 0 && !error && <p>Nenhum paciente cadastrado ainda.</p>}

      {data.length > 0 && (
        <>
          <div
            className="stagger"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              marginBottom: '2rem',
            }}
          >
            <div className="card stat-tile">
              <div className="stat-tile-icon">
                <Users size={22} />
              </div>
              <div>
                <div className="stat-tile-value">{data.length}</div>
                <div className="stat-tile-label">pacientes</div>
              </div>
            </div>
            <div className="card stat-tile">
              <div className="stat-tile-icon">
                <TrendingUp size={22} />
              </div>
              <div>
                <div className="stat-tile-value">{mediaAdesao}%</div>
                <div className="stat-tile-label">adesão média hoje</div>
              </div>
            </div>
            <div className="card stat-tile">
              <div
                className="stat-tile-icon"
                style={{ background: 'var(--color-weight-3-bg)', color: 'var(--color-weight-3)' }}
              >
                <XCircle size={22} />
              </div>
              <div>
                <div className="stat-tile-value" style={{ color: 'var(--color-weight-3)' }}>
                  {naoTomando.length}
                </div>
                <div className="stat-tile-label">não estão tomando</div>
              </div>
            </div>
          </div>

          <div className="card" style={{ marginBottom: '2rem', height: 320 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Legend />
                <Bar dataKey="No horário" stackId="doses" fill="#15803d" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Com atraso" stackId="doses" fill="#b45309" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Não retirado" stackId="doses" fill="#b91c1c" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="grid-cards stagger">
            <CategoryList title="Não está tomando" patients={naoTomando} />
            <CategoryList title="Com atraso" patients={comAtraso} />
            <CategoryList title="No horário" patients={noHorario} />
            <CategoryList title="Pendente" patients={pendente} />
          </div>
        </>
      )}
    </div>
  )
}
