import { useState } from 'react'
import Button from '../../components/ui/Button'
import FormField from '../../components/ui/FormField'
import { fakeRequest } from '../../data/fakeRequest'
import { focusFirstError, hasErrors } from '../../utils/form'

const SCORES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const MAX_COMMENT = 500

// Formulario de voto. El puntaje es un grupo de radios dentro de un fieldset
// con legend, no una fila de divs con click: así el lector de pantalla anuncia
// "Puntaje de 1 a 10, 8 de 10", se recorre con las flechas y un solo Tab lo
// atraviesa entero. El aspecto de botón es solo CSS sobre el label.
//
// El mensaje de éxito NO vive acá: lo muestra la página. Si al votar la última
// entrega disponible este formulario se desmonta, el aviso se iría con él y
// quien votó no vería nunca la confirmación.
function VoteForm({ options, selected, onSelect, onVote }) {
  const [score, setScore] = useState('')
  const [comment, setComment] = useState('')
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return

    const found = {}
    if (!score) {
      found['vote-score'] = 'Elegí un puntaje de 1 a 10 para enviar el voto.'
    }
    if (comment.length > MAX_COMMENT) {
      found['vote-comment'] = `El comentario no puede pasar de ${MAX_COMMENT} caracteres.`
    }
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }

    setSending(true)
    await fakeRequest(600)
    setSending(false)
    setScore('')
    setComment('')
    onVote(selected)
  }

  return (
    <form className="vote-form" onSubmit={handleSubmit} noValidate aria-label="Votar una entrega">
      <FormField
        id="vote-submission"
        label="Entrega"
        type="select"
        value={selected}
        onChange={value => onSelect(Number(value))}
        options={options}
      />

      {/* El fieldset agrupa los 10 radios bajo un mismo nombre accesible. */}
      <fieldset
        className={errors['vote-score'] ? 'score-group score-group-error' : 'score-group'}
        aria-invalid={errors['vote-score'] ? true : undefined}
        aria-describedby={errors['vote-score'] ? 'vote-score-error' : undefined}
      >
        <legend className="label-caps">Puntaje de 1 a 10</legend>
        <div className="score-options">
          {SCORES.map(value => (
            <label key={value} className="score-option">
              <input
                type="radio"
                name="vote-score"
                // El primero lleva el id del error: focusFirstError manda el
                // foco acá y, al ser un grupo de radios, entra en el grupo.
                id={value === 1 ? 'vote-score' : `vote-score-${value}`}
                value={value}
                checked={score === String(value)}
                onChange={event => setScore(event.target.value)}
              />
              <span>{value}</span>
            </label>
          ))}
        </div>
        {errors['vote-score'] && (
          <p id="vote-score-error" className="field-message">{errors['vote-score']}</p>
        )}
      </fieldset>

      <FormField
        id="vote-comment"
        label="Comentario"
        type="textarea"
        value={comment}
        onChange={setComment}
        error={errors['vote-comment']}
        hint="Opcional. Contale al productor qué funcionó y qué mejorarías."
        maxLength={MAX_COMMENT}
      />

      <Button type="submit" disabled={sending}>
        {sending ? 'Enviando…' : 'Enviar voto'}
      </Button>
    </form>
  )
}

export default VoteForm
