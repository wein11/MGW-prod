// ============================================================================
// PARA ESTUDIAR: JuryBand.jsx (cómo se reparte el voto de un desafío)
// ----------------------------------------------------------------------------
// QUÉ ES: la franja "Jurado: Comunidad 30% · Verificados 30% · Invitado 40%".
//   Explica la regla de negocio central de los desafíos (criterio 1: la
//   interfaz representa el negocio).
// REUTILIZACIÓN: la usan la Landing y Desafíos. Cada una pasa su texto como
//   children, así el componente es el mismo pero la nota cambia.
// Los porcentajes vienen de JURY_WEIGHTS en mockData: si cambia la regla, se
//   cambia en un solo lugar.
// ============================================================================
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
