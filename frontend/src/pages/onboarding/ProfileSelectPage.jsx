import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { HeartHandshake, UserRound, ChevronRight } from 'lucide-react'
import { markOnboardingSeen } from '../../utils/onboarding'

const PROFILES = [
  {
    role: 'CUIDADOR',
    title: 'Sou Cuidador',
    description: 'Acompanhe seus pacientes, cadastre medicamentos e receba alertas de atraso.',
    icon: HeartHandshake,
  },
  {
    role: 'PACIENTE',
    title: 'Sou Paciente',
    description: 'Veja seus medicamentos de hoje e registre a abertura da gaveta.',
    icon: UserRound,
  },
]

export default function ProfileSelectPage() {
  const navigate = useNavigate()

  function handleSelect(role) {
    markOnboardingSeen()
    navigate('/login', { state: { profile: role } })
  }

  return (
    <div
      className="container"
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        maxWidth: 520,
      }}
    >
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
        style={{ textAlign: 'center', marginBottom: '2rem' }}
      >
        <h1 style={{ marginBottom: '0.5rem' }}>Você é Cuidador ou Paciente?</h1>
        <p style={{ color: 'var(--color-text-muted)' }}>
          Escolha seu perfil para continuar com o login certo.
        </p>
      </motion.div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {PROFILES.map(({ role, title, description, icon: Icon }, index) => (
          <motion.button
            key={role}
            type="button"
            onClick={() => handleSelect(role)}
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.45, delay: 0.1 + index * 0.1, ease: [0.16, 1, 0.3, 1] }}
            whileTap={{ scale: 0.98 }}
            className="card card-hover"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '1.1rem',
              textAlign: 'left',
              border: '2px solid var(--color-border)',
              cursor: 'pointer',
              width: '100%',
              font: 'inherit',
              color: 'inherit',
            }}
          >
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 'var(--radius-sm)',
                background: 'var(--color-primary-light)',
                color: 'var(--color-primary-dark)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <Icon size={28} strokeWidth={2} />
            </div>
            <div style={{ flex: 1 }}>
              <h2 style={{ margin: '0 0 0.25rem' }}>{title}</h2>
              <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: '0.95rem' }}>
                {description}
              </p>
            </div>
            <ChevronRight size={22} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
          </motion.button>
        ))}
      </div>
    </div>
  )
}
