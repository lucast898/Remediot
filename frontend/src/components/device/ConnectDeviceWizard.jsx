import { useEffect, useRef, useState } from 'react'
import { CheckCircle2, Link2, Loader2, X } from 'lucide-react'
import { deviceApi } from '../../services/api'

// A mesma PORTAL_SENHA do firmware. Só serve para entrar na rede da própria gaveta, de perto.
const PORTAL_PASSWORD = import.meta.env.VITE_GAVETA_SENHA_PORTAL

const POLL_INTERVAL_MS = 3000
const POLL_LIMIT_MS = 120000

/**
 * Assistente "Conectar gaveta" (D7, D8): o Wi-Fi da casa é configurado na página
 * da própria gaveta; o app só vincula pelos 4 caracteres do nome da rede.
 */
export default function ConnectDeviceWizard({ patientId, onClose, onPaired }) {
  const [code, setCode] = useState('')
  const [pairing, setPairing] = useState(false)
  const [error, setError] = useState('')
  const [progress, setProgress] = useState(null)
  const cancelled = useRef(false)

  useEffect(() => {
    cancelled.current = false
    return () => {
      cancelled.current = true
    }
  }, [])

  async function waitUntilSynced(start) {
    while (!cancelled.current && Date.now() - start < POLL_LIMIT_MS) {
      const device = await deviceApi.get(patientId)
      if (device.online && device.agendaStatus === 'SINCRONIZADA') {
        setProgress('done')
        onPaired(device)
        return
      }
      setProgress(device.online ? 'agenda' : 'online')
      await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS))
    }
    if (!cancelled.current) setProgress('slow')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setPairing(true)
    try {
      await deviceApi.pair(patientId, code)
      setProgress('online')
      await waitUntilSynced(Date.now())
    } catch (err) {
      setError(err.message)
      setProgress(null)
    } finally {
      setPairing(false)
    }
  }

  return (
    <section className="card wizard" aria-labelledby="wizard-title">
      <div className="page-header">
        <h3 id="wizard-title" style={{ margin: 0 }}>
          Conectar gaveta
        </h3>
        <button type="button" className="btn btn-ghost" onClick={onClose}>
          <X size={18} /> Fechar
        </button>
      </div>

      <ol className="wizard-steps">
        <li>Ligue a gaveta na tomada. A luz pisca em branco.</li>
        <li>
          No celular, abra o Wi-Fi e conecte na rede <strong>RemedIoT-XXXX</strong>
          {PORTAL_PASSWORD ? (
            <>
              {' '}
              com a senha <strong className="code-text">{PORTAL_PASSWORD}</strong>.
            </>
          ) : (
            ' com a senha que veio com a gaveta.'
          )}
        </li>
        <li>
          A página da gaveta abre sozinha (se não abrir, acesse <strong>192.168.4.1</strong> no navegador). Escolha o
          Wi-Fi da casa e digite a senha dele. <em>A senha do Wi-Fi é digitada na página da gaveta, não neste app.</em>
        </li>
        <li>Volte para a internet normal e digite abaixo os 4 caracteres do fim do nome da rede (ex.: RemedIoT-C4D5 → C4D5).</li>
      </ol>

      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="device-code">Código da gaveta (4 caracteres)</label>
          <input
            id="device-code"
            className="code-input"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase().replace(/[^0-9A-F]/g, '').slice(0, 4))}
            maxLength={4}
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            aria-describedby="device-code-help"
            disabled={pairing}
          />
          <small id="device-code-help">Só letras de A a F e números.</small>
        </div>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        <button type="submit" className="btn btn-block" disabled={pairing || code.length !== 4}>
          {pairing ? <Loader2 size={18} className="spin" /> : <Link2 size={18} />}
          {pairing ? 'Conectando...' : 'Conectar'}
        </button>
      </form>

      {progress && (
        <div className={`alert ${progress === 'done' ? 'alert-success' : 'alert-info'}`} role="status" aria-live="polite">
          {progress === 'done' && <CheckCircle2 size={20} />}
          {progress === 'online' && 'Gaveta vinculada. Aguardando ela ficar online...'}
          {progress === 'agenda' && 'Gaveta online. Enviando a agenda de remédios...'}
          {progress === 'done' && 'Tudo pronto: gaveta conectada e agenda em dia.'}
          {progress === 'slow' &&
            'A gaveta foi vinculada, mas ainda não confirmou a agenda. Confira se ela está ligada e com Wi-Fi; o estado aparece no card da gaveta.'}
        </div>
      )}
    </section>
  )
}
