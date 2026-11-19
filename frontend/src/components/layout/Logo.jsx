// ============================================================================
// PARA ESTUDIAR: Logo.jsx
// ----------------------------------------------------------------------------
// QUÉ ES: el logo "MGW PROD" hecho con texto (no es una imagen), que siempre
//   lleva al inicio. Está en el Header y en Login/Registro.
// ACCESIBILIDAD: aria-label="MGW Prod, ir al inicio" le da un nombre claro al
//   link. Sin eso, el lector leería solo "MGW PROD" y no sabrías adónde va.
// ============================================================================
import { Link } from 'react-router-dom'
import './Logo.css'

// Logo de texto: link al inicio con nombre accesible.
function Logo() {
  return (
    <Link to="/" className="logo" aria-label="MGW Prod, ir al inicio">
      <span className="logo-mark">MGW</span>{' '}
      <span className="logo-text">PROD</span>
    </Link>
  )
}

export default Logo
