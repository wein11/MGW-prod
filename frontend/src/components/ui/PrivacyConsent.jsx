// ============================================================================
// PARA ESTUDIAR: PrivacyConsent.jsx (casilla "Acepto la Política de privacidad")
// ----------------------------------------------------------------------------
// QUÉ ES: un checkbox obligatorio que se repite en Registro, Contacto y Pago.
//   Por eso es un componente: se escribe una vez y se usa tres veces.
// CHECKBOX CONTROLADO: `checked` viene del useState de la página y onChange
//   devuelve event.target.checked (true/false), no un texto.
// El link abre la política en otra pestaña (target="_blank") para no perder
//   lo que la persona ya escribió en el formulario. rel="noopener" es por
//   seguridad: la pestaña nueva no puede controlar la nuestra.
// ============================================================================
import { Link } from 'react-router-dom'
import './PrivacyConsent.css'

// Checkbox obligatorio de aceptación de la Política de privacidad. Va en
// Registro, Contacto y Pago, porque son los formularios que piden datos
// personales. El id es "privacy" para que focusFirstError lo encuentre.
function PrivacyConsent({ checked, onChange, error }) {
  return (
    <div className={error ? 'privacy-consent field-error' : 'privacy-consent'}>
      <input
        type="checkbox"
        id="privacy"
        name="privacy"
        checked={checked}
        onChange={event => onChange(event.target.checked)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? 'privacy-error' : undefined}
        required
      />
      <label htmlFor="privacy">
        Leí y acepto la{' '}
        <Link to="/privacidad" target="_blank" rel="noopener">
          Política de privacidad
        </Link>
      </label>
      {error && <p id="privacy-error" className="field-message">{error}</p>}
    </div>
  )
}

export default PrivacyConsent
