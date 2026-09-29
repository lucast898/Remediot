import { AlertTriangle, CheckCircle2, Clock, XCircle } from 'lucide-react'

const STATUS = {
  AGENDADA: { label: 'Agendada', className: 'badge-status badge-status-agendada', Icon: Clock },
  TOMADA: { label: 'Registrada no horário', className: 'badge-status badge-status-tomada', Icon: CheckCircle2 },
  ATRASADA: { label: 'Registrada com atraso', className: 'badge-status badge-status-atrasada', Icon: AlertTriangle },
  NAO_RETIRADA: { label: 'Não retirada', className: 'badge-status badge-status-nao-retirada', Icon: XCircle },
}

export default function DoseStatusBadge({ status }) {
  const info = STATUS[status] || { label: status, className: 'badge-status', Icon: Clock }
  const { Icon } = info
  return (
    <span className={info.className}>
      <Icon size={14} strokeWidth={2.5} />
      {info.label}
    </span>
  )
}
