import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import Button from '../ui/Button'
import Logo from './Logo'
import './Header.css'

const NAV_LINKS = [
  { to: '/', label: 'Inicio' },
  { to: '/beats', label: 'Beats' },
  { to: '/colaboraciones', label: 'Colaboraciones' },
  { to: '/desafios', label: 'Desafíos' },
  { to: '/ranking', label: 'Ranking' },
  { to: '/premium', label: 'Premium' },
  { to: '/nosotros', label: 'Nosotros' },
  { to: '/contacto', label: 'Contacto' },
]

// Barra de navegación. En pantallas chicas los links se esconden detrás de
// un botón "Menú" con aria-expanded, que avisa al lector de pantalla si está
// abierto. NavLink agrega aria-current="page" al link de la página actual.
function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  function handleLogout() {
    logout()
    setMenuOpen(false)
    navigate('/')
  }

  return (
    <header className="site-header">
      <div className="container site-header-inner">
        <Logo />

        <button
          type="button"
          className="menu-toggle"
          aria-expanded={menuOpen}
          aria-controls="main-nav"
          onClick={() => setMenuOpen(!menuOpen)}
        >
          {menuOpen ? 'Cerrar' : 'Menú'}
        </button>

        <nav id="main-nav" aria-label="Principal" className={menuOpen ? 'nav nav-open' : 'nav'}>
          <ul className="nav-list">
            {NAV_LINKS.map(link => (
              <li key={link.to}>
                <NavLink to={link.to} end={link.to === '/'} onClick={() => setMenuOpen(false)}>
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>

          <div className="nav-session">
            {user ? (
              <>
                <span className="nav-user">{user.name}</span>
                <Button variant="secondary" onClick={handleLogout}>Salir</Button>
              </>
            ) : (
              <Button to="/login" onClick={() => setMenuOpen(false)}>Entrar</Button>
            )}
          </div>
        </nav>
      </div>
    </header>
  )
}

export default Header
