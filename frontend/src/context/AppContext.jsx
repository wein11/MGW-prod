import { createContext, useContext, useState } from 'react'
import * as mock from '../data/mockData'

// Estado compartido de toda la app: la sesión simulada y los datos de demo.
// Vive arriba de las rutas para que un cambio hecho en una pantalla (por
// ejemplo, aceptar un topline en el detalle del beat) se vea también en otra
// (Colaboraciones) sin pasar props por cada página. No hay backend: todo vive
// en memoria, así que al recargar la página (F5) vuelve a los datos de demo.
const AppContext = createContext(null)

export function AppProvider({ children }) {
  // user: { name, role: 'ARTIST' | 'LABEL', plan: 'FREE' | 'PREMIUM' } o null si no hay sesión.
  const [user, setUser] = useState(null)
  const [beats, setBeats] = useState(mock.beats)
  const [comments, setComments] = useState(mock.comments)
  const [toplines, setToplines] = useState(mock.toplines)
  const [submissions, setSubmissions] = useState(mock.submissions)

  // Devuelve true si el email y la contraseña coinciden con un usuario de demo.
  // Solo se guarda nombre, rol y plan: nunca el email ni la contraseña.
  function login(email, password) {
    const found = mock.users.find(
      u => u.email === email.trim().toLowerCase() && u.password === password
    )
    if (!found) return false
    setUser({ name: found.name, role: found.role, plan: 'FREE' })
    return true
  }

  // Alta simulada: con los datos ya validados, inicia la sesión directamente.
  function register({ name, role }) {
    setUser({ name: name.trim(), role, plan: 'FREE' })
  }

  function upgradeToPremium() {
    setUser({ ...user, plan: 'PREMIUM' })
  }

  function logout() {
    setUser(null)
  }

  const value = {
    user, login, register, upgradeToPremium, logout,
    beats, setBeats,
    comments, setComments,
    toplines, setToplines,
    submissions, setSubmissions,
  }

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp() {
  return useContext(AppContext)
}
