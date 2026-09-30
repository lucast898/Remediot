import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, MapPin, Pencil, Pill, Plus } from 'lucide-react'
import DoseStatusBadge from '../../components/DoseStatusBadge'
import WeightBadge from '../../components/WeightBadge'
import { dosesApi, medicationsApi, patientsApi } from '../../services/api'
import { wazeUrl } from '../../utils/waze'

function formatTime(dateTimeString) {
  return new Date(dateTimeString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

function BackToPortfolioLink() {
  return (
    <Link
      to="/cuidador"
      className="btn-ghost btn"
      style={{ padding: '0.4rem 0.6rem', marginBottom: '1rem', display: 'inline-flex' }}
    >
      <ArrowLeft size={18} /> Voltar para a carteira
    </Link>
  )
}

export default function PatientDetailPage() {
  const { patientId } = useParams()

  const [patient, setPatient] = useState(null)
  const [medications, setMedications] = useState([])
  const [doses, setDoses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError('')
      try {
        const [patientData, medicationsData, dosesData] = await Promise.all([
          patientsApi.get(patientId),
          medicationsApi.list(patientId),
          dosesApi.listForDate(patientId),
        ])
        if (cancelled) return
        setPatient(patientData)
        setMedications(medicationsData)
        setDoses(dosesData)
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
  }, [patientId])

  if (loading) {
    return (
      <div className="container-wide">
        <BackToPortfolioLink />
        <p>Carregando...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container-wide">
        <BackToPortfolioLink />
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      </div>
    )
  }

  const taken = doses.filter((d) => d.status === 'TOMADA').length
  const late = doses.filter((d) => d.status === 'ATRASADA').length
  const missed = doses.filter((d) => d.status === 'NAO_RETIRADA').length
  const pending = doses.filter((d) => d.status === 'AGENDADA').length
  const total = doses.length || 1

  const waze = wazeUrl(patient.latitude, patient.longitude)

  return (
    <div className="container-wide fade-in">
      <BackToPortfolioLink />
      <div className="page-header">
        <div>
          <h1 style={{ marginBottom: '0.25rem' }}>{patient.fullName}</h1>
          <p style={{ margin: 0, color: 'var(--color-text-muted)' }}>
            {patient.age ? `${patient.age} anos` : ''} {patient.address ? `· ${patient.address}` : ''}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          {waze && (
            <a href={waze} target="_blank" rel="noreferrer" className="btn btn-secondary">
              <MapPin size={16} />
              Traçar rota
            </a>
          )}
          <Link to={`/cuidador/pacientes/${patientId}/editar`} className="btn btn-secondary">
            <Pencil size={16} />
            Editar paciente
          </Link>
        </div>
      </div>

      <section className="card" style={{ marginBottom: '1.5rem' }}>
        <h2>Resumo de hoje</h2>
        <div
          role="img"
          aria-label={`${taken} registrados no horário, ${late} atrasados, ${missed} não retirados, ${pending} pendentes`}
          style={{ display: 'flex', height: '24px', borderRadius: '999px', overflow: 'hidden', margin: '1rem 0' }}
        >
          <div style={{ width: `${(taken / total) * 100}%`, background: 'var(--color-weight-1)' }} />
          <div style={{ width: `${(late / total) * 100}%`, background: 'var(--color-weight-2)' }} />
          <div style={{ width: `${(missed / total) * 100}%`, background: 'var(--color-weight-3)' }} />
          <div style={{ width: `${(pending / total) * 100}%`, background: '#cbd5e1' }} />
        </div>
        <p>
          <strong>{taken}</strong> registrados no horário · <strong>{late}</strong> com atraso ·{' '}
          <strong>{missed}</strong> não retirados · <strong>{pending}</strong> pendentes
        </p>
      </section>

      <section className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="page-header">
          <h2 style={{ margin: 0 }}>Histórico de hoje</h2>
        </div>
        {doses.length === 0 && <p>Nenhum medicamento agendado para hoje.</p>}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {doses.map((dose) => (
            <li
              key={dose.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.5rem',
                padding: '0.75rem 0',
                borderBottom: '1px solid var(--color-border)',
              }}
            >
              <span>
                <strong>{formatTime(dose.scheduledAt)}</strong> — {dose.medicationName} ({dose.medicationDose})
              </span>
              <span style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <WeightBadge weight={dose.medicationWeight} />
                <DoseStatusBadge status={dose.status} />
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <div className="page-header">
          <h2 style={{ margin: 0 }}>Medicamentos</h2>
          <Link to={`/cuidador/pacientes/${patientId}/medicamentos/novo`} className="btn">
            <Plus size={18} />
            Adicionar medicamento
          </Link>
        </div>
        {medications.length === 0 && <p>Nenhum medicamento cadastrado ainda.</p>}
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {medications.map((medication) => (
            <li
              key={medication.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.5rem',
                padding: '0.75rem 0',
                borderBottom: '1px solid var(--color-border)',
              }}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Pill size={16} style={{ color: 'var(--color-primary)', flexShrink: 0 }} />
                <strong>{medication.name}</strong> — {medication.dose} · horários:{' '}
                {medication.times.map((time) => time.slice(0, 5)).join(', ')}
              </span>
              <span style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <WeightBadge weight={medication.weight} />
                <Link
                  to={`/cuidador/pacientes/${patientId}/medicamentos/${medication.id}/editar`}
                  className="btn btn-secondary"
                >
                  <Pencil size={16} />
                  Editar
                </Link>
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
