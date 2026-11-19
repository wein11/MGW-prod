// ============================================================================
// PARA ESTUDIAR: LiveBar.jsx (franja negra de arriba)
// ----------------------------------------------------------------------------
// QUÉ ES: muestra "En vivo · 2 desafíos abiertos · 8 beats publicados".
// DATO DERIVADO: los números NO se guardan en un estado aparte; se calculan
//   en cada render a partir de los datos. beats.length sale del Context, así
//   que si publicás un beat, el número sube solo (React vuelve a dibujar).
//   Regla de React: si un valor se puede calcular de otro, no hace falta useState.
// El puntito rojo tiene aria-hidden porque es solo decoración.
// ============================================================================
import { useApp } from '../../context/AppContext'
import { challenges } from '../../data/mockData'
import './LiveBar.css'

// Franja oscura de arriba con datos "en vivo" calculados de los datos simulados.
function LiveBar() {
  const { beats } = useApp()
  const openChallenges = challenges.filter(c => c.status !== 'CLOSED').length

  return (
    <p className="live-bar">
      <span className="live-dot" aria-hidden="true" />
      En vivo · {openChallenges} desafíos abiertos · {beats.length} beats publicados
    </p>
  )
}

export default LiveBar
