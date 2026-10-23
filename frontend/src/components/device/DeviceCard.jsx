import { useCallback, useEffect, useRef, useState } from 'react'
import { BellOff, Box, CheckCircle2, Link2, MapPin, PackagePlus, RefreshCw, Unlink, Wifi, WifiOff } from 'lucide-react'
import { deviceApi } from '../../services/api'
import { agendaText, commandResultText, connectionText, formatDeviceDateTime, waitForCommand } from '../../utils/device'
import ConnectDeviceWizard from './ConnectDeviceWizard'

const REFRESH_MS = 15000

/**
 * Card "Gaveta" na tela do paciente (cuidador e paciente). A gaveta registra a
 * abertura do compartimento; os textos nunca afirmam que o remédio foi tomado.
 */
export default function DeviceCard({ patientId, isCaregiver, onDeviceChange }) {
  const [device, setDevice] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')
  const [busyAction, setBusyAction] = useState(null)
  const [feedback, setFeedback] = useState(null)
  const [showWizard, setShowWizard] = useState(false)
  const [confirming, setConfirming] = useState(null)
  // Em ref para o recarregamento periódico não reiniciar a cada render de quem usa o card
  const onDeviceChangeRef = useRef(onDeviceChange)
  onDeviceChangeRef.current = onDeviceChange

  const load = useCallback(async () => {
    try {
      const data = await deviceApi.get(patientId)
      setDevice(data)
      onDeviceChangeRef.current?.(data)
      if (data.paired) {
        const [alertsData, historyData] = await Promise.all([
          deviceApi.alerts(patientId),
          deviceApi.events(patientId, 20),
        ])
        setAlerts(alertsData)
        setHistory(historyData)
      } else {
        setAlerts([])
        setHistory([])
      }
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }, [patientId])

  useEffect(() => {
    load()
    const timer = setInterval(load, REFRESH_MS)
    return () => clearInterval(timer)
  }, [load])

  async function runCommand(action, send) {
    setBusyAction(action)
    setFeedback(null)
    setConfirming(null)
    try {
      const sent = await send()
      const final = sent ? await waitForCommand(patientId, sent) : null
      setFeedback(final ? commandResultText(action, final) : { text: 'Pronto.', ok: true })
      await load()
    } catch (err) {
      setFeedback({ text: err.message, ok: false })
    } finally {
      setBusyAction(null)
    }
  }

  const command = (action, body) => runCommand(action, () => deviceApi.command(patientId, action, body))

  async function handleUnpair() {
    setBusyAction('unpair')
    setConfirming(null)
    try {
      await deviceApi.unpair(patientId)
      setFeedback({ text: 'Gaveta desvinculada. Ela parou de acompanhar as doses deste paciente.', ok: true })
      await load()
    } catch (err) {
      setFeedback({ text: err.message, ok: false })
    } finally {
      setBusyAction(null)
    }
  }

  if (!device) {
    return (
      <section className="card" aria-labelledby="device-title" style={{ marginBottom: '1.5rem' }}>
        <h2 id="device-title">Gaveta</h2>
        {error ? (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        ) : (
          <p>Carregando...</p>
        )}
      </section>
    )
  }

  if (!device.paired) {
    return (
      <section className="card" aria-labelledby="device-title" style={{ marginBottom: '1.5rem' }}>
        <h2 id="device-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Box size={22} /> Gaveta
        </h2>
        <p>Nenhuma gaveta conectada a este paciente.</p>
        {feedback && <Feedback feedback={feedback} />}
        {isCaregiver ? (
          showWizard ? (
            <ConnectDeviceWizard
              patientId={patientId}
              onClose={() => setShowWizard(false)}
              onPaired={() => {
                setShowWizard(false)
                setFeedback({ text: 'Gaveta conectada e agenda em dia.', ok: true })
                load()
              }}
            />
          ) : (
            <button type="button" className="btn" onClick={() => setShowWizard(true)}>
              <Link2 size={18} /> Conectar gaveta
            </button>
          )
        ) : (
          <p style={{ color: 'var(--color-text-muted)' }}>Peça ao seu cuidador para conectar a gaveta pelo app.</p>
        )}
      </section>
    )
  }

  const agenda = agendaText(device)
  const busy = busyAction !== null

  return (
    <section className="card" aria-labelledby="device-title" style={{ marginBottom: '1.5rem' }}>
      <div className="page-header">
        <h2 id="device-title" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Box size={22} /> Gaveta
        </h2>
        <span className={`device-pill ${device.online ? 'device-pill-online' : 'device-pill-offline'}`}>
          {device.online ? <Wifi size={16} /> : <WifiOff size={16} />}
          {connectionText(device)}
        </span>
      </div>

      <ul className="device-facts">
        <li>
          Última conexão: <strong>{formatDeviceDateTime(device.lastSeenAt) || 'ainda não conectou'}</strong>
        </li>
        <li className={agenda.ok ? undefined : 'device-fact-warning'}>{agenda.text}</li>
        {device.alarmActive && <li className="device-fact-warning">Alarme tocando na gaveta agora</li>}
        {device.refillMode && <li>Modo reabastecer ligado: abrir a gaveta agora não conta como dose</li>}
      </ul>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      {feedback && <Feedback feedback={feedback} />}

      {alerts.length > 0 && (
        <div className="device-alerts" aria-labelledby="device-alerts-title">
          <h3 id="device-alerts-title">Alertas da gaveta</h3>
          <ul>
            {alerts.map((alert) => (
              <li key={alert.eventId}>
                <span>
                  <strong>{formatDeviceDateTime(alert.occurredAt)}</strong> — {alert.description}
                </span>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={busy}
                  onClick={() =>
                    runCommand('acknowledge', () => deviceApi.acknowledgeAlert(patientId, alert.eventId))
                  }
                >
                  <CheckCircle2 size={18} />
                  {alert.type === 'ABERTURA_FORA_HORARIO' ? 'Fui eu' : 'Ciente'}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="device-actions">
        {device.refillMode ? (
          <button type="button" className="btn" disabled={busy} onClick={() => command('refill/stop')}>
            <PackagePlus size={18} /> {busyAction === 'refill/stop' ? 'Aguardando a gaveta...' : 'Encerrar reabastecer'}
          </button>
        ) : (
          <button type="button" className="btn" disabled={busy} onClick={() => command('refill', { minutes: 10 })}>
            <PackagePlus size={18} /> {busyAction === 'refill' ? 'Aguardando a gaveta...' : 'Reabastecer (10 min)'}
          </button>
        )}
        <button type="button" className="btn btn-secondary" disabled={busy} onClick={() => command('locate')}>
          <MapPin size={18} /> {busyAction === 'locate' ? 'Aguardando a gaveta...' : 'Localizar'}
        </button>
        {device.alarmActive && (
          <button type="button" className="btn btn-secondary" disabled={busy} onClick={() => command('silence-alarm')}>
            <BellOff size={18} /> Silenciar alarme
          </button>
        )}
        {device.agendaStatus !== 'SINCRONIZADA' && (
          <button type="button" className="btn btn-secondary" disabled={busy} onClick={() => command('resync-agenda')}>
            <RefreshCw size={18} /> Reenviar agenda
          </button>
        )}
      </div>

      {isCaregiver && (
        <div className="device-actions" style={{ marginTop: '0.75rem' }}>
          {confirming === 'reset-wifi' ? (
            <ConfirmBox
              text='A gaveta vai esquecer o Wi-Fi atual e abrir a rede "RemedIoT-..." para configurar outro. Continuar?'
              confirmLabel="Sim, trocar Wi-Fi"
              onConfirm={() => command('reset-wifi')}
              onCancel={() => setConfirming(null)}
              disabled={busy}
            />
          ) : confirming === 'unpair' ? (
            <ConfirmBox
              text="A gaveta para de acompanhar as doses deste paciente. Continuar?"
              confirmLabel="Sim, desvincular"
              onConfirm={handleUnpair}
              onCancel={() => setConfirming(null)}
              disabled={busy}
            />
          ) : (
            <>
              <button
                type="button"
                className="btn btn-secondary"
                disabled={busy}
                onClick={() => setConfirming('reset-wifi')}
              >
                <Wifi size={18} /> Trocar Wi-Fi
              </button>
              <button
                type="button"
                className="btn btn-danger-outline"
                disabled={busy}
                onClick={() => setConfirming('unpair')}
              >
                <Unlink size={18} /> Desvincular
              </button>
            </>
          )}
        </div>
      )}

      <details className="device-history">
        <summary>Histórico da gaveta</summary>
        {history.length === 0 ? (
          <p>Nenhum evento ainda.</p>
        ) : (
          <ul>
            {history.map((event) => (
              <li key={event.eventId}>
                <strong>{formatDeviceDateTime(event.occurredAt)}</strong>
                {!event.timeReliable && ' (hora aproximada)'} — {event.description}
                {event.caregiverOpening && ' · aberta pelo cuidador'}
              </li>
            ))}
          </ul>
        )}
      </details>
    </section>
  )
}

function Feedback({ feedback }) {
  return (
    <div className={`alert ${feedback.ok ? 'alert-success' : 'alert-danger'}`} role="status" aria-live="polite">
      {feedback.text}
    </div>
  )
}

function ConfirmBox({ text, confirmLabel, onConfirm, onCancel, disabled }) {
  return (
    <div className="confirm-box" role="alertdialog" aria-label={confirmLabel}>
      <p style={{ margin: 0 }}>{text}</p>
      <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
        <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={disabled}>
          {confirmLabel}
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={disabled}>
          Cancelar
        </button>
      </div>
    </div>
  )
}
