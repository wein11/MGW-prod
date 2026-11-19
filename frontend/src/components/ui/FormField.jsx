import './FormField.css'

// Campo de formulario con su label, ayuda y error.
// - label + htmlFor: el lector de pantalla anuncia el nombre del campo.
// - aria-invalid y aria-describedby: anuncian el error y la ayuda.
// - El id tiene que coincidir con la clave del error para que focusFirstError funcione.
// type: 'text' | 'email' | 'password' | 'number' | 'url' | 'textarea' | 'select'
function FormField({ id, label, type = 'text', value, onChange, error, hint, options = [], required, ...rest }) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

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
