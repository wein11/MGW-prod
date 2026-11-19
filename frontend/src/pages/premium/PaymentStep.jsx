import { useState } from 'react'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import FormField from '../../components/ui/FormField'
import PrivacyConsent from '../../components/ui/PrivacyConsent'
import { fakeRequest } from '../../data/fakeRequest'
import { focusFirstError, hasErrors } from '../../utils/form'
import { formatPrice } from '../../utils/format'
import {
  isRequired,
  isValidCardNumber,
  isValidCvv,
  isValidExpiry,
  maskCard,
} from '../../utils/validation'
import { plans } from '../../data/mockData'

const PREMIUM = plans.find(plan => plan.id === 'PREMIUM')

// Ayudas de tipeo: dejan solo dígitos y arman el formato mientras se escribe,
// así la persona no tiene que poner espacios ni la barra a mano.
function formatCardNumber(value) {
  return value.replace(/\D/g, '').slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ')
}

function formatExpiry(value) {
  const digits = value.replace(/\D/g, '').slice(0, 4)
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits
}

// Paso 2: datos de la tarjeta. IMPORTANTE (datos sensibles): el número, el
// vencimiento y el CVV viven solo en el estado de este componente. Nunca se
// escriben en localStorage/sessionStorage ni en la consola, y al pasar al
// paso 3 el componente se desmonta, así que React los descarta. Hacia afuera
// solo sale el número enmascarado ("•••• 1111").
function PaymentStep({ onBack, onPaid }) {
  const [form, setForm] = useState({ cardName: '', cardNumber: '', cardExpiry: '', cardCvv: '' })
  const [accepted, setAccepted] = useState(false)
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)

  function setField(key, format = value => value) {
    return value => setForm(current => ({ ...current, [key]: format(value) }))
  }

  // Las claves se agregan en el orden de los campos en pantalla: así
  // focusFirstError lleva el foco al primero que falló.
  function validate() {
    const found = {}
    if (!isRequired(form.cardName)) {
      found.cardName = 'Ingresá el nombre como figura en la tarjeta.'
    }
    if (!isRequired(form.cardNumber)) {
      found.cardNumber = 'Ingresá el número de tarjeta.'
    } else if (!isValidCardNumber(form.cardNumber)) {
      found.cardNumber = 'El número de tarjeta no es válido. Revisalo.'
    }
    if (!isRequired(form.cardExpiry)) {
      found.cardExpiry = 'Ingresá el vencimiento (MM/AA).'
    } else if (!/^\d{2}\/\d{2}$/.test(form.cardExpiry)) {
      found.cardExpiry = 'Usá el formato MM/AA, por ejemplo 10/28.'
    } else if (!isValidExpiry(form.cardExpiry)) {
      found.cardExpiry = 'La tarjeta está vencida o el mes no es válido.'
    }
    if (!isRequired(form.cardCvv)) {
      found.cardCvv = 'Ingresá el código de seguridad.'
    } else if (!isValidCvv(form.cardCvv)) {
      found.cardCvv = 'El CVV tiene 3 o 4 dígitos.'
    }
    if (!accepted) {
      found.privacy = 'Tenés que aceptar la Política de privacidad para pagar.'
    }
    return found
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    const found = validate()
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    onPaid(maskCard(form.cardNumber))
  }

  return (
    <section className="section" aria-label="Pago">
      <div className="container payment">
        <form className="payment-form" onSubmit={handleSubmit} noValidate aria-label="Datos de la tarjeta">
          {hasErrors(errors) && (
            <Alert kind="error">Revisá los campos marcados antes de pagar.</Alert>
          )}
          <FormField
            id="cardName"
            label="Nombre en la tarjeta"
            value={form.cardName}
            onChange={setField('cardName')}
            error={errors.cardName}
            placeholder="Como figura en la tarjeta"
            autoComplete="cc-name"
            required
          />
          <FormField
            id="cardNumber"
            label="Número de tarjeta"
            value={form.cardNumber}
            onChange={setField('cardNumber', formatCardNumber)}
            error={errors.cardNumber}
            placeholder="0000 0000 0000 0000"
            autoComplete="cc-number"
            inputMode="numeric"
            required
          />
          <div className="field-row">
            <FormField
              id="cardExpiry"
              label="Vencimiento (MM/AA)"
              value={form.cardExpiry}
              onChange={setField('cardExpiry', formatExpiry)}
              error={errors.cardExpiry}
              placeholder="MM/AA"
              autoComplete="cc-exp"
              inputMode="numeric"
              required
            />
            <FormField
              id="cardCvv"
              label="CVV"
              type="password"
              value={form.cardCvv}
              onChange={setField('cardCvv', value => value.replace(/\D/g, '').slice(0, 4))}
              error={errors.cardCvv}
              placeholder="123"
              autoComplete="cc-csc"
              inputMode="numeric"
              required
            />
          </div>
          <PrivacyConsent checked={accepted} onChange={setAccepted} error={errors.privacy} />
          <p className="field-hint">No guardamos ningún dato de tu tarjeta.</p>
          <div className="form-actions">
            <Button type="submit" disabled={sending}>
              {sending ? 'Enviando…' : `Pagar ${formatPrice(PREMIUM.price)}`}
            </Button>
            <Button variant="secondary" onClick={onBack} disabled={sending}>Volver</Button>
          </div>
        </form>

        <aside className="summary" aria-labelledby="summary-title">
          <h2 id="summary-title">Resumen</h2>
          <dl>
            <div><dt>Plan Premium</dt><dd>{formatPrice(PREMIUM.price)} / mes</dd></div>
            <div><dt>Facturación</dt><dd>mensual</dd></div>
          </dl>
          <p className="summary-total">
            <span>Total</span> <strong>{formatPrice(PREMIUM.price)}</strong>
          </p>
        </aside>
      </div>
    </section>
  )
}

export default PaymentStep
