import JuryBand from '../components/layout/JuryBand'
import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import { useAuth } from '../context/AuthContext'
import { useData } from '../context/DataContext'
import { PRIZE_POINTS, RANKING_TOTAL_PRODUCERS, challenges } from '../data/mockData'
import './home/Home.css'

// Las tres cosas que se pueden hacer en la plataforma, cada una con su link.
// Están en un array para dibujar las tarjetas con un solo map.
const FEATURES = [
  {
    title: 'Publicar beats',
    text: 'Subí tu beat con género, BPM y tonalidad para que otros productores y artistas lo encuentren.',
    to: '/beats',
    linkText: 'Ver beats',
  },
  {
    title: 'Competir en desafíos',
    text: 'Entrá a los desafíos abiertos. Un jurado puntúa cada entrega de 1 a 10 y deja su comentario.',
    to: '/desafios',
    linkText: 'Ver desafíos',
  },
  {
    title: 'Subir en el ranking',
    text: 'Cada podio suma puntos a tu perfil y te acerca al primer puesto de la tabla.',
    to: '/ranking',
    linkText: 'Ver ranking',
  },
]

const LATEST_COUNT = 3

// Landing (boards 1 y 1b del wireframe): presenta el negocio y lleva a las
// secciones principales. Los números salen de los datos simulados, así
// coinciden con la franja "En vivo" de arriba.
function HomePage() {
  const { user } = useAuth()
  const { beats } = useData()
  const openChallenges = challenges.filter(c => c.status !== 'CLOSED').length
  // Los beats nuevos se agregan al principio del array, así que los primeros
  // son los más recientes.
  const latestBeats = beats.slice(0, LATEST_COUNT)

  const stats = [
    { label: 'Beats publicados', value: beats.length },
    { label: 'Desafíos abiertos', value: openChallenges },
    { label: 'Productores', value: RANKING_TOTAL_PRODUCERS },
  ]

  return (
    <>
      <PageHeader
        title="Tu música, frente a quien la tiene que escuchar"
        description="Publicá tus beats, competí en desafíos votados por un jurado y sumá puntos para llegar al primer puesto del ranking."
        stats={stats}
      >
        <Button to="/beats">Ver beats</Button>
        {/* Con sesión iniciada no tiene sentido "Crear cuenta". */}
        {user ? (
          <Button to="/desafios" variant="secondary">Ver desafíos</Button>
        ) : (
          <Button to="/registro" variant="secondary">Crear cuenta</Button>
        )}
      </PageHeader>

      <section className="section" aria-labelledby="features-title">
        <div className="container">
          <h2 id="features-title">Qué podés hacer</h2>
          <ul className="feature-list">
            {FEATURES.map(feature => (
              <li key={feature.to}>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
                <Button variant="link" to={feature.to}>
                  {feature.linkText} <span aria-hidden="true">→</span>
                </Button>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <JuryBand>
        Cada desafío lo define un jurado mixto. Al cerrar se reparten {PRIZE_POINTS.join(', ')} puntos.
      </JuryBand>

      <section className="section" aria-labelledby="latest-title">
        <div className="container">
          <div className="section-head">
            <h2 id="latest-title">Últimos beats</h2>
            <Button variant="link" to="/beats">
              Ver todos<span className="visually-hidden"> los beats</span> <span aria-hidden="true">→</span>
            </Button>
          </div>
          <ul className="beat-list">
            {latestBeats.map(beat => (
              <li key={beat.id} className="beat-row">
                <div>
                  <h3>{beat.title}</h3>
                  <p className="beat-meta">
                    {beat.genre} / {beat.bpm} BPM / {beat.key}
                  </p>
                </div>
                <p className="beat-row-info">por {beat.ownerName}</p>
                <Button variant="link" to={`/beats/${beat.id}`}>
                  Ver detalle<span className="visually-hidden"> de {beat.title}</span>
                  <span aria-hidden="true">→</span>
                </Button>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </>
  )
}

export default HomePage
