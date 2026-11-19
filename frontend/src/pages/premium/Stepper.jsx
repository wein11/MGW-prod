// ============================================================================
// PARA ESTUDIAR: Stepper.jsx (indicador de pasos de Premium)
// ----------------------------------------------------------------------------
// QUÉ ES: "1 Plan · 2 Pago · 3 Listo" arriba del flujo de Premium.
// PROP current: el paso actual (1, 2 o 3).
// Cada paso puede estar completado (✓), actual o pendiente: se calcula
//   comparando su número con current.
// ACCESIBILIDAD: <ol> porque los pasos tienen orden; el paso actual lleva
//   aria-current="step" (el lector dice "paso actual"); los completados tienen
//   un texto oculto "(completado)".
// ============================================================================
// Indicador de los 3 pasos de Premium. Es una lista ordenada (<ol>) porque
// los pasos tienen un orden; el paso actual lleva aria-current="step" para
// que el lector de pantalla lo anuncie. Reemplaza al texto "Paso N de 3" que
// iba arriba del título: el stepper ya dice en qué paso estás.
const STEPS = ['Plan', 'Pago', 'Listo']

function Stepper({ current }) {
  return (
    <div className="container">
      <ol className="stepper" aria-label="Pasos para pasar a Premium">
        {STEPS.map((label, index) => {
          const number = index + 1
          const state = number < current ? 'done' : number === current ? 'current' : 'todo'
          return (
            <li
              key={label}
              className={`stepper-item stepper-${state}`}
              aria-current={state === 'current' ? 'step' : undefined}
            >
              <span aria-hidden="true">{state === 'done' ? '✓' : number}</span> {label}
              {state === 'done' && <span className="visually-hidden"> (completado)</span>}
            </li>
          )
        })}
      </ol>
    </div>
  )
}

export default Stepper
