import { AlertOctagon, AlertTriangle, Feather } from 'lucide-react'

const INFO = {
  1: { label: 'Peso 1 · tranquilo', Icon: Feather },
  2: { label: 'Peso 2 · sério', Icon: AlertTriangle },
  3: { label: 'Peso 3 · não pode atrasar', Icon: AlertOctagon },
}

export default function WeightBadge({ weight }) {
  const info = INFO[weight]
  const Icon = info?.Icon || Feather
  return (
    <span className={`badge-weight-${weight}`}>
      <Icon size={14} strokeWidth={2.5} />
      {info?.label || `Peso ${weight}`}
    </span>
  )
}
