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
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/beats" element={<BeatsPage />} />
          <Route path="/beats/:id" element={<BeatDetailPage />} />
          <Route path="/colaboraciones" element={<CollaborationsPage />} />
          <Route path="/desafios" element={<ChallengesPage />} />
          <Route path="/ranking" element={<RankingPage />} />
          <Route path="/premium" element={<PremiumPage />} />
          <Route path="/nosotros" element={<AboutPage />} />
          <Route path="/contacto" element={<ContactPage />} />
          <Route path="/privacidad" element={<PrivacyPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
