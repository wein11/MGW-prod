// ============================================================================
// PARA ESTUDIAR: ContactPage.jsx (ruta "/contacto")
// ----------------------------------------------------------------------------
// QUÉ ES: formulario de contacto simulado (no se envía a ningún lado).
// ESTADOS DEL FORMULARIO (criterio 7: error, espera y éxito):
//   - errors: mensajes por campo + resumen arriba.
//   - sending: mientras "envía" el botón dice "Enviando…" y está deshabilitado
//     (evita doble envío).
//   - sent: cuando termina, el formulario se reemplaza por el Alert de éxito y
//     un botón "Enviar otro" que vuelve todo a cero (sendAnother).
// VALIDACIONES: nombre, email válido, mensaje de 10+ caracteres y aceptar la
//   política de privacidad (PrivacyConsent).
// El select de motivo usa REASONS (array de opciones).
// ============================================================================
import { useState } from 'react'
import PageHeader from '../components/layout/PageHeader'
import Alert from '../components/ui/Alert'
import Button from '../components/ui/Button'
import FormField from '../components/ui/FormField'
import PrivacyConsent from '../components/ui/PrivacyConsent'
import { fakeRequest } from '../data/fakeRequest'
import { focusFirstError, hasErrors } from '../utils/form'
import { isRequired, isValidEmail } from '../utils/validation'
import './ContactPage.css'

const REASONS = [
  { value: 'consulta', label: 'Consulta' },
  { value: 'sugerencia', label: 'Sugerencia' },
  { value: 'reclamo', label: 'Reclamo' },
]

const EMPTY_FORM = { name: '', email: '', reason: 'consulta', message: '' }

// Formulario de contacto simulado: no se envía a ningún lado. Al enviar, el
// formulario se reemplaza por el mensaje de éxito (con "Enviar otro"). El
// mensaje usa <Alert>, que tiene role="status": el lector de pantalla lo lee solo.
function ContactPage() {
  const [form, setForm] = useState(EMPTY_FORM)
  const [accepted, setAccepted] = useState(false)
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)
  const [sent, setSent] = useState(false)

  function setField(key) {
    return value => setForm(current => ({ ...current, [key]: value }))
  }

  function validate() {
    const found = {}
    if (!isRequired(form.name)) {
      found.name = 'Ingresá tu nombre.'
    }
    if (!isRequired(form.email)) {
      found.email = 'Ingresá tu email.'
    } else if (!isValidEmail(form.email)) {
      found.email = 'El email no es válido. Ejemplo: nombre@correo.com'
    }
    if (form.message.trim().length < 10) {
      found.message = 'El mensaje tiene que tener al menos 10 caracteres.'
    }
    if (!accepted) {
      found.privacy = 'Tenés que aceptar la Política de privacidad para enviar el mensaje.'
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
    setSending(false)
    setSent(true)
  }

  function sendAnother() {
    setForm(EMPTY_FORM)
    setAccepted(false)
    setErrors({})
    setSent(false)
  }

  return (
    <>
      <PageHeader title="Contacto" description="Mandanos una consulta, una sugerencia o un reclamo." />
      <section className="section contact" aria-label="Formulario de contacto">
        <div className="container">
          {sent ? (
            <div className="contact-success">
              <Alert kind="success">Recibimos tu mensaje. Te respondemos por email.</Alert>
              <Button variant="secondary" onClick={sendAnother}>Enviar otro</Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate aria-label="Enviar un mensaje">
              <div className="field-row">
                <FormField
                  id="name"
                  label="Nombre"
                  value={form.name}
                  onChange={setField('name')}
                  error={errors.name}
                  placeholder="Tu nombre"
                  autoComplete="name"
                  required
                />
                <FormField
                  id="email"
                  label="Email"
                  type="email"
                  value={form.email}
                  onChange={setField('email')}
                  error={errors.email}
                  placeholder="tu@email.com"
                  autoComplete="email"
                  required
                />
              </div>
              <FormField
                id="reason"
                label="Motivo"
                type="select"
                value={form.reason}
                onChange={setField('reason')}
                options={REASONS}
              />
              <FormField
                id="message"
                label="Mensaje"
                type="textarea"
                value={form.message}
                onChange={setField('message')}
                error={errors.message}
                hint="Mínimo 10 caracteres."
                placeholder="Contanos en qué podemos ayudarte"
                required
              />
              <PrivacyConsent checked={accepted} onChange={setAccepted} error={errors.privacy} />
              <Button type="submit" disabled={sending}>
                {sending ? 'Enviando…' : 'Enviar mensaje'}
              </Button>
            </form>
          )}
        </div>
      </section>
    </>
  )
}

export default ContactPage
