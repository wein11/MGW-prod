import { Link, Outlet } from 'react-router-dom'
import Logo from './Logo'
import Waveform from './Waveform'
import './AuthLayout.css'

// Layout de Login y Registro: sin menú, como en el wireframe, para que la
// persona se concentre en el formulario. Igual tiene el link al inicio (logo)
// y a la Política de privacidad.
function AuthLayout() {
  return (
    <div className="auth-layout">
      <header className="container auth-header">
        <Logo />
      </header>
      <main id="main" className="auth-main">
        <Outlet />
      </main>
      {/* Se abre en otra pestaña para no perder lo que se esté escribiendo. */}
      <footer className="container auth-footer">
        <Link to="/privacidad" target="_blank" rel="noopener">
          Política de privacidad<span className="visually-hidden"> (se abre en otra pestaña)</span>
        </Link>
      </footer>
      <Waveform />
    </div>
  )
}

export default AuthLayout
