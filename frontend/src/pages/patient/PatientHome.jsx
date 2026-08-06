import Layout from '../../components/Layout'
import { useAuth } from '../../context/AuthContext'

export default function PatientHome() {
  const { auth } = useAuth()

  return (
    <Layout>
      <div className="container-wide">
        <h1>Minha agenda</h1>
        <p>
          Bem-vindo(a), {auth.fullName}. A agenda do dia e o botão de ligar para o cuidador
          chegam nas próximas fases.
        </p>
      </div>
    </Layout>
  )
}
