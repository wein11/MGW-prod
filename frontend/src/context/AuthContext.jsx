import { createContext, useContext, useRef, useState } from 'react'
import { users } from '../data/mockData'

// Sesión simulada. Guarda en sessionStorage solo { name, role, plan }:
// nunca el email ni la contraseña (datos sensibles). sessionStorage hace que
// la sesión sobreviva a un F5 pero se borre al cerrar la pestaña.
const STORAGE_KEY = 'mgw-session'

const AuthContext = createContext(null)

function normalizeEmail(email) {
  return email.trim().toLowerCase()
}

function readSession() {
  const saved = sessionStorage.getItem(STORAGE_KEY)
  return saved ? JSON.parse(saved) : null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readSession)
  // Cuentas creadas y usuarios Premium de esta visita. Viven solo en memoria,
  // como el resto de los datos simulados: nunca van al navegador y un F5 los
  // borra. Son refs porque no se dibujan: cambiarlos no tiene que re-renderizar.
  const registeredAccounts = useRef([])
  // Si se recarga con sesión Premium, se recupera el plan desde la sesión.
  const premiumNames = useRef(new Set(user?.plan === 'PREMIUM' ? [user.name] : []))

  function saveSession(session) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    setUser(session)
  }

  // Devuelve true si el email y la contraseña coinciden con un usuario de demo.
  function login(email, password) {
    const found = [...users, ...registeredAccounts.current].find(
      u => u.email === normalizeEmail(email) && u.password === password
    )
    if (!found) return false
    const plan = premiumNames.current.has(found.name) ? 'PREMIUM' : 'FREE'
    saveSession({ name: found.name, role: found.role, plan })
    return true
  }

  // Alta simulada: con los datos ya validados, inicia la sesión directamente.
  // Devuelve false si el email ya tiene una cuenta.
  function register({ name, email, password, role }) {
    const normalized = normalizeEmail(email)
    const exists = [...users, ...registeredAccounts.current].some(u => u.email === normalized)
    if (exists) return false
    registeredAccounts.current.push({ name: name.trim(), email: normalized, password, role })
    saveSession({ name: name.trim(), role, plan: 'FREE' })
    return true
  }

  function upgradeToPremium() {
    premiumNames.current.add(user.name)
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
