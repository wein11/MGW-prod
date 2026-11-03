import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import FormField from '../components/ui/FormField'
import PrivacyConsent from '../components/ui/PrivacyConsent'
import { useAuth } from '../context/AuthContext'
import { fakeRequest } from '../data/fakeRequest'
import { focusFirstError, hasErrors } from '../utils/form'
import { isRequired, isValidEmail, isValidPassword } from '../utils/validation'
import './AuthPages.css'

const ACCOUNT_TYPES = [
  { value: 'ARTIST', title: 'Artista', text: 'Entregás en los desafíos, subís beats y toplines.' },
  { value: 'LABEL', title: 'Discográfica', text: 'Creás desafíos y elegís al artista invitado.' },
]

// Alta simulada. Al terminar inicia la sesión y va al inicio. La contraseña y
// el email se validan pero nunca se guardan (useAuth solo guarda nombre y rol).
function RegisterPage() {
  const { user, register } = useAuth()
  const [form, setForm] = useState({ name: '', email: '', password: '', city: '' })
  const [role, setRole] = useState('ARTIST')
  const [accepted, setAccepted] = useState(false)
  const [errors, setErrors] = useState({})
  const [sending, setSending] = useState(false)

  if (user) return <Navigate to="/" replace />

  function setField(key) {
    return value => setForm(current => ({ ...current, [key]: value }))
  }

  // Las claves van en el orden de los campos para que el foco llegue al primero con error.
  function validate() {
    const found = {}
    if (!isRequired(form.name)) {
      found.name = 'Ingresá tu nombre artístico.'
    }
    if (!isRequired(form.email)) {
      found.email = 'Ingresá tu email.'
    } else if (!isValidEmail(form.email)) {
      found.email = 'El email no es válido. Ejemplo: nombre@correo.com'
    }
    if (!isRequired(form.password)) {
      found.password = 'Ingresá una contraseña.'
    } else if (!isValidPassword(form.password)) {
      found.password = 'La contraseña tiene que tener al menos 8 caracteres.'
    }
    if (!accepted) {
      found.privacy = 'Tenés que aceptar la Política de privacidad para crear la cuenta.'
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
    // register() inicia la sesión; el <Navigate> de arriba lleva al inicio.
    register({ name: form.name, role })
  }

  return (
    <div className="auth-page">
      <PageHeader title="Crear cuenta" wave={false} />
      <form onSubmit={handleSubmit} noValidate aria-label="Crear cuenta">
        <FormField
          id="name"
          label="Nombre artístico"
          value={form.name}
          onChange={setField('name')}
          error={errors.name}
          autoComplete="nickname"
          required
        />
        <FormField
          id="email"
          label="Email"
          type="email"
          value={form.email}
          onChange={setField('email')}
          error={errors.email}
          autoComplete="email"
          required
        />
        <FormField
          id="password"
          label="Contraseña"
          type="password"
          value={form.password}
          onChange={setField('password')}
          error={errors.password}
          hint="Mínimo 8 caracteres."
          autoComplete="new-password"
          required
        />

        {/* Tipo de cuenta: radios reales (se usan con flechas y Tab) con aspecto de tarjeta. */}
        <fieldset className="account-type">
          <legend className="label-caps">Tipo de cuenta</legend>
          <div className="account-options">
            {ACCOUNT_TYPES.map(type => (
              <label
                key={type.value}
                className={role === type.value ? 'account-option account-option-selected' : 'account-option'}
              >
                <input
                  type="radio"
                  name="role"
                  value={type.value}
                  checked={role === type.value}
                  onChange={() => setRole(type.value)}
                  className="visually-hidden"
                />
                <strong>{type.title}</strong>
                <span>{type.text}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <FormField
          id="city"
          label="Ciudad (opcional)"
          value={form.city}
          onChange={setField('city')}
          autoComplete="address-level2"
        />
        <PrivacyConsent checked={accepted} onChange={setAccepted} error={errors.privacy} />
        <Button type="submit" disabled={sending} className="auth-submit">
          {sending ? 'Enviando…' : 'Crear cuenta'}
        </Button>
      </form>
      <p>
        ¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link>
      </p>
    </div>
  )
}

export default RegisterPage
