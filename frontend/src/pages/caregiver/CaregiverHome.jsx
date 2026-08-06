import Layout from '../../components/Layout'
import { useAuth } from '../../context/AuthContext'

export default function CaregiverHome() {
  const { auth } = useAuth()

  return (
    <Layout>
      <div className="container-wide">
        <h1>Área do cuidador</h1>
        <p>
          Bem-vindo(a), {auth.fullName}. A carteira de pacientes e o dashboard de assertividade
          chegam nas próximas fases.
        </p>
      </div>
    </Layout>
  )
}
