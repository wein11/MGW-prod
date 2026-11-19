import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import EmptyState from '../../components/ui/EmptyState'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useApp } from '../../context/AppContext'
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
  const { toplines, setToplines } = useApp()
  const [statusFilter, setStatusFilter] = useState('ALL')
  // Mensaje de confirmación de la última acción. Se muestra con <Alert>, que
  // tiene role="status": el lector de pantalla lo lee solo.
  const [message, setMessage] = useState('')

  const myBeatIds = myBeats.map(beat => beat.id)
  const received = toplines.filter(topline => myBeatIds.includes(topline.beatId))
  const visible =
    statusFilter === 'ALL' ? received : received.filter(topline => topline.status === statusFilter)

  // Solo los beats que tienen alguna propuesta visible con el filtro actual.
  const groups = myBeats
    .map(beat => ({ beat, items: visible.filter(topline => topline.beatId === beat.id) }))
    .filter(group => group.items.length > 0)

  function changeStatus(topline, status) {
    setToplines(current =>
      current.map(item => (item.id === topline.id ? { ...item, status } : item))
    )
    const action = status === TOPLINE_STATUS.ACCEPTED ? 'Aceptaste' : 'Rechazaste'
    setMessage(`${action} la propuesta de ${topline.artistName}.`)
  }

  function remove(topline) {
    setToplines(current => current.filter(item => item.id !== topline.id))
    setMessage(`Quitaste la propuesta de ${topline.artistName}.`)
  }

  return (
    <section className="section" aria-labelledby="proposals-title">
      <div className="container">
        <div className="section-head">
          <h2 id="proposals-title">Propuestas sobre mis beats</h2>
          <p>{plural(visible.length, 'propuesta')}</p>
        </div>

        {message && <Alert kind="success">{message}</Alert>}

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
                  <li key={topline.id} className="topline-row">
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
                          <Button onClick={() => changeStatus(topline, TOPLINE_STATUS.ACCEPTED)}>
                            Aceptar
                            <span className="visually-hidden"> propuesta de {topline.artistName}</span>
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() => changeStatus(topline, TOPLINE_STATUS.REJECTED)}
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
                          onConfirm={() => remove(topline)}
                        />
                      ) : (
                        <Button variant="secondary" onClick={() => remove(topline)}>
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
