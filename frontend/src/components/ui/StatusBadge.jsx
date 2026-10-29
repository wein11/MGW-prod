import './StatusBadge.css'

const LABELS = {
  PENDING: 'Pendiente',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada',
  OPEN: 'Abierto',
  VOTING: 'En votación',
  CLOSED: 'Cerrado',
}

// Etiqueta de estado. El texto siempre está escrito (no solo el color), para
// que se entienda sin distinguir colores.
// status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'OPEN' | 'VOTING' | 'CLOSED'
function StatusBadge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>
}

export default StatusBadge
