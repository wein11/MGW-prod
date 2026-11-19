import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useApp } from '../../context/AppContext'
import { fakeRequest } from '../../data/fakeRequest'
import { formatDate, plural } from '../../utils/format'
import { focusFirstError, hasErrors } from '../../utils/form'
import { isValidHttpsUrl } from '../../utils/validation'

// Los toplines que subió el usuario a beats ajenos. Puede corregir el link
// (se edita en la misma fila, sin cambiar de pantalla, porque es un cambio
// de un solo campo) o borrarlo.
function MyToplinesSection({ myToplines, beatsById }) {
  const { setToplines } = useApp()
  // Id del topline que se está editando: solo uno a la vez.
  const [editingId, setEditingId] = useState(null)
  const [draftUrl, setDraftUrl] = useState('')
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  // Mensaje de confirmación de la última acción. Se muestra con <Alert>, que
  // tiene role="status": el lector de pantalla lo lee solo.
  const [message, setMessage] = useState('')

  function startEditing(topline) {
    setEditingId(topline.id)
    setDraftUrl(topline.audioUrl)
    setErrors({})
    setMessage('')
  }

  function cancelEditing() {
    setEditingId(null)
    setErrors({})
  }

  async function save(event, toplineId) {
    event.preventDefault()
    if (saving) return

    const found = {}
    if (!isValidHttpsUrl(draftUrl)) {
      found['topline-url'] = 'El link tiene que empezar con https:// (por ejemplo, de SoundCloud).'
    }
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }

    setSaving(true)
    await fakeRequest(600)
    setToplines(current =>
      current.map(topline =>
        topline.id === toplineId ? { ...topline, audioUrl: draftUrl.trim() } : topline
      )
    )
    setSaving(false)
    setEditingId(null)
    setMessage('El link se actualizó.')
  }

  function remove(toplineId, beatTitle) {
    setToplines(current => current.filter(topline => topline.id !== toplineId))
    setMessage(`Borraste tu topline para "${beatTitle}".`)
  }

  return (
    <section className="section" aria-labelledby="my-toplines-title">
      <div className="container">
        <div className="section-head">
          <h2 id="my-toplines-title">Mis toplines</h2>
          <p>{plural(myToplines.length, 'topline')} subido{myToplines.length === 1 ? '' : 's'} por vos</p>
        </div>

        {message && <Alert kind="success">{message}</Alert>}

        {myToplines.length === 0 ? (
          <p>
            Todavía no subiste ninguno. Entrá a <Link to="/beats">un beat</Link> y subí tu topline.
          </p>
        ) : (
          <ul className="item-list">
            {myToplines.map(topline => {
              const beat = beatsById[topline.beatId]
              const beatTitle = beat ? beat.title : 'Beat borrado'

              return (
                <li key={topline.id} className="my-topline">
                  <div className="topline-row">
                    <div>
                      <h3>
                        {beat ? <Link to={`/beats/${beat.id}`}>{beatTitle}</Link> : beatTitle}
                      </h3>
                      <p className="beat-meta">
                        <time dateTime={topline.createdAt}>{formatDate(topline.createdAt)}</time>
                      </p>
                    </div>

                    <div className="topline-actions">
                      <StatusBadge status={topline.status} />
                      <Button variant="secondary" onClick={() => startEditing(topline)}>
                        Editar link
                        <span className="visually-hidden"> de tu topline para {beatTitle}</span>
                      </Button>
                      <ConfirmDelete
                        label={`tu topline para "${beatTitle}"`}
                        buttonText="Borrar topline"
                        onConfirm={() => remove(topline.id, beatTitle)}
                      />
                    </div>
                  </div>

                  {editingId === topline.id && (
                    <form
                      className="inline-edit"
                      onSubmit={event => save(event, topline.id)}
                      noValidate
                      aria-label={`Editar el link del topline para ${beatTitle}`}
                    >
                      <FormField
                        id="topline-url"
                        label="Link al audio"
                        type="url"
                        value={draftUrl}
                        onChange={setDraftUrl}
                        error={errors['topline-url']}
                        placeholder="https://…"
                        required
                      />
                      <div className="inline-edit-actions">
                        <Button type="submit" disabled={saving}>
                          {saving ? 'Enviando…' : 'Guardar'}
                        </Button>
                        <Button variant="secondary" onClick={cancelEditing}>
                          Cancelar
                        </Button>
                      </div>
                    </form>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </section>
  )
}

export default MyToplinesSection
