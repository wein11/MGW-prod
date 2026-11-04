import { JURY_WEIGHTS } from '../../data/mockData'
import './JuryBand.css'

const SOURCES = [
  { label: 'Comunidad', value: JURY_WEIGHTS.community },
  { label: 'Verificados', value: JURY_WEIGHTS.verified },
  { label: 'Invitado', value: JURY_WEIGHTS.guest },
]

// Franja que explica cómo se reparte el puntaje de un desafío (30/30/40).
// La usan la Landing y Desafíos; cada una pasa su propia nota como children.
// Se usa <dl>: cada fuente de voto (término) tiene su porcentaje (definición).
function JuryBand({ children }) {
  return (
    <section className="jury" aria-labelledby="jury-title">
      <div className="container jury-inner">
        <h2 id="jury-title" className="label-caps jury-title">Jurado</h2>
        <dl className="jury-weights">
          {SOURCES.map(source => (
            <div key={source.label}>
              <dt className="label-caps">{source.label}</dt>
              <dd>{source.value}%</dd>
            </div>
          ))}
        </dl>
        <p className="jury-note">{children}</p>
      </div>
    </section>
  )
}

export default JuryBand
