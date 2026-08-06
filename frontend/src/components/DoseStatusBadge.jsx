const STATUS = {
  AGENDADA: { label: 'Agendada', className: 'badge-status badge-status-agendada' },
  TOMADA: { label: 'Registrada no horário', className: 'badge-status badge-status-tomada' },
  ATRASADA: { label: 'Registrada com atraso', className: 'badge-status badge-status-atrasada' },
  NAO_RETIRADA: { label: 'Não retirada', className: 'badge-status badge-status-nao-retirada' },
}

export default function DoseStatusBadge({ status }) {
  const info = STATUS[status] || { label: status, className: 'badge-status' }
  return <span className={info.className}>{info.label}</span>
}
