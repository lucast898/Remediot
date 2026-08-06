const LABELS = {
  1: 'Peso 1 · tranquilo',
  2: 'Peso 2 · sério',
  3: 'Peso 3 · não pode atrasar',
}

export default function WeightBadge({ weight }) {
  return <span className={`badge-weight-${weight}`}>{LABELS[weight] || `Peso ${weight}`}</span>
}
