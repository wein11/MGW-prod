// ============================================================================
// PARA ESTUDIAR: Header.jsx (menú principal)
// ----------------------------------------------------------------------------
// QUÉ ES: la barra con el logo, los links a cada sección y el botón
//   Entrar/Salir. Está en todas las páginas con menú (via Layout).
// ESTADO LOCAL: menuOpen (useState) guarda si el menú del celular está abierto.
//   Es local porque solo le importa al Header (no va en el Context).
// NAVLINK: es un Link que sabe si es la página actual. Le agrega solo
//   aria-current="page" (el lector dice "página actual") y lo usamos en CSS
//   para subrayarlo. end en "/" evita que Inicio quede marcado en todas.
// RENDERIZADO CONDICIONAL: {user ? (nombre + Salir) : (Entrar)}. Si hay
//   sesión muestra una cosa; si no, la otra.
// SALIR: logout() borra la sesión y navigate('/') lleva al inicio.
//   useNavigate() es el hook de React Router para cambiar de página desde código.
// MENÚ MÓVIL (criterio 5): en pantallas chicas los links se esconden detrás
//   del botón "Menú". aria-expanded le dice al lector si está abierto o cerrado
//   y aria-controls a qué elemento controla.
// NAV_LINKS es un array y se dibuja con .map(): agregar una sección es
//   agregar una línea al array.
// ============================================================================
import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useApp } from '../../context/AppContext'
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
  const { user, logout } = useApp()
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
