import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Footer from './Footer'
import Header from './Header'
import LiveBar from './LiveBar'
import './Layout.css'

// Estructura común de las páginas con navegación. El link "Saltar al
// contenido" es el primer elemento enfocable: permite a quien usa teclado
// evitar recorrer todo el menú en cada página.
function Layout() {
  const { pathname } = useLocation()

  // Al cambiar de página, volver arriba (si no, se mantiene el scroll anterior).
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return (
    <>
      <a href="#main" className="skip-link">Saltar al contenido</a>
      <LiveBar />
      <Header />
      <main id="main" tabIndex="-1">
        <Outlet />
      </main>
      <Footer />
    </>
  )
}

export default Layout
