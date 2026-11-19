// ============================================================================
// PARA ESTUDIAR: LoginPage.jsx (ruta "/login")
// ----------------------------------------------------------------------------
// QUÉ ES: el inicio de sesión simulado. Compara con las cuentas del Context.
// FLUJO DEL FORMULARIO (el mismo patrón que Registro, Contacto, etc.):
//   1. Cada campo es controlado: useState(email), useState(password).
//   2. handleSubmit recibe el evento y hace event.preventDefault() para que el
//      navegador no recargue la página (comportamiento por defecto de <form>).
//   3. validate() arma el objeto de errores (vacío, formato de email).
//   4. Si hay errores: setErrors + focusFirstError y se corta ahí.
//   5. Si no: sending=true (botón "Enviando…" y deshabilitado), await
//      fakeRequest, y se llama a login(email, password) del Context.
//   6. Si login devuelve false: Alert de error "El email o la contraseña no
//      son correctos", se borra la contraseña y el foco va a ese campo.
//   7. Si devuelve true: cambia `user` y el <Navigate to="/"> de arriba
//      redirige al inicio.
// <Navigate>: componente de React Router que redirige apenas se dibuja. Con
//   `replace` reemplaza la entrada del historial ("Atrás" no vuelve al Login).
// noValidate en <form>: apaga los globos de error del navegador para mostrar
//   los nuestros, que son accesibles y tienen el estilo de la app.
// ============================================================================
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import Alert from '../components/ui/Alert'
import Button from '../components/ui/Button'
import FormField from '../components/ui/FormField'
import { useApp } from '../context/AppContext'
import { fakeRequest } from '../data/fakeRequest'
import { focusFirstError, hasErrors } from '../utils/form'
import { isRequired, isValidEmail } from '../utils/validation'
import './AuthPages.css'

// Login simulado: compara con los usuarios de demo (mockData). Si coincide,
// AppContext guarda la sesión y esta página redirige al inicio.
function LoginPage() {
  const { user, login } = useApp()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [failed, setFailed] = useState(false)
  const [sending, setSending] = useState(false)

  // Con sesión iniciada no tiene sentido ver el login: va al inicio.
  if (user) return <Navigate to="/" replace />

  function validate() {
    const found = {}
    if (!isRequired(email)) {
      found.email = 'Ingresá tu email.'
    } else if (!isValidEmail(email)) {
      found.email = 'El email no es válido. Ejemplo: nombre@correo.com'
    }
    if (!isRequired(password)) {
      found.password = 'Ingresá tu contraseña.'
    }
    return found
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (sending) return
    setFailed(false)
    const found = validate()
    setErrors(found)
    if (hasErrors(found)) {
      focusFirstError(found)
      return
    }
    setSending(true)
    await fakeRequest(600)
    if (!login(email, password)) {
      // Credenciales incorrectas: se avisa, se borra la contraseña y el foco
      // vuelve a ese campo para reintentar sin tocar el mouse.
      setSending(false)
      setFailed(true)
      setPassword('')
      document.getElementById('password')?.focus()
    }
    // Si salió bien, user cambia y el <Navigate> de arriba redirige al inicio.
  }

  return (
    <div className="auth-page">
      <PageHeader title="Iniciar sesión" wave={false} />
      <form onSubmit={handleSubmit} noValidate aria-label="Iniciar sesión">
        <FormField
          id="email"
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          error={errors.email}
          placeholder="tu@email.com"
          autoComplete="email"
          required
        />
        <FormField
          id="password"
          label="Contraseña"
          type="password"
          value={password}
          onChange={setPassword}
          error={errors.password}
          autoComplete="current-password"
          required
        />
        <Button type="submit" disabled={sending} className="auth-submit">
          {sending ? 'Enviando…' : 'Entrar'}
        </Button>
        {failed && <Alert kind="error">El email o la contraseña no son correctos.</Alert>}
      </form>
      <p>
        ¿No tenés cuenta? <Link to="/registro">Registrate</Link>
      </p>
    </div>
  )
}

export default LoginPage
