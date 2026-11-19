// ============================================================================
// PARA ESTUDIAR: App.jsx (el mapa de rutas)
// ----------------------------------------------------------------------------
// QUÉ ES: define qué página se muestra para cada dirección (URL). Es una SPA
//   (Single Page Application): el navegador carga un solo HTML y React cambia
//   el contenido sin recargar cuando cambia la URL.
// PIEZAS DE REACT ROUTER:
//   - <BrowserRouter>: conecta React con la barra de direcciones del navegador.
//   - <Routes>: mira la URL actual y elige UNA <Route> que coincida.
//   - <Route path="/beats" element={<BeatsPage />}>: "si la URL es /beats,
//     dibujá BeatsPage".
//   - <Route path="/beats/:id">: ":id" es un parámetro. /beats/3 muestra el
//     detalle del beat 3; la página lo lee con useParams().
//   - <Route path="*">: comodín. Atrapa cualquier URL que no exista → 404.
// RUTAS ANIDADAS (layouts):
//   - Las rutas de adentro de <Route element={<Layout />}> se dibujan DENTRO
//     de Layout (que tiene Header y Footer) en el lugar donde Layout pone
//     <Outlet />. Así no repetimos el menú en cada página.
//   - Login y Registro van dentro de AuthLayout, que no tiene menú.
// PREGUNTA PROBABLE: "¿Qué pasa si escribo /cualquier-cosa?"
//   → Entra en path="*" y se muestra NotFoundPage con un link al inicio.
// PREGUNTA PROBABLE: "¿Por qué Login no tiene el menú?"
//   → Porque está en otro layout (AuthLayout): así lo pedía el wireframe, para
//     que la persona se enfoque en el formulario.
// ============================================================================
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import AuthLayout from './components/layout/AuthLayout'
import Layout from './components/layout/Layout'
import AboutPage from './pages/AboutPage'
import BeatDetailPage from './pages/BeatDetailPage'
import BeatsPage from './pages/BeatsPage'
import ChallengesPage from './pages/ChallengesPage'
import CollaborationsPage from './pages/CollaborationsPage'
import ContactPage from './pages/ContactPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import PremiumPage from './pages/PremiumPage'
import PrivacyPage from './pages/PrivacyPage'
import RankingPage from './pages/RankingPage'
import RegisterPage from './pages/RegisterPage'

// Rutas de la app. Las páginas con menú van dentro de <Layout>; Login y
// Registro van dentro de <AuthLayout> (sin menú, como en el wireframe).
// La ruta "*" atrapa cualquier dirección que no exista y muestra la 404.
function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Grupo 1: páginas con Header y Footer (Layout). */}
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/beats" element={<BeatsPage />} />
          {/* :id es dinámico: /beats/1, /beats/2, etc. */}
          <Route path="/beats/:id" element={<BeatDetailPage />} />
          <Route path="/colaboraciones" element={<CollaborationsPage />} />
          <Route path="/desafios" element={<ChallengesPage />} />
          <Route path="/ranking" element={<RankingPage />} />
          <Route path="/premium" element={<PremiumPage />} />
          <Route path="/nosotros" element={<AboutPage />} />
          <Route path="/contacto" element={<ContactPage />} />
          <Route path="/privacidad" element={<PrivacyPage />} />
          {/* Cualquier otra dirección: página 404. */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        {/* Grupo 2: Login y Registro, sin menú (AuthLayout). */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
