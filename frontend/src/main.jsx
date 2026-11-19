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
