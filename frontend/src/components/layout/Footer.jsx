import { Link } from 'react-router-dom'
import './Footer.css'

// Se calcula una vez al cargar, no en cada render.
const CURRENT_YEAR = new Date().getFullYear()

// Pie de todas las páginas. Incluye el link a la Política de privacidad,
// obligatorio porque la app maneja datos personales.
function Footer() {
  return (
    <footer className="site-footer">
      <div className="container site-footer-inner">
        <p>© {CURRENT_YEAR} MGW Prod</p>
        <nav aria-label="Pie de página">
          <ul>
            <li><Link to="/nosotros">Quiénes somos</Link></li>
            <li><Link to="/contacto">Contacto</Link></li>
            <li><Link to="/privacidad">Política de privacidad</Link></li>
          </ul>
        </nav>
      </div>
    </footer>
  )
}

export default Footer
