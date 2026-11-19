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
