import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { medicationsApi } from '../../services/api'

export default function MedicationFormPage({ mode }) {
  const isEdit = mode === 'edit'
  const { patientId, medicationId } = useParams()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [dose, setDose] = useState('')
  const [weight, setWeight] = useState('1')
  const [times, setTimes] = useState([''])
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEdit) return
    let cancelled = false

    medicationsApi
      .list(patientId)
      .then((list) => {
        if (cancelled) return
        const medication = list.find((m) => String(m.id) === medicationId)
        if (!medication) {
          setError('Medicamento não encontrado')
          return
        }
        setName(medication.name)
        setDose(medication.dose)
        setWeight(String(medication.weight))
        setTimes(medication.times.length > 0 ? medication.times : [''])
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false))

    return () => {
      cancelled = true
    }
  }, [isEdit, patientId, medicationId])

  function updateTime(index, value) {
    setTimes((prev) => prev.map((t, i) => (i === index ? value : t)))
  }

  function addTime() {
    setTimes((prev) => [...prev, ''])
  }

  function removeTime(index) {
    setTimes((prev) => prev.filter((_, i) => i !== index))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)

    const payload = {
      name,
      dose,
      weight: Number(weight),
      times: times.filter(Boolean),
    }

    try {
      if (isEdit) {
        await medicationsApi.update(patientId, medicationId, payload)
      } else {
        await medicationsApi.create(patientId, payload)
      }
      navigate(`/cuidador/pacientes/${patientId}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
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
    <div className="container">
      <h1>{isEdit ? 'Editar medicamento' : 'Adicionar medicamento'}</h1>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="name">Nome do medicamento</label>
          <input id="name" required value={name} onChange={(e) => setName(e.target.value)} />
        </div>

        <div className="field">
          <label htmlFor="dose">Dose (ex.: 500mg, 1 comprimido)</label>
          <input id="dose" required value={dose} onChange={(e) => setDose(e.target.value)} />
        </div>

        <div className="field">
          <label htmlFor="weight">Peso (urgência do alerta)</label>
          <select id="weight" value={weight} onChange={(e) => setWeight(e.target.value)}>
            <option value="1">Peso 1 — tranquilo</option>
            <option value="2">Peso 2 — sério</option>
            <option value="3">Peso 3 — não pode atrasar</option>
          </select>
        </div>

        <div className="field">
          <label>Horários</label>
          {times.map((time, index) => (
            <div key={index} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <input
                type="time"
                required
                value={time}
                onChange={(e) => updateTime(index, e.target.value)}
              />
              {times.length > 1 && (
                <button type="button" className="btn btn-secondary" onClick={() => removeTime(index)}>
                  Remover
                </button>
              )}
            </div>
          ))}
          <button type="button" className="btn btn-secondary" onClick={addTime}>
            + Adicionar horário
          </button>
        </div>

        <button type="submit" className="btn btn-block" disabled={saving} style={{ marginTop: '1rem' }}>
          {saving ? 'Salvando...' : 'Salvar'}
        </button>
      </form>
    </div>
  )
}
