import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Clock, PackageOpen, Phone, Plus } from 'lucide-react'
import DoseStatusBadge from '../../components/DoseStatusBadge'
import DeviceCard from '../../components/device/DeviceCard'
import WeightBadge from '../../components/WeightBadge'
import { useAuth } from '../../context/AuthContext'
import { dosesApi, patientsApi } from '../../services/api'

function formatTime(dateTimeString) {
  return new Date(dateTimeString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

export default function PatientAgendaPage() {
  const { auth } = useAuth()
  const [patient, setPatient] = useState(null)
  const [doses, setDoses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [simulatingId, setSimulatingId] = useState(null)
  const [justRegisteredId, setJustRegisteredId] = useState(null)
  // D11: com gaveta vinculada, quem registra a abertura é a gaveta; o simulador some
  const [hasDevice, setHasDevice] = useState(null)
  const handleDeviceChange = useCallback((device) => setHasDevice(device.paired), [])

  const load = useCallback(async () => {
    setError('')
    try {
      const [patientData, dosesData] = await Promise.all([
        patientsApi.get(auth.patientId),
        dosesApi.listForDate(auth.patientId),
      ])
      setPatient(patientData)
      setDoses(dosesData)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [auth.patientId])

  useEffect(() => {
    load()
  }, [load])

  async function handleSimulate(doseId) {
    setSimulatingId(doseId)
    setError('')
    try {
      await dosesApi.simulateOpening(doseId)
      await load()
      setJustRegisteredId(doseId)
      setTimeout(() => setJustRegisteredId(null), 1500)
    } catch (err) {
      setError(err.message)
    } finally {
      setSimulatingId(null)
    }
  }

  if (loading) {
    return (
      <div className="container">
        <p>Carregando...</p>
      </div>
    )
  }

  return (
    <div className="container fade-in">
      <h1>Minha agenda de hoje</h1>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {patient?.caregiverPhone ? (
        <a href={`tel:${patient.caregiverPhone}`} className="btn btn-call btn-block">
          <Phone size={20} />
          Ligar para {patient.caregiverFullName || 'o cuidador'}
        </a>
      ) : (
        <p className="card">Nenhum telefone de cuidador cadastrado ainda.</p>
      )}

      <div style={{ marginTop: '2rem' }}>
        <DeviceCard patientId={auth.patientId} isCaregiver={false} onDeviceChange={handleDeviceChange} />
      </div>

      <section style={{ marginTop: '2rem' }}>
        <div className="page-header">
          <h2 style={{ margin: 0 }}>Medicamentos de hoje</h2>
          <Link to="medicamentos/novo" className="btn btn-secondary">
            <Plus size={18} />
            Adicionar medicamento
          </Link>
        </div>

        {doses.length === 0 && <p>Nenhum medicamento agendado para hoje.</p>}

        <ul className="stagger" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {doses.map((dose) => (
            <li
              key={dose.id}
              className="card"
              style={{
                marginBottom: '1rem',
                boxShadow: justRegisteredId === dose.id ? '0 0 0 3px var(--color-primary-light)' : undefined,
                transition: 'box-shadow 0.4s ease',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                }}
              >
                <div>
                  <strong style={{ fontSize: '1.2rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Clock size={18} style={{ color: 'var(--color-primary)' }} />
                    {formatTime(dose.scheduledAt)}
                  </strong>{' '}
                  — {dose.medicationName} ({dose.medicationDose})
                  <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    <WeightBadge weight={dose.medicationWeight} />
                    <DoseStatusBadge status={dose.status} />
                  </div>
                </div>
                {hasDevice === false && (
                  <button
                    type="button"
                    className="btn"
                    disabled={simulatingId === dose.id}
                    onClick={() => handleSimulate(dose.id)}
                  >
                    <PackageOpen size={18} />
                    {simulatingId === dose.id ? 'Registrando...' : 'Simular abertura da gaveta'}
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </section>

      <p style={{ color: 'var(--color-text-muted)', marginTop: '1rem' }}>
        Em breve: alarmes sonoros 20, 10 e 1 minuto antes de cada horário.
      </p>
    </div>
  )
}
