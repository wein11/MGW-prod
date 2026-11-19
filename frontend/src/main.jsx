// ============================================================================
// PARA ESTUDIAR: main.jsx (punto de entrada de la app)
// ----------------------------------------------------------------------------
// QUÉ ES: el primer archivo que ejecuta el navegador. index.html tiene un
//   <div id="root"> vacío y este archivo "monta" React adentro de ese div.
// QUÉ HACE, EN ORDEN:
//   1. Importa los estilos globales (tokens.css = variables de colores/medidas,
//      global.css = estilos base). Al importarlos acá aplican a toda la app.
//   2. createRoot(...) crea la raíz de React sobre el div #root.
//   3. render(...) dibuja el árbol de componentes: StrictMode > AppProvider > App.
// CONCEPTOS:
//   - StrictMode: modo de desarrollo de React que ejecuta dos veces algunos
//     pasos para detectar errores. No se ve en pantalla ni afecta el build final.
//   - AppProvider: el "proveedor" del Context. Todo lo que está adentro
//     (o sea, toda la app) puede leer la sesión y los datos con useApp().
// PREGUNTA PROBABLE: "¿Por qué AppProvider envuelve a App y no al revés?"
//   → Porque el Context solo llega a los componentes que están ADENTRO del
//     Provider. Si estuviera adentro de App, el Header o las rutas no lo verían.
// ============================================================================
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/tokens.css'
import './styles/global.css'
import App from './App.jsx'
import { AppProvider } from './context/AppContext'

// AppProvider envuelve toda la app: la sesión simulada y los datos compartidos
// quedan disponibles en cualquier página con useApp().
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppProvider>
      <App />
    </AppProvider>
  </StrictMode>,
)
