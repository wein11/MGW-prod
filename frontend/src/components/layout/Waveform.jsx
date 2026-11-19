// ============================================================================
// PARA ESTUDIAR: Waveform.jsx (onda decorativa)
// ----------------------------------------------------------------------------
// QUÉ ES: la franja de barritas rojo → naranja que parece una onda de audio.
//   Es la identidad visual de MGW Prod (plataforma musical).
// CÓMO: Array.from crea 120 barras; cada una tiene una altura calculada con
//   Math.sin (para que parezca una onda) y un porcentaje de mezcla de color.
//   Se calcula UNA vez afuera del componente, así no cambia en cada render.
// style={{ ... }}: estilos en línea en React. Va un objeto JS (doble llave:
//   la primera es "acá va JavaScript", la segunda es el objeto).
// aria-hidden="true": es decoración, el lector de pantalla la ignora.
// ============================================================================
import './Waveform.css'

// Onda decorativa del diseño (franja de barras rojo -> naranja).
// Es solo decoración: aria-hidden para que el lector de pantalla la ignore.
const BAR_COUNT = 120

// Alturas fijas calculadas con senos para que la onda no cambie en cada render.
const bars = Array.from({ length: BAR_COUNT }, (_, i) => {
  const height = 25 + Math.abs(Math.sin(i * 0.9) * 45 + Math.sin(i * 0.23) * 30)
  const mix = Math.round((i / (BAR_COUNT - 1)) * 100)
  return { height: Math.min(height, 100), mix }
})

function Waveform() {
  return (
    <div className="waveform" aria-hidden="true">
      {bars.map((bar, i) => (
        <span
          key={i}
          style={{
            height: `${bar.height}%`,
            background: `color-mix(in srgb, var(--accent) ${bar.mix}%, var(--primary))`,
          }}
        />
      ))}
    </div>
  )
}

export default Waveform
