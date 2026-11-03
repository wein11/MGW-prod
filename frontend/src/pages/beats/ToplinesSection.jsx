import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useAuth } from '../../context/AuthContext'
import { useData } from '../../context/DataContext'
import { fakeRequest } from '../../data/fakeRequest'
import { TOPLINE_STATUS } from '../../data/mockData'
import { formatDate, plural, todayIso } from '../../utils/format'
import { focusFirstError, hasErrors } from '../../utils/form'
import { nextId } from '../../utils/ids'
import { isValidHttpsUrl } from '../../utils/validation'

// Toplines de un beat. Quién puede hacer qué:
// - el dueño del beat acepta o rechaza los pendientes;
// - quien subió un topline lo puede borrar;
// - cualquier ARTIST que no sea el dueño puede subir uno nuevo.
// Los cambios van al DataContext, así se ven también en Colaboraciones.
function ToplinesSection({ beat, isOwner }) {
  const { user } = useAuth()
  const { toplines, setToplines } = useData()
  const [audioUrl, setAudioUrl] = useState('')
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [uploaded, setUploaded] = useState(false)
  // Id del elemento que tiene que recibir el foco después de un cambio.
  const [focusTarget, setFocusTarget] = useState(null)

  const beatToplines = toplines.filter(topline => topline.beatId === beat.id)
  const canUpload = user?.role === 'ARTIST' && !isOwner

  // Al aceptar, rechazar o borrar, el botón que se tocó desaparece y el foco
  // quedaría perdido en la página. Esto lo lleva a la fila (que anuncia el
  // nuevo estado) o al título de la sección, para quien usa teclado.
  useEffect(() => {
    if (focusTarget) document.getElementById(focusTarget)?.focus()
  }, [focusTarget])

  function changeStatus(toplineId, status) {
    setToplines(current =>
      current.map(topline => (topline.id === toplineId ? { ...topline, status } : topline))
    )
    setFocusTarget(`topline-${toplineId}`)
  }

  function removeTopline(toplineId) {
    setToplines(current => current.filter(topline => topline.id !== toplineId))
    setFocusTarget('toplines-title')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    setUploaded(false)
    const found = {}
    if (!isValidHttpsUrl(audioUrl)) {
      found['topline-url'] = 'El link tiene que empezar con https:// (por ejemplo, de SoundCloud).'
    }
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    const newTopline = {
      id: nextId(toplines),
      beatId: beat.id,
      artistName: user.name,
      audioUrl: audioUrl.trim(),
      status: TOPLINE_STATUS.PENDING,
      createdAt: todayIso(),
    }
    setToplines(current => [newTopline, ...current])
    setSending(false)
    setUploaded(true)
    setAudioUrl('')
  }

  return (
    <section className="section" aria-labelledby="toplines-title">
      <div className="container">
        <div className="section-head">
          <h2 id="toplines-title" tabIndex={-1}>Toplines sobre este beat</h2>
          <p>{plural(beatToplines.length, 'topline')}</p>
        </div>
        <p className="section-intro">
          Un topline es la voz de un artista sobre este beat. Al subirlo se crea una propuesta de
          colaboración que el dueño del beat puede aceptar o rechazar.
        </p>

        {beatToplines.length === 0 ? (
          <p>Todavía nadie subió un topline.</p>
        ) : (
          <ul className="item-list">
            {beatToplines.map(topline => (
              // tabIndex -1: la fila puede recibir foco desde el código pero no
              // suma un paso más al recorrer la página con Tab.
              <li key={topline.id} id={`topline-${topline.id}`} tabIndex={-1} className="topline-row">
                <div>
                  <h3>{topline.artistName}</h3>
                  <p className="beat-meta">
                    <time dateTime={topline.createdAt}>{formatDate(topline.createdAt)}</time>
                  </p>
                </div>
                <div className="topline-actions">
                  <StatusBadge status={topline.status} />
                  <a href={topline.audioUrl} target="_blank" rel="noopener noreferrer">
                    Escuchar topline<span className="visually-hidden"> de {topline.artistName} (se abre en otra pestaña)</span>
                  </a>
                  {isOwner && topline.status === TOPLINE_STATUS.PENDING && (
                    <>
                      <Button onClick={() => changeStatus(topline.id, TOPLINE_STATUS.ACCEPTED)}>
                        Aceptar<span className="visually-hidden"> topline de {topline.artistName}</span>
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() => changeStatus(topline.id, TOPLINE_STATUS.REJECTED)}
                      >
                        Rechazar<span className="visually-hidden"> topline de {topline.artistName}</span>
                      </Button>
                    </>
                  )}
                  {user?.name === topline.artistName && (
                    <ConfirmDelete
                      label="tu topline"
                      buttonText="Borrar topline"
                      onConfirm={() => removeTopline(topline.id)}
                    />
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        {/* El dueño no sube toplines a su propio beat: para él no hay bloque. */}
        {!isOwner && <h3>Subir un topline</h3>}
        {canUpload && (
          <form className="narrow-form" onSubmit={handleSubmit} noValidate aria-label="Subir un topline">
            <FormField
              id="topline-url"
              label="Link al audio"
              type="url"
              value={audioUrl}
              onChange={setAudioUrl}
              error={errors['topline-url']}
              placeholder="https://…"
              required
            />
            <Button type="submit" disabled={sending}>
              {sending ? 'Enviando…' : 'Subir topline'}
            </Button>
            {uploaded && (
              <Alert kind="success">
                Listo: topline subido. Se creó una propuesta de colaboración pendiente.
              </Alert>
            )}
          </form>
        )}
        {!user && (
          <p>
            <Link to="/login">Entrá con tu cuenta de artista</Link> para subir un topline.
          </p>
        )}
        {user && user.role !== 'ARTIST' && <p>Solo las cuentas de artista pueden subir toplines.</p>}
      </div>
    </section>
  )
}

export default ToplinesSection
