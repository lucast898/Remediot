import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi, patientsApi } from '../../services/api'
import { wazeUrl } from '../../utils/waze'

export default function PatientListPage() {
  const [patients, setPatients] = useState([])
  const [adherenceByPatient, setAdherenceByPatient] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError('')
      try {
        const [patientsData, adherenceData] = await Promise.all([
          patientsApi.list(),
          dashboardApi.get(),
        ])
        if (cancelled) return
        setPatients(patientsData)
        setAdherenceByPatient(
          Object.fromEntries(adherenceData.map((item) => [item.patientId, item]))
        )
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="container-wide">
      <div className="page-header">
        <h1>Minha carteira de pacientes</h1>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to="dashboard" className="btn btn-secondary">
            Dashboard de assertividade
          </Link>
          <Link to="pacientes/novo" className="btn">
            + Adicionar paciente
          </Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {loading && <p>Carregando...</p>}

      {!loading && patients.length === 0 && !error && (
        <p>Nenhum paciente cadastrado ainda. Clique em "Adicionar paciente" para começar.</p>
      )}

      <div className="grid-cards">
        {patients.map((patient) => {
          const adherence = adherenceByPatient[patient.id]
          const waze = wazeUrl(patient.latitude, patient.longitude)
          const highlight = adherence?.weightThreeLateAlert

          return (
            <div key={patient.id} className={`card patient-card ${highlight ? 'alert-weight-3' : ''}`}>
              <img
                className="patient-card-photo"
                src={patient.photoUrl || '/favicon.svg'}
                alt=""
                aria-hidden="true"
              />
              <div style={{ flex: 1 }}>
                <Link to={`pacientes/${patient.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                  <h2 style={{ margin: '0 0 0.25rem' }}>{patient.fullName}</h2>
                </Link>
                <p style={{ margin: '0 0 0.25rem', color: 'var(--color-text-muted)' }}>
                  {patient.age ? `${patient.age} anos` : 'Idade não informada'}
                </p>
                <p style={{ margin: '0 0 0.5rem', color: 'var(--color-text-muted)' }}>
                  {patient.address || 'Endereço não informado'}
                </p>

                {adherence && (
                  <p style={{ margin: '0 0 0.5rem', fontWeight: 700 }}>
                    Adesão hoje: {adherence.adherencePercentage}%
                    {highlight && ' · ⚠ remédio de peso 3 atrasado'}
                  </p>
                )}

                {waze && (
                  <a href={waze} target="_blank" rel="noreferrer" className="btn btn-secondary">
                    Traçar rota
                  </a>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
