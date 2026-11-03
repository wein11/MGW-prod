import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import ConfirmDelete from '../../components/ui/ConfirmDelete'
import FormField from '../../components/ui/FormField'
import { useData } from '../../context/DataContext'
import { fakeRequest } from '../../data/fakeRequest'
import { GENRES } from '../../data/mockData'
import { focusFirstError, hasErrors } from '../../utils/form'
import { isRequired, isValidBpm } from '../../utils/validation'
import { BPM_ERROR } from './PublishBeatForm'

const GENRE_OPTIONS = GENRES.map(genre => ({ value: genre, label: genre }))

// "Gestionar mi beat": editar y borrar. Solo se dibuja para el dueño (lo
// decide BeatDetailPage). Los id de los campos llevan el prefijo "edit-"
// para no chocar con otros formularios de la misma página.
function ManageBeat({ beat }) {
  const { setBeats, setComments, setToplines } = useData()
  const navigate = useNavigate()
  // El formulario arranca con los datos actuales del beat.
  const [form, setForm] = useState({
    'edit-title': beat.title,
    'edit-genre': beat.genre,
    'edit-bpm': String(beat.bpm),
    'edit-key': beat.key,
  })
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [saved, setSaved] = useState(false)

  function setField(key) {
    return value => setForm(current => ({ ...current, [key]: value }))
  }

  function validate() {
    const found = {}
    if (!isRequired(form['edit-title'])) found['edit-title'] = 'El título no puede quedar vacío.'
    if (!isValidBpm(form['edit-bpm'])) found['edit-bpm'] = BPM_ERROR
    if (!isRequired(form['edit-key'])) found['edit-key'] = 'Ingresá la tonalidad (por ejemplo, Dm).'
    return found
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    setSaved(false)
    const found = validate()
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    const changes = {
      title: form['edit-title'].trim(),
      genre: form['edit-genre'],
      bpm: Number(form['edit-bpm']),
      key: form['edit-key'].trim(),
    }
    // map devuelve una lista nueva: React detecta el cambio y vuelve a dibujar.
    setBeats(current => current.map(item => (item.id === beat.id ? { ...item, ...changes } : item)))
    setSending(false)
    setSaved(true)
  }

  // Al borrar el beat también se borran sus comentarios y toplines, para que
  // no queden datos sueltos apuntando a un beat que ya no existe.
  function handleDelete() {
    setBeats(current => current.filter(item => item.id !== beat.id))
    setComments(current => current.filter(comment => comment.beatId !== beat.id))
    setToplines(current => current.filter(topline => topline.beatId !== beat.id))
    navigate('/beats', { state: { deletedTitle: beat.title } })
  }

  return (
    <section className="section" aria-labelledby="manage-title">
      <div className="container">
        <div className="section-head">
          <h2 id="manage-title">Gestionar mi beat</h2>
          <p className="tag">Solo para el dueño</p>
        </div>

        <div className="manage-grid">
          <form onSubmit={handleSubmit} noValidate aria-labelledby="edit-heading">
            <h3 id="edit-heading">Editar datos</h3>
            <div className="form-grid">
              <FormField
                id="edit-title"
                label="Título"
                value={form['edit-title']}
                onChange={setField('edit-title')}
                error={errors['edit-title']}
                required
              />
              <FormField
                id="edit-genre"
                label="Género"
                type="select"
                value={form['edit-genre']}
                onChange={setField('edit-genre')}
                options={GENRE_OPTIONS}
              />
              <FormField
                id="edit-bpm"
                label="BPM"
                type="number"
                inputMode="numeric"
                value={form['edit-bpm']}
                onChange={setField('edit-bpm')}
                error={errors['edit-bpm']}
                required
              />
              <FormField
                id="edit-key"
                label="Tonalidad"
                value={form['edit-key']}
                onChange={setField('edit-key')}
                error={errors['edit-key']}
                required
              />
            </div>
            <Button type="submit" disabled={sending}>
              {sending ? 'Enviando…' : 'Guardar cambios'}
            </Button>
            {saved && <Alert kind="success">Los cambios del beat se guardaron.</Alert>}
          </form>

          <div className="manage-delete">
            <h3>Borrar beat</h3>
            <p>Al hacer clic se pide una confirmación antes de borrar.</p>
            <ConfirmDelete label={`"${beat.title}"`} buttonText="Borrar beat" onConfirm={handleDelete} />
          </div>
        </div>
      </div>
    </section>
  )
}

export default ManageBeat
