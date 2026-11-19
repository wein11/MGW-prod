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
  // Cuentas de esta visita: las de demo más las creadas en Registro. Cada una
  // guarda su plan, así un usuario que pasó a Premium sigue Premium si sale y
  // vuelve a entrar. Como todo lo demás, un F5 las vuelve a las de demo.
  const [accounts, setAccounts] = useState(mock.users.map(u => ({ ...u, plan: 'FREE' })))
  const [beats, setBeats] = useState(mock.beats)
  const [comments, setComments] = useState(mock.comments)
  const [toplines, setToplines] = useState(mock.toplines)
  const [submissions, setSubmissions] = useState(mock.submissions)

  function findAccount(email) {
    return accounts.find(a => a.email === email.trim().toLowerCase())
  }

  // Devuelve true si el email y la contraseña coinciden con una cuenta.
  // En la sesión solo va nombre, rol y plan: nunca el email ni la contraseña.
  function login(email, password) {
    const found = findAccount(email)
    if (!found || found.password !== password) return false
    setUser({ name: found.name, role: found.role, plan: found.plan })
    return true
  }

  // Alta simulada: con los datos ya validados, crea la cuenta e inicia la
  // sesión. Devuelve false si el email ya tiene una cuenta.
  function register({ name, email, password, role }) {
    if (findAccount(email)) return false
    const account = { name: name.trim(), email: email.trim().toLowerCase(), password, role, plan: 'FREE' }
    setAccounts([...accounts, account])
    setUser({ name: account.name, role, plan: 'FREE' })
    return true
  }

  function upgradeToPremium() {
    setAccounts(accounts.map(a => (a.name === user.name ? { ...a, plan: 'PREMIUM' } : a)))
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
