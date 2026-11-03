import { useParams } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import { useAuth } from '../context/AuthContext'
import { useData } from '../context/DataContext'
import { formatDate } from '../utils/format'
import CommentsSection from './beats/CommentsSection'
import ManageBeat from './beats/ManageBeat'
import ToplinesSection from './beats/ToplinesSection'
import './beats/Beats.css'

// Detalle de un beat (board 3 del wireframe). El id sale de la URL
// (/beats/:id). Cada bloque es un componente aparte en pages/beats/ para que
// esta página solo decida qué se muestra según quién mira.
function BeatDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const { beats, comments } = useData()
  // useParams devuelve texto: se pasa a número para comparar con beat.id.
  const beat = beats.find(item => item.id === Number(id))

  if (!beat) {
    return (
      <>
        <PageHeader title="Beat no encontrado" description="Puede que lo hayan borrado o que el link esté mal." />
        <section className="section">
          <div className="container">
            <EmptyState title="Este beat no existe">
              <p>Volvé al listado para ver los beats publicados.</p>
              <Button to="/beats">Ir a Beats</Button>
            </EmptyState>
          </div>
        </section>
      </>
    )
  }

  const commentCount = comments.filter(comment => comment.beatId === beat.id).length
  const isOwner = user?.name === beat.ownerName

  return (
    <>
      <PageHeader
        title={beat.title}
        description={`${beat.genre} / ${beat.bpm} BPM / ${beat.key}`}
        stats={[
          { label: 'Comentarios', value: commentCount },
          { label: 'Publicado', value: formatDate(beat.publishedAt) },
          { label: 'Productor', value: beat.ownerName },
        ]}
      >
        <Button variant="link" to="/beats">
          <span aria-hidden="true">←</span> Volver a beats
        </Button>
      </PageHeader>

      <section className="section" aria-label="Escuchar">
        <div className="container beat-media">
          <img src={beat.cover} alt={`Portada de ${beat.title}`} width="256" height="256" />
          <div>
            {/* Los audios son links externos (SoundCloud): no se pueden
                reproducir con <audio>, así que se abren en otra pestaña.
                Es un <a> común con la clase del botón porque Button solo
                sabe hacer links internos (to) o botones de acción. */}
            <a className="btn btn-secondary" href={beat.audioUrl} target="_blank" rel="noopener noreferrer">
              Escuchar beat<span className="visually-hidden"> (se abre en otra pestaña)</span>
            </a>
            {beat.description && <p className="beat-description">{beat.description}</p>}
          </div>
        </div>
      </section>

      <CommentsSection beatId={beat.id} />
      {/* key: si cambia el beat, el formulario se reinicia con sus datos. */}
      {isOwner && <ManageBeat key={beat.id} beat={beat} />}
      <ToplinesSection beat={beat} isOwner={isOwner} />
    </>
  )
}

export default BeatDetailPage
