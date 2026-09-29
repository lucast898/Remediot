import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { HeartPulse } from 'lucide-react'
import { markOnboardingSeen } from '../../utils/onboarding'

export default function WelcomePage() {
  const navigate = useNavigate()

  function handleContinue() {
    markOnboardingSeen()
    navigate('/perfil')
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '2rem 1.5rem',
        background: 'linear-gradient(160deg, #0f766e 0%, #115e59 55%, #0f1a1a 130%)',
        color: '#ffffff',
      }}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.7 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        style={{
          width: 88,
          height: 88,
          borderRadius: 24,
          background: 'rgba(255,255,255,0.15)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '1.75rem',
          backdropFilter: 'blur(4px)',
        }}
      >
        <HeartPulse size={44} strokeWidth={2} />
      </motion.div>

      <motion.h1
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.15, ease: [0.16, 1, 0.3, 1] }}
        style={{ fontSize: '2rem', margin: '0 0 0.75rem', color: '#ffffff' }}
      >
        Bem-vindo ao RemedIoT
      </motion.h1>

      <motion.p
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
        style={{ maxWidth: 360, color: 'rgba(255,255,255,0.92)', fontSize: '1.05rem', margin: '0 0 2.5rem' }}
      >
        Cuidado com a rotina de medicamentos, acompanhado de perto por quem você confia.
      </motion.p>

      <motion.button
        type="button"
        onClick={handleContinue}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.45, ease: [0.16, 1, 0.3, 1] }}
        whileTap={{ scale: 0.96 }}
        className="btn"
        style={{
          background: '#ffffff',
          color: 'var(--color-primary-dark)',
          minWidth: 220,
          fontSize: '1.05rem',
        }}
      >
        Começar
      </motion.button>
    </div>
  )
}
