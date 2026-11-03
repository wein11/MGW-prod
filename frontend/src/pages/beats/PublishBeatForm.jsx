import { useState } from 'react'
import coverDefault from '../../assets/covers/cover-1.svg'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import FormField from '../../components/ui/FormField'
import { useAuth } from '../../context/AuthContext'
import { useData } from '../../context/DataContext'
import { fakeRequest } from '../../data/fakeRequest'
import { GENRES } from '../../data/mockData'
import { todayIso } from '../../utils/format'
import { focusFirstError, hasErrors } from '../../utils/form'
import { nextId } from '../../utils/ids'
import { isRequired, isValidBpm, isValidHttpsUrl } from '../../utils/validation'

const GENRE_OPTIONS = GENRES.map(genre => ({ value: genre, label: genre }))

const EMPTY_BEAT = { title: '', genre: GENRES[0], audioUrl: '', bpm: '', key: '' }

// Mensaje exacto del wireframe: lo pide la consigna del equipo.
export const BPM_ERROR = 'El BPM tiene que estar entre 40 y 220. Revisalo y volvé a intentar.'

// Formulario "Publicar beat". Solo un ARTIST puede publicar: a un visitante
// se le pide entrar y a una discográfica se le explica por qué no puede.
// Las claves de "errors" son iguales a los id de los campos, así
// focusFirstError lleva el foco al primer campo con problema.
function PublishBeatForm() {
  const { user } = useAuth()
  const { beats, setBeats } = useData()
  const [form, setForm] = useState(EMPTY_BEAT)
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [published, setPublished] = useState('')

  if (!user) {
    return (
      <EmptyState title="Publicar beat">
        <p>Para publicar un beat tenés que entrar con tu cuenta de artista.</p>
        <Button to="/login">Entrar</Button>
      </EmptyState>
    )
  }

  if (user.role !== 'ARTIST') {
    return (
      <EmptyState title="Publicar beat">
        <p>Solo las cuentas de artista pueden publicar beats. Desde una discográfica podés explorarlos y comentarlos.</p>
      </EmptyState>
    )
  }

  // Devuelve una función que actualiza un solo campo del formulario.
  function setField(key) {
    return value => setForm(current => ({ ...current, [key]: value }))
  }

  function validate() {
    const found = {}
    if (!isRequired(form.title)) {
      found.title = 'Ingresá el título del beat.'
    }
    if (!isValidHttpsUrl(form.audioUrl)) {
      found.audioUrl = 'El link tiene que empezar con https:// (por ejemplo, de SoundCloud).'
    }
    if (!isValidBpm(form.bpm)) {
      found.bpm = BPM_ERROR
    }
    if (!isRequired(form.key)) {
      found.key = 'Ingresá la tonalidad (por ejemplo, Dm).'
    }
    return found
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    setPublished('')
    const found = validate()
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    const newBeat = {
      id: nextId(beats),
      title: form.title.trim(),
      genre: form.genre,
      bpm: Number(form.bpm),
      key: form.key.trim(),
      audioUrl: form.audioUrl.trim(),
      cover: coverDefault,
      ownerName: user.name,
      publishedAt: todayIso(),
      description: '',
    }
    // Se agrega al principio para que aparezca primero en la lista.
    setBeats(current => [newBeat, ...current])
    setSending(false)
    setPublished(newBeat.title)
    setForm(EMPTY_BEAT)
  }

  return (
    <section aria-labelledby="publish-title">
      <div className="section-head">
        <h2 id="publish-title">Publicar beat</h2>
        <p className="tag">Visible solo para artistas</p>
      </div>
      <form className="publish-form" onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          <FormField
            id="title"
            label="Título"
            value={form.title}
            onChange={setField('title')}
            error={errors.title}
            placeholder="Ej: Noche en Palermo"
            required
          />
          <FormField
            id="genre"
            label="Género"
            type="select"
            value={form.genre}
            onChange={setField('genre')}
            options={GENRE_OPTIONS}
          />
        </div>
        <FormField
          id="audioUrl"
          label="Link al audio"
          type="url"
          value={form.audioUrl}
          onChange={setField('audioUrl')}
          error={errors.audioUrl}
          placeholder="https://soundcloud.com/tu-usuario/tu-beat"
          required
        />
        <div className="form-grid">
          <FormField
            id="bpm"
            label="BPM"
            type="number"
            inputMode="numeric"
            value={form.bpm}
            onChange={setField('bpm')}
            error={errors.bpm}
            hint="Entre 40 y 220."
            placeholder="Ej: 140"
            required
          />
          <FormField
            id="key"
            label="Tonalidad"
            value={form.key}
            onChange={setField('key')}
            error={errors.key}
            placeholder="Dm"
            required
          />
        </div>
        <Button type="submit" disabled={sending}>
          {sending ? 'Enviando…' : 'Publicar beat'}
        </Button>
      </form>
      {published && (
        <Alert kind="success">
          Tu beat &quot;{published}&quot; ya está publicado y aparece primero en la lista.
        </Alert>
      )}
    </section>
  )
}

export default PublishBeatForm
