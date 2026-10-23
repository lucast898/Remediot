import { deviceApi } from '../services/api'

// Os horários da gaveta chegam em UTC; mostramos sempre no horário de São Paulo
export function formatDeviceDateTime(instant) {
  if (!instant) return ''
  return new Date(instant).toLocaleString('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function connectionText(device) {
  if (device.online) return 'Conectada'
  switch (device.offlineReason) {
    case 'REINICIO_SOLICITADO':
      return 'Reiniciando...'
    case 'TROCANDO_WIFI':
      return 'Aguardando o Wi-Fi novo'
    case 'CONEXAO_PERDIDA':
      return 'Sem conexão'
    default:
      return 'Desconectada'
  }
}

const AGENDA_ERRORS = {
  DOSES_DEMAIS: 'A gaveta guarda no máximo 40 horários. Remova alguns horários para enviar a agenda.',
  AGENDA_GRANDE_DEMAIS: 'A agenda ficou grande demais para a gaveta. Remova alguns horários.',
  VERSAO_ANTIGA: 'A gaveta já tem uma agenda mais nova. Toque em "Reenviar agenda".',
  HORA_INVALIDA: 'Algum horário está num formato que a gaveta não entende.',
  PESO_INVALIDO: 'Algum remédio está com peso inválido.',
}

export function agendaText(device) {
  switch (device.agendaStatus) {
    case 'SINCRONIZADA':
      return { text: 'Agenda em dia na gaveta', ok: true }
    case 'AGUARDANDO':
      return { text: 'Agenda enviada, aguardando a gaveta confirmar', ok: false }
    case 'REJEITADA': {
      const reasons = (device.agendaErrors || []).map((code) => AGENDA_ERRORS[code] || `Erro ${code}`)
      return { text: `A gaveta não aceitou a agenda. ${reasons.join(' ')}`.trim(), ok: false }
    }
    default:
      return { text: 'Nenhuma agenda enviada ainda', ok: false }
  }
}

const SUCCESS = {
  refill: 'Modo reabastecer ligado. Pode abrir a gaveta para repor os remédios: isso não conta como dose.',
  'refill/stop': 'Modo reabastecer desligado.',
  locate: 'A gaveta está piscando e apitando para você encontrá-la.',
  'silence-alarm': 'Alarme silenciado. A dose continua pendente até a gaveta ser aberta.',
  'request-status': 'Status atualizado.',
  'resync-agenda': 'Agenda reenviada para a gaveta.',
  'reset-wifi': 'A gaveta apagou o Wi-Fi e abriu a rede de configuração "RemedIoT-...".',
  restart: 'A gaveta está reiniciando.',
  acknowledge: 'Pronto, a gaveta foi avisada.',
}

const DEVICE_MESSAGES = {
  DOSE_EM_ANDAMENTO: 'Não dá para reabastecer perto do horário de uma dose.',
  SEM_ALARME_ATIVO: 'Não há alarme tocando agora.',
  MODO_NAO_ESTAVA_ATIVO: 'O modo reabastecer já estava desligado.',
  ALERTA_NAO_PENDENTE: 'A gaveta já tinha resolvido esse alerta.',
}

/** Texto para o usuário a partir da resposta final de um comando. */
export function commandResultText(action, command) {
  const deviceMessage = command.message && DEVICE_MESSAGES[command.message]
  switch (command.status) {
    case 'OK':
      return { text: SUCCESS[action] || 'Comando executado.', ok: true }
    case 'IGNORADO':
      return { text: deviceMessage || 'Não havia nada a fazer.', ok: true }
    case 'DUPLICADO':
      return { text: 'A gaveta já tinha recebido esse comando.', ok: true }
    case 'EXPIRADO':
      return { text: 'O comando chegou atrasado na gaveta e não foi executado. Tente de novo.', ok: false }
    case 'SEM_RESPOSTA':
      return { text: 'A gaveta não respondeu. Confira se ela está ligada e com internet.', ok: false }
    default:
      return {
        text: deviceMessage || `A gaveta não executou o comando${command.message ? ` (${command.message})` : ''}.`,
        ok: false,
      }
  }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/** Consulta o comando até a gaveta responder (o backend desiste em 30 s: SEM_RESPOSTA). */
export async function waitForCommand(patientId, command, { intervalMs = 1500, maxWaitMs = 40000 } = {}) {
  let current = command
  const deadline = Date.now() + maxWaitMs
  while (current.status === 'PENDENTE' && Date.now() < deadline) {
    await sleep(intervalMs)
    current = await deviceApi.commandStatus(patientId, current.commandId)
  }
  return current
}
