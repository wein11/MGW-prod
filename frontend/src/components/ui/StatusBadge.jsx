// ============================================================================
// PARA ESTUDIAR: StatusBadge.jsx (etiqueta de estado)
// ----------------------------------------------------------------------------
// QUÉ ES: la etiquetita de color "Pendiente", "Aceptada", "Abierto", etc.
// CÓMO FUNCIONA: recibe el estado en inglés (como viene del backend, ej.
//   'PENDING'), busca el texto en español en LABELS y arma la clase CSS
//   'badge-pending' para el color.
// ACCESIBILIDAD: el estado se entiende por el TEXTO, no solo por el color.
//   Una persona daltónica no distingue verde de rojo (criterio 3: contraste y
//   no depender solo del color).
// ============================================================================
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
