import { useData } from '../../context/DataContext'
import { challenges } from '../../data/mockData'
import './LiveBar.css'

// Franja oscura de arriba con datos "en vivo" calculados de los datos simulados.
function LiveBar() {
  const { beats } = useData()
  const openChallenges = challenges.filter(c => c.status !== 'CLOSED').length

  return (
    <p className="live-bar">
      <span className="live-dot" aria-hidden="true" />
      En vivo · {openChallenges} desafíos abiertos · {beats.length} beats publicados
    </p>
  )
}

export default LiveBar
