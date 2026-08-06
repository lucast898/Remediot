import { Route, Routes } from 'react-router-dom'
import Layout from '../../components/Layout'
import PatientListPage from './PatientListPage'
import PatientFormPage from './PatientFormPage'
import PatientDetailPage from './PatientDetailPage'
import MedicationFormPage from './MedicationFormPage'

export default function CaregiverArea() {
  return (
    <Layout>
      <Routes>
        <Route index element={<PatientListPage />} />
        <Route path="pacientes/novo" element={<PatientFormPage mode="create" />} />
        <Route path="pacientes/:patientId" element={<PatientDetailPage />} />
        <Route path="pacientes/:patientId/editar" element={<PatientFormPage mode="edit" />} />
        <Route path="pacientes/:patientId/medicamentos/novo" element={<MedicationFormPage mode="create" />} />
        <Route
          path="pacientes/:patientId/medicamentos/:medicationId/editar"
          element={<MedicationFormPage mode="edit" />}
        />
      </Routes>
    </Layout>
  )
}
