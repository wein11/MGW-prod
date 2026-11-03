import { useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import FormField from '../../components/ui/FormField'
import { useAuth } from '../../context/AuthContext'
import { useData } from '../../context/DataContext'
import { fakeRequest } from '../../data/fakeRequest'
import { formatDate, plural, todayIso } from '../../utils/format'
import { focusFirstError, hasErrors } from '../../utils/form'
import { nextId } from '../../utils/ids'

const MAX_LENGTH = 500

// Comentarios de un beat: la lista y el formulario para sumar uno.
// Comenta cualquier usuario con sesión (artista o discográfica).
function CommentsSection({ beatId }) {
  const { user } = useAuth()
  const { comments, setComments } = useData()
  const [text, setText] = useState('')
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [sent, setSent] = useState(false)

  const beatComments = comments.filter(comment => comment.beatId === beatId)

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    setSent(false)
    const length = text.trim().length
    const found = {}
    if (length === 0) {
      found.comment = 'Escribí tu comentario antes de enviarlo.'
    } else if (length > MAX_LENGTH) {
      found.comment = `El comentario puede tener hasta ${MAX_LENGTH} caracteres (tiene ${length}).`
    }
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    const newComment = {
      id: nextId(comments),
      beatId,
      authorName: user.name,
      text: text.trim(),
      createdAt: todayIso(),
    }
    // Va al final porque la lista está en orden cronológico.
    setComments(current => [...current, newComment])
    setSending(false)
    setSent(true)
    setText('')
  }

  return (
    <section className="section" aria-labelledby="comments-title">
      <div className="container">
        <div className="section-head">
          <h2 id="comments-title">Comentarios</h2>
          <p>{plural(beatComments.length, 'comentario')}</p>
        </div>

        {beatComments.length === 0 ? (
          <p>Todavía nadie comentó este beat.</p>
        ) : (
          <ul className="item-list">
            {beatComments.map(comment => (
              <li key={comment.id} className="comment">
                <p className="comment-head">
                  <strong>{comment.authorName}</strong>
                  <time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>
                </p>
                <p>{comment.text}</p>
              </li>
            ))}
          </ul>
        )}

        {user ? (
          <form className="narrow-form" onSubmit={handleSubmit} noValidate>
            <FormField
              id="comment"
              label="Tu comentario"
              type="textarea"
              value={text}
              onChange={setText}
              error={errors.comment}
              hint={`Hasta ${MAX_LENGTH} caracteres.`}
              placeholder="Contale al productor qué te pareció"
              required
            />
            <Button type="submit" disabled={sending}>
              {sending ? 'Enviando…' : 'Comentar'}
            </Button>
            {sent && <Alert kind="success">Tu comentario se publicó.</Alert>}
          </form>
        ) : (
          <p>
            <Link to="/login">Entrá</Link> para dejar un comentario.
          </p>
        )}
      </div>
    </section>
  )
}

export default CommentsSection
