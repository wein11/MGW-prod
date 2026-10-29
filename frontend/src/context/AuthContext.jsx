import { createContext, useContext, useState } from 'react'
import { users } from '../data/mockData'

// Sesión simulada. Guarda en sessionStorage solo { name, role, plan }:
// nunca el email ni la contraseña (datos sensibles). sessionStorage hace que
// la sesión sobreviva a un F5 pero se borre al cerrar la pestaña.
const STORAGE_KEY = 'mgw-session'

const AuthContext = createContext(null)

function readSession() {
  const saved = sessionStorage.getItem(STORAGE_KEY)
  return saved ? JSON.parse(saved) : null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readSession)

  function saveSession(session) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    setUser(session)
  }

  // Devuelve true si el email y la contraseña coinciden con un usuario de demo.
  function login(email, password) {
    const found = users.find(
      u => u.email === email.trim().toLowerCase() && u.password === password
    )
    if (!found) return false
    saveSession({ name: found.name, role: found.role, plan: 'FREE' })
    return true
  }

  // Alta simulada: con los datos ya validados, inicia la sesión directamente.
  function register({ name, role }) {
    saveSession({ name: name.trim(), role, plan: 'FREE' })
  }

  function upgradeToPremium() {
    saveSession({ ...user, plan: 'PREMIUM' })
  }

  function logout() {
    sessionStorage.removeItem(STORAGE_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, upgradeToPremium, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// user: { name, role: 'ARTIST' | 'LABEL', plan: 'FREE' | 'PREMIUM' } o null si no hay sesión.
export function useAuth() {
  return useContext(AuthContext)
}
