import { Link } from 'react-router-dom'
import './Logo.css'

// Logo de texto: link al inicio con nombre accesible.
function Logo() {
  return (
    <Link to="/" className="logo" aria-label="MGW Prod, ir al inicio">
      <span className="logo-mark">MGW</span>
      <span className="logo-text">PROD</span>
    </Link>
  )
}

export default Logo
