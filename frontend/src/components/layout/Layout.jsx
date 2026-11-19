// ============================================================================
// PARA ESTUDIAR: Layout.jsx (el "molde" de las páginas con menú)
// ----------------------------------------------------------------------------
// QUÉ ES: lo que se repite en todas las páginas: barra de arriba (LiveBar),
//   menú (Header), contenido y pie (Footer).
// <Outlet />: el "hueco" donde React Router pone la página actual. Si la URL
//   es /beats, en el Outlet aparece BeatsPage (ver rutas en App.jsx).
// useEffect + useLocation (UNO DE LOS 2 useEffect DE LA APP):
//   - useLocation() devuelve la ubicación actual; pathname es "/beats", etc.
//   - useEffect(() => {...}, [pathname]) ejecuta el código cada vez que
//     cambia pathname, o sea, cada vez que se cambia de página.
//   - Sin esto, al pasar de una página larga a otra, quedarías scrolleado
//     a la mitad (en una SPA el navegador no vuelve arriba solo).
// ACCESIBILIDAD: "Saltar al contenido" es un link oculto que aparece al
//   apretar Tab. Lleva directo a <main id="main"> sin recorrer todo el menú.
//   tabIndex="-1" en main permite que reciba el foco al saltar.
// ============================================================================
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
