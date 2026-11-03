import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import { useAuth } from '../context/AuthContext'
import { useData } from '../context/DataContext'
import { TOPLINE_STATUS } from '../data/mockData'
import MyToplinesSection from './collaborations/MyToplinesSection'
import ProposalsSection from './collaborations/ProposalsSection'
import './collaborations/Collaborations.css'

const DESCRIPTION =
  'Cuando un artista sube un topline sobre un beat, se crea una propuesta de colaboración. Acá ves las que te llegaron y los toplines que subiste vos.'

// Colaboraciones (wireframe 3b). Dos bandejas en una pantalla: lo que le llegó
// a mis beats y los toplines que subí yo.
// Es la única pantalla que depende por completo de quién sos, así que primero
// resuelve el acceso y recién después calcula los datos.
function CollaborationsPage() {
  const { user } = useAuth()
  const { beats, toplines } = useData()

  // Sin sesión no hay "mis beats" ni "mis toplines" que mostrar.
  if (!user) {
    return (
      <>
        <PageHeader title="Colaboraciones" description={DESCRIPTION} />
        <section className="section">
          <div className="container">
            <EmptyState title="Tenés que iniciar sesión para ver tus colaboraciones.">
              <p>Dependen de tus beats y de los toplines que hayas subido.</p>
              <Button to="/login">Entrar</Button>
            </EmptyState>
          </div>
        </section>
      </>
    )
  }

  // Una discográfica no publica beats ni sube toplines: la pantalla no le
  // aplica. Se lo decimos en vez de mostrarle dos listas vacías.
  if (user.role !== 'ARTIST') {
    return (
      <>
        <PageHeader title="Colaboraciones" description={DESCRIPTION} />
        <section className="section">
          <div className="container">
            <EmptyState title="Las colaboraciones son solo para artistas.">
              <p>
                Tu cuenta es de discográfica. Podés seguir los desafíos y el ranking para
                encontrar productores.
              </p>
              <Button to="/desafios">Ver desafíos</Button>
            </EmptyState>
          </div>
        </section>
      </>
    )
  }

  const myBeats = beats.filter(beat => beat.ownerName === user.name)
  const myBeatIds = myBeats.map(beat => beat.id)
  const received = toplines.filter(topline => myBeatIds.includes(topline.beatId))
  const pending = received.filter(topline => topline.status === TOPLINE_STATUS.PENDING)
  const myToplines = toplines.filter(topline => topline.artistName === user.name)

  // Mapa id -> beat, para que "Mis toplines" pueda nombrar el beat de cada fila
  // sin recorrer la lista entera cada vez.
  const beatsById = Object.fromEntries(beats.map(beat => [beat.id, beat]))

  return (
    <>
      <PageHeader
        title="Colaboraciones"
        description={DESCRIPTION}
        stats={[
          { value: received.length, label: 'Propuestas recibidas' },
          { value: pending.length, label: 'Pendientes' },
          { value: myToplines.length, label: 'Toplines subidos' },
        ]}
      />

      <ProposalsSection myBeats={myBeats} />
      <MyToplinesSection myToplines={myToplines} beatsById={beatsById} />
    </>
  )
}

export default CollaborationsPage
