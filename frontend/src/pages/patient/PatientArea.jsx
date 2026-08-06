import { Route, Routes, useNavigate } from 'react-router-dom'
import Layout from '../../components/Layout'
import { useAuth } from '../../context/AuthContext'
import MedicationFormPage from '../shared/MedicationFormPage'
import PatientAgendaPage from './PatientAgendaPage'

function AddMedication() {
  const { auth } = useAuth()
  const navigate = useNavigate()
  return (
    <MedicationFormPage
      mode="create"
      patientId={auth.patientId}
      onSaved={() => navigate('/paciente')}
    />
  )
}

export default function PatientArea() {
  return (
    <Layout>
      <Routes>
        <Route index element={<PatientAgendaPage />} />
        <Route path="medicamentos/novo" element={<AddMedication />} />
      </Routes>
    </Layout>
  )
}
