import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import Alert from '../components/ui/Alert'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import FormField from '../components/ui/FormField'
import { useData } from '../context/DataContext'
import { GENRES } from '../data/mockData'
import { plural } from '../utils/format'
import PublishBeatForm from './beats/PublishBeatForm'
import './beats/Beats.css'

const EMPTY_FILTERS = { genre: '', bpm: '', producer: '' }

const GENRE_OPTIONS = [
  { value: '', label: 'Todos' },
  ...GENRES.map(genre => ({ value: genre, label: genre })),
]

// Listado de beats (board 2 del wireframe): formulario para publicar y la
// lista con filtros por género, BPM y productor. Los filtros no guardan una
// lista aparte: en cada render se calcula "visibles" a partir de los beats
// del contexto, así un beat recién publicado aparece sin pasos extra.
function BeatsPage() {
  const { beats, comments } = useData()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  // Si venimos de borrar un beat, el detalle manda su título en el state de
  // la navegación para mostrar la confirmación acá.
  const deletedTitle = useLocation().state?.deletedTitle

  // Productores sin repetir, ordenados, para armar el select.
  const producers = [...new Set(beats.map(beat => beat.ownerName))].sort()
  const producerOptions = [
    { value: '', label: 'Todos' },
    ...producers.map(name => ({ value: name, label: name })),
  ]
  const genreCount = new Set(beats.map(beat => beat.genre)).size

  const visibleBeats = beats.filter(beat =>
    (filters.genre === '' || beat.genre === filters.genre) &&
    (filters.bpm === '' || beat.bpm === Number(filters.bpm)) &&
    (filters.producer === '' || beat.ownerName === filters.producer)
  )

  function setFilter(key) {
    return value => setFilters(current => ({ ...current, [key]: value }))
  }

  function countComments(beatId) {
    return comments.filter(comment => comment.beatId === beatId).length
  }

  return (
    <>
      <PageHeader
        title="Beats"
        description="Explorá lo que publica la comunidad o subí el tuyo."
        stats={[
          { label: 'Beats', value: beats.length },
          { label: 'Géneros distintos', value: genreCount },
        ]}
      />

      <section className="section" aria-label="Publicar">
        <div className="container">
          {deletedTitle && <Alert kind="success">El beat &quot;{deletedTitle}&quot; se borró.</Alert>}
          <PublishBeatForm />
        </div>
      </section>

      <section className="section" aria-labelledby="all-beats-title">
        <div className="container">
          <div className="section-head">
            <h2 id="all-beats-title">Todos los beats</h2>
            {/* aria-live: el lector de pantalla anuncia el nuevo total al filtrar. */}
            <p aria-live="polite">{plural(visibleBeats.length, 'beat')}</p>
          </div>

          {/* Los filtros se aplican al cambiar cada campo, por eso no hay
              botón "Buscar"; el preventDefault evita que Enter recargue. */}
          <form
            className="beat-filters"
            aria-label="Filtrar beats"
            onSubmit={event => event.preventDefault()}
          >
            <FormField
              id="filter-genre"
              label="Género"
              type="select"
              value={filters.genre}
              onChange={setFilter('genre')}
              options={GENRE_OPTIONS}
            />
            <FormField
              id="filter-bpm"
              label="BPM"
              type="number"
              inputMode="numeric"
              value={filters.bpm}
              onChange={setFilter('bpm')}
              placeholder="Ej: 140"
            />
            <FormField
              id="filter-producer"
              label="Productor"
              type="select"
              value={filters.producer}
              onChange={setFilter('producer')}
              options={producerOptions}
            />
            <Button variant="secondary" onClick={() => setFilters(EMPTY_FILTERS)}>
              Limpiar
            </Button>
          </form>

          {visibleBeats.length === 0 ? (
            <EmptyState title="No hay beats con esos filtros">
              <p>Probá con otro género, otro BPM o tocá &quot;Limpiar&quot;.</p>
            </EmptyState>
          ) : (
            <ul className="beat-list">
              {visibleBeats.map(beat => (
                <li key={beat.id} className="beat-row">
                  <div>
                    <h3>{beat.title}</h3>
                    <p className="beat-meta">
                      {beat.genre} / {beat.bpm} BPM / {beat.key}
                    </p>
                  </div>
                  <p className="beat-row-info">
                    <span>por {beat.ownerName}</span>
                    <span className="beat-row-count">{plural(countComments(beat.id), 'comentario')}</span>
                  </p>
                  {/* El texto oculto evita que haya muchos links iguales
                      ("Ver detalle") sin decir de qué beat son. */}
                  <Button variant="link" to={`/beats/${beat.id}`}>
                    Ver detalle<span className="visually-hidden"> de {beat.title}</span>
                    <span aria-hidden="true">→</span>
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </>
  )
}

export default BeatsPage
