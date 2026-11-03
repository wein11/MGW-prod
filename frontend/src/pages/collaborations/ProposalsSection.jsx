import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import EmptyState from '../../components/ui/EmptyState'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useData } from '../../context/DataContext'
import { TOPLINE_STATUS } from '../../data/mockData'
import { formatDate, plural } from '../../utils/format'

// Propuestas que llegaron a los beats del usuario, agrupadas por beat.
// Se agrupa porque el dueño piensa "qué me llegó para este beat", no
// "qué me llegó en total": el beat es el contexto que necesita para decidir.
//
// Qué puede hacer según el estado de la propuesta (sale del wireframe 3b):
// - Pendiente: Aceptar, Rechazar o Quitar propuesta.
// - Aceptada:  Borrar topline, con confirmación en dos pasos.
// - Rechazada: Quitar propuesta.
// "Quitar propuesta" saca la propuesta de la bandeja sin preguntar porque no
// destruye nada que el dueño haya hecho; "Borrar topline" sí pregunta, porque
// deshace una colaboración que ya estaba aceptada.
function ProposalsSection({ myBeats }) {
  const { toplines, setToplines } = useData()
  const [statusFilter, setStatusFilter] = useState('ALL')
  // Id del elemento que tiene que recibir el foco después de una acción: el
  // botón que se tocó desaparece y, sin esto, el foco se pierde al principio
  // de la página. Va en un ref y no en estado porque no se dibuja: cambiarlo
  // no tiene que provocar otro render.
  const pendingFocus = useRef(null)

  // El efecto corre después de cada render, cuando el DOM ya refleja el cambio.
  // Si la fila dejó de existir (con el filtro en "Pendiente", la propuesta
  // recién aceptada ya no entra), el foco va al título de la sección, que
  // siempre está: si no, quedaría en el body.
  useEffect(() => {
    const id = pendingFocus.current
    if (!id) return
    pendingFocus.current = null
    const target = document.getElementById(id) ?? document.getElementById('proposals-title')
    target?.focus()
  })

  const myBeatIds = myBeats.map(beat => beat.id)
  const received = toplines.filter(topline => myBeatIds.includes(topline.beatId))
  const visible =
    statusFilter === 'ALL' ? received : received.filter(topline => topline.status === statusFilter)

  // Solo los beats que tienen alguna propuesta visible con el filtro actual.
  const groups = myBeats
    .map(beat => ({ beat, items: visible.filter(topline => topline.beatId === beat.id) }))
    .filter(group => group.items.length > 0)

  function changeStatus(toplineId, status) {
    setToplines(current =>
      current.map(topline => (topline.id === toplineId ? { ...topline, status } : topline))
    )
    // La fila anuncia el estado nuevo, así que el foco vuelve ahí.
    pendingFocus.current = `proposal-${toplineId}`
  }

  function remove(toplineId) {
    setToplines(current => current.filter(topline => topline.id !== toplineId))
    pendingFocus.current = 'proposals-title'
  }

  return (
    <section className="section" aria-labelledby="proposals-title">
      <div className="container">
        <div className="section-head">
          <h2 id="proposals-title" tabIndex={-1}>Propuestas sobre mis beats</h2>
          <p>{plural(visible.length, 'propuesta')}</p>
        </div>

        <div className="collab-filter">
          <FormField
            id="status-filter"
            label="Estado"
            type="select"
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { value: 'ALL', label: 'Todas' },
              { value: TOPLINE_STATUS.PENDING, label: 'Pendiente' },
              { value: TOPLINE_STATUS.ACCEPTED, label: 'Aceptada' },
              { value: TOPLINE_STATUS.REJECTED, label: 'Rechazada' },
            ]}
          />
        </div>

        {groups.length === 0 ? (
          <EmptyState title="No hay propuestas con ese estado">
            <p>Probá con otro estado o volvé a &ldquo;Todas&rdquo;.</p>
          </EmptyState>
        ) : (
          groups.map(({ beat, items }) => (
            <article key={beat.id} className="collab-group">
              <div className="collab-group-head">
                <h3>
                  <Link to={`/beats/${beat.id}`}>{beat.title}</Link>
                </h3>
                <p className="beat-meta">
                  {beat.genre} &middot; {beat.bpm} BPM &middot; {beat.key}
                </p>
              </div>

              <ul className="item-list">
                {items.map(topline => (
                  // tabIndex -1: la fila puede recibir foco desde el código,
                  // pero no agrega un paso más al recorrer con Tab.
                  <li
                    key={topline.id}
                    id={`proposal-${topline.id}`}
                    tabIndex={-1}
                    className="topline-row"
                  >
                    <div>
                      <h4>{topline.artistName}</h4>
                      <p className="beat-meta">
                        <time dateTime={topline.createdAt}>{formatDate(topline.createdAt)}</time>
                      </p>
                    </div>

                    <div className="topline-actions">
                      <StatusBadge status={topline.status} />
                      <a href={topline.audioUrl} target="_blank" rel="noopener noreferrer">
                        Escuchar topline
                        <span className="visually-hidden">
                          {' '}de {topline.artistName} para {beat.title} (se abre en otra pestaña)
                        </span>
                      </a>

                      {topline.status === TOPLINE_STATUS.PENDING && (
                        <>
                          <Button onClick={() => changeStatus(topline.id, TOPLINE_STATUS.ACCEPTED)}>
                            Aceptar
                            <span className="visually-hidden"> propuesta de {topline.artistName}</span>
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() => changeStatus(topline.id, TOPLINE_STATUS.REJECTED)}
                          >
                            Rechazar
                            <span className="visually-hidden"> propuesta de {topline.artistName}</span>
                          </Button>
                        </>
                      )}

                      {topline.status === TOPLINE_STATUS.ACCEPTED ? (
                        <ConfirmDelete
                          label={`el topline de ${topline.artistName} para "${beat.title}"`}
                          buttonText="Borrar topline"
                          onConfirm={() => remove(topline.id)}
                        />
                      ) : (
                        <Button variant="secondary" onClick={() => remove(topline.id)}>
                          Quitar propuesta
                          <span className="visually-hidden"> de {topline.artistName}</span>
                        </Button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </article>
          ))
        )}
      </div>
    </section>
  )
}

export default ProposalsSection
