// ============================================================================
// PARA ESTUDIAR: FormField.jsx (campo de formulario accesible)
// ----------------------------------------------------------------------------
// QUÉ ES: un componente que dibuja label + input (o textarea/select) + texto
//   de ayuda + mensaje de error. Lo usan todos los formularios.
// ES UN "COMPONENTE CONTROLADO": el valor lo guarda la página padre con
//   useState y lo pasa por `value`; cuando el usuario escribe, FormField
//   llama a `onChange(nuevoTexto)` y la página actualiza su estado.
//   React es la "única fuente de verdad" del valor del campo.
// ACCESIBILIDAD (criterio 3 y 7):
//   - <label htmlFor={id}> + <input id={id}>: quedan unidos. Al hacer clic en
//     el texto se enfoca el campo y el lector de pantalla lee el nombre.
//   - aria-invalid="true": avisa que el campo tiene un error.
//   - aria-describedby="email-hint email-error": el lector lee también la
//     ayuda y el error cuando el usuario entra al campo.
//   - El asterisco de obligatorio tiene aria-hidden: se ve pero no se lee
//     (ya lo comunica el atributo `required`).
// PREGUNTA PROBABLE: "¿Cómo se entera la persona ciega de que hay un error?"
//   → focusFirstError le mueve el foco al campo y el lector lee label +
//     aria-invalid + el texto del error vinculado con aria-describedby.
// ============================================================================
import './FormField.css'

// Campo de formulario con su label, ayuda y error.
// - label + htmlFor: el lector de pantalla anuncia el nombre del campo.
// - aria-invalid y aria-describedby: anuncian el error y la ayuda.
// - El id tiene que coincidir con la clave del error para que focusFirstError funcione.
// type: 'text' | 'email' | 'password' | 'number' | 'url' | 'textarea' | 'select'
function FormField({ id, label, type = 'text', value, onChange, error, hint, options = [], required, ...rest }) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  // Junta los ids que existan: "email-hint email-error", o undefined si no hay
  // ninguno (así React no dibuja el atributo vacío).
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

  // Props compartidas por input, textarea y select, para no repetirlas 3 veces.
  const common = {
    id,
    name: id,
    value,
    onChange: event => onChange(event.target.value),
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
    required,
    ...rest,
  }

  let control
  if (type === 'textarea') {
    control = <textarea rows={4} {...common} />
  } else if (type === 'select') {
    control = (
      <select {...common}>
        {options.map(option => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    )
  } else {
    control = <input type={type} {...common} />
  }

  return (
    <div className={error ? 'field field-error' : 'field'}>
      <label htmlFor={id} className="label-caps">
        {label}
        {required && <span aria-hidden="true"> *</span>}
      </label>
      {control}
      {hint && <p id={hintId} className="field-hint">{hint}</p>}
      {error && <p id={errorId} className="field-message">{error}</p>}
    </div>
  )
}

export default FormField
