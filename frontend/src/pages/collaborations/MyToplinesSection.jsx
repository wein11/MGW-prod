import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import FormField from '../../components/ui/FormField'
import StatusBadge from '../../components/ui/StatusBadge'
import { useData } from '../../context/DataContext'
import { fakeRequest } from '../../data/fakeRequest'
import { formatDate, plural } from '../../utils/format'
import { focusFirstError, hasErrors } from '../../utils/form'
import { isValidHttpsUrl } from '../../utils/validation'

// Los toplines que subió el usuario a beats ajenos. Puede corregir el link
// (se edita en la misma fila, sin cambiar de pantalla, porque es un cambio
// de un solo campo) o borrarlo.
function MyToplinesSection({ myToplines, beatsById }) {
  const { setToplines } = useData()
  // Id del topline que se está editando: solo uno a la vez.
  const [editingId, setEditingId] = useState(null)
  const [draftUrl, setDraftUrl] = useState('')
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [savedId, setSavedId] = useState(null)
  // Igual que en ProposalsSection: el destino del foco va en un ref porque no
  // se dibuja, y el efecto lo consume después del render.
  const pendingFocus = useRef(null)

  useEffect(() => {
    const id = pendingFocus.current
    if (!id) return
    pendingFocus.current = null
    // Si el elemento ya no está (se borró la fila), el foco va al título.
    const target = document.getElementById(id) ?? document.getElementById('my-toplines-title')
    target?.focus()
  })

  function startEditing(topline) {
    setEditingId(topline.id)
    setDraftUrl(topline.audioUrl)
    setErrors({})
    setSavedId(null)
  }

  function cancelEditing() {
    const id = editingId
    setEditingId(null)
    setErrors({})
    // El formulario desaparece: el foco vuelve al botón que lo abrió.
    pendingFocus.current = `edit-${id}`
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
    setSavedId(toplineId)
    pendingFocus.current = `edit-${toplineId}`
  }

  function remove(toplineId) {
    setToplines(current => current.filter(topline => topline.id !== toplineId))
    pendingFocus.current = 'my-toplines-title'
  }

  return (
    <section className="section" aria-labelledby="my-toplines-title">
      <div className="container">
        <div className="section-head">
          <h2 id="my-toplines-title" tabIndex={-1}>Mis toplines</h2>
          <p>{plural(myToplines.length, 'topline')} subido{myToplines.length === 1 ? '' : 's'} por vos</p>
        </div>

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
                      <Button
                        variant="secondary"
                        id={`edit-${topline.id}`}
                        onClick={() => startEditing(topline)}
                      >
                        Editar link
                        <span className="visually-hidden"> de tu topline para {beatTitle}</span>
                      </Button>
                      <ConfirmDelete
                        label={`tu topline para "${beatTitle}"`}
                        buttonText="Borrar topline"
                        onConfirm={() => remove(topline.id)}
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

                  {savedId === topline.id && <Alert kind="success">Listo: el link se actualizó.</Alert>}
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
