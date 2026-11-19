// ============================================================================
// PARA ESTUDIAR: ToplinesSection.jsx (toplines de un beat)
// ----------------------------------------------------------------------------
// QUÉ ES: un topline es la voz/melodía que un artista graba sobre un beat
//   ajeno. Subirlo crea una propuesta de colaboración PENDIENTE.
// PERMISOS SEGÚN QUIÉN MIRA:
//   - Dueño del beat: Aceptar o Rechazar los pendientes.
//   - Quien subió el topline: lo puede borrar (ConfirmDelete).
//   - Otro artista (no dueño): ve el formulario para subir uno nuevo.
// MENSAJES: useState `message` + <Alert kind="success">. Cada acción deja un
//   texto ("Aceptaste el topline de Kira Beats.") que el lector de pantalla
//   lee solo (role="status").
// TODO VA AL CONTEXT (setToplines): por eso lo que aceptás acá aparece igual
//   en la página Colaboraciones.
// ============================================================================
import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useApp } from '../../context/AppContext'
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
// Los cambios van al AppContext, así se ven también en Colaboraciones.
function ToplinesSection({ beat, isOwner }) {
  const { user, toplines, setToplines } = useApp()
  const [audioUrl, setAudioUrl] = useState('')
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [uploaded, setUploaded] = useState(false)
  // Mensaje de confirmación de la última acción. Se muestra con <Alert>, que
  // tiene role="status": el lector de pantalla lo lee solo.
  const [message, setMessage] = useState('')

  const beatToplines = toplines.filter(topline => topline.beatId === beat.id)
  const canUpload = user?.role === 'ARTIST' && !isOwner

  function changeStatus(topline, status) {
    setToplines(current =>
      current.map(item => (item.id === topline.id ? { ...item, status } : item))
    )
    const action = status === TOPLINE_STATUS.ACCEPTED ? 'Aceptaste' : 'Rechazaste'
    setMessage(`${action} el topline de ${topline.artistName}.`)
  }

  function removeTopline(toplineId) {
    setToplines(current => current.filter(topline => topline.id !== toplineId))
    setMessage('Borraste tu topline.')
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
          <h2 id="toplines-title">Toplines sobre este beat</h2>
          <p>{plural(beatToplines.length, 'topline')}</p>
        </div>
        <p className="section-intro">
          Un topline es la voz de un artista sobre este beat. Al subirlo se crea una propuesta de
          colaboración que el dueño del beat puede aceptar o rechazar.
        </p>

        {message && <Alert kind="success">{message}</Alert>}

        {beatToplines.length === 0 ? (
          <p>Todavía nadie subió un topline.</p>
        ) : (
          <ul className="item-list">
            {beatToplines.map(topline => (
              <li key={topline.id} className="topline-row">
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
                      <Button onClick={() => changeStatus(topline, TOPLINE_STATUS.ACCEPTED)}>
                        Aceptar<span className="visually-hidden"> topline de {topline.artistName}</span>
                      </Button>
                      <Button
                        variant="secondary"
                        onClick={() => changeStatus(topline, TOPLINE_STATUS.REJECTED)}
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
