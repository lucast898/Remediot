import { Route, Routes } from 'react-router-dom'
import { RequireAuth, RequireRole } from './components/RouteGuards'
import HomeRedirect from './pages/HomeRedirect'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import WelcomePage from './pages/onboarding/WelcomePage'
import ProfileSelectPage from './pages/onboarding/ProfileSelectPage'
import CaregiverArea from './pages/caregiver/CaregiverArea'
import PatientArea from './pages/patient/PatientArea'

function App() {
  return (
    <Routes>
      <Route path="/bem-vindo" element={<WelcomePage />} />
      <Route path="/perfil" element={<ProfileSelectPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<RegisterPage />} />

      <Route path="/" element={<HomeRedirect />} />

      <Route
        path="/cuidador/*"
        element={
          <RequireAuth>
            <RequireRole role="CUIDADOR">
              <CaregiverArea />
            </RequireRole>
          </RequireAuth>
        }
      />

      <Route
        path="/paciente/*"
        element={
          <RequireAuth>
            <RequireRole role="PACIENTE">
              <PatientArea />
            </RequireRole>
          </RequireAuth>
        }
      />
    </Routes>
  )
}

export default App
