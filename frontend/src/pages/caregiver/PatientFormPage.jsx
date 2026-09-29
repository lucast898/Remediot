import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Save } from 'lucide-react'
import { patientsApi } from '../../services/api'

const EMPTY_FORM = {
  fullName: '',
  email: '',
  password: '',
  age: '',
  address: '',
  latitude: '',
  longitude: '',
  photoUrl: '',
}

export default function PatientFormPage({ mode }) {
  const isEdit = mode === 'edit'
  const { patientId } = useParams()
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})

  useEffect(() => {
    if (!isEdit) return
    let cancelled = false

    patientsApi
      .get(patientId)
      .then((data) => {
        if (cancelled) return
        setForm({
          fullName: data.fullName,
          email: data.email,
          password: '',
          age: data.age ?? '',
          address: data.address ?? '',
          latitude: data.latitude ?? '',
          longitude: data.longitude ?? '',
          photoUrl: data.photoUrl ?? '',
        })
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false))

    return () => {
      cancelled = true
    }
  }, [isEdit, patientId])

  function handleChange(field) {
    return (event) => setForm((prev) => ({ ...prev, [field]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setSaving(true)

    const payload = {
      age: form.age === '' ? null : Number(form.age),
      address: form.address || null,
      latitude: form.latitude === '' ? null : Number(form.latitude),
      longitude: form.longitude === '' ? null : Number(form.longitude),
      photoUrl: form.photoUrl || null,
    }

    try {
      if (isEdit) {
        await patientsApi.update(patientId, payload)
        navigate(`/cuidador/pacientes/${patientId}`)
      } else {
        const created = await patientsApi.create({
          ...payload,
          fullName: form.fullName,
          email: form.email,
          password: form.password,
        })
        navigate(`/cuidador/pacientes/${created.id}`)
      }
    } catch (err) {
      setError(err.message)
      setFieldErrors(err.fields || {})
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
    <div className="container fade-in">
      <h1>{isEdit ? 'Editar paciente' : 'Adicionar paciente'}</h1>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {!isEdit && (
          <>
            <div className="field">
              <label htmlFor="fullName">Nome completo</label>
              <input
                id="fullName"
                required
                value={form.fullName}
                onChange={handleChange('fullName')}
              />
              {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
            </div>

            <div className="field">
              <label htmlFor="email">E-mail (login do paciente)</label>
              <input
                id="email"
                type="email"
                required
                value={form.email}
                onChange={handleChange('email')}
              />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </div>

            <div className="field">
              <label htmlFor="password">Senha inicial do paciente</label>
              <input
                id="password"
                type="password"
                minLength={6}
                required
                value={form.password}
                onChange={handleChange('password')}
              />
              {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
            </div>
          </>
        )}

        <div className="field">
          <label htmlFor="age">Idade</label>
          <input id="age" type="number" min="0" value={form.age} onChange={handleChange('age')} />
        </div>

        <div className="field">
          <label htmlFor="address">Endereço</label>
          <input id="address" value={form.address} onChange={handleChange('address')} />
        </div>

        <div className="field">
          <label htmlFor="latitude">Latitude (para o Waze)</label>
          <input
            id="latitude"
            type="number"
            step="any"
            value={form.latitude}
            onChange={handleChange('latitude')}
          />
        </div>

        <div className="field">
          <label htmlFor="longitude">Longitude (para o Waze)</label>
          <input
            id="longitude"
            type="number"
            step="any"
            value={form.longitude}
            onChange={handleChange('longitude')}
          />
        </div>

        <div className="field">
          <label htmlFor="photoUrl">URL da foto (opcional)</label>
          <input id="photoUrl" value={form.photoUrl} onChange={handleChange('photoUrl')} />
        </div>

        <button type="submit" className="btn btn-block" disabled={saving}>
          <Save size={18} />
          {saving ? 'Salvando...' : 'Salvar'}
        </button>
      </form>
    </div>
  )
}
