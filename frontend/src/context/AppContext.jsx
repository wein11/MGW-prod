// ============================================================================
// PARA ESTUDIAR: AppContext.jsx (estado compartido de toda la app)
// ----------------------------------------------------------------------------
// QUÉ ES: un Context de React. Sirve para que muchos componentes lean y
//   modifiquen los mismos datos sin pasarlos por props de página en página
//   (eso se llama "prop drilling" y es lo que evitamos).
// LAS 3 PIEZAS:
//   1. createContext(null): crea el "canal" (AppContext).
//   2. <AppProvider>: componente que guarda los datos con useState y los
//      publica con <AppContext.Provider value={...}>. Se usa una sola vez,
//      en main.jsx, envolviendo toda la app.
//   3. useApp(): hook propio que hace useContext(AppContext). Cualquier
//      componente escribe `const { user, beats } = useApp()` y listo.
// QUÉ GUARDA:
//   - user: la sesión simulada ({ name, role, plan } o null si no hay sesión).
//   - accounts: las cuentas que pueden entrar (las de demo + las registradas).
//   - beats, comments, toplines, submissions: los datos de demo, que arrancan
//     con lo de mockData.js y cambian cuando el usuario publica, borra, etc.
// POR QUÉ ACÁ Y NO EN CADA PÁGINA: si aceptás un topline en el detalle del
//   beat, el cambio tiene que verse también en Colaboraciones. Si cada página
//   tuviera su propio useState, cada una tendría una copia distinta.
// SIN BACKEND: todo vive en memoria (useState). Al apretar F5 se pierde todo
//   y vuelve a los datos de demo; también se cierra la sesión.
// PREGUNTA PROBABLE: "¿Qué pasa cuando llamo a setBeats?"
//   → React guarda el nuevo array y vuelve a dibujar todos los componentes que
//     usan beats (por eso el listado se actualiza solo).
// PREGUNTA PROBABLE: "¿Por qué setX(nuevoArray) y no beats.push(...)?"
//   → Porque en React el estado no se modifica directo: hay que darle un valor
//     NUEVO al setter. Si mutás el array, React no se entera y no re-dibuja.
// ============================================================================
import { createContext, useContext, useState } from 'react'
import * as mock from '../data/mockData'

// Estado compartido de toda la app: la sesión simulada y los datos de demo.
// Vive arriba de las rutas para que un cambio hecho en una pantalla (por
// ejemplo, aceptar un topline en el detalle del beat) se vea también en otra
// (Colaboraciones) sin pasar props por cada página. No hay backend: todo vive
// en memoria, así que al recargar la página (F5) vuelve a los datos de demo.
const AppContext = createContext(null)

// { children } = todo lo que va entre <AppProvider> y </AppProvider> (la app).
export function AppProvider({ children }) {
  // user: { name, role: 'ARTIST' | 'LABEL', plan: 'FREE' | 'PREMIUM' } o null si no hay sesión.
  const [user, setUser] = useState(null)
  // Cuentas de esta visita: las de demo más las creadas en Registro. Cada una
  // guarda su plan, así un usuario que pasó a Premium sigue Premium si sale y
  // vuelve a entrar. Como todo lo demás, un F5 las vuelve a las de demo.
  // .map(u => ({ ...u, plan: 'FREE' })) copia cada usuario y le agrega plan.
  const [accounts, setAccounts] = useState(mock.users.map(u => ({ ...u, plan: 'FREE' })))
  // Los datos de la demo arrancan con lo que hay en mockData.js.
  const [beats, setBeats] = useState(mock.beats)
  const [comments, setComments] = useState(mock.comments)
  const [toplines, setToplines] = useState(mock.toplines)
  const [submissions, setSubmissions] = useState(mock.submissions)

  // Busca una cuenta por email. trim() saca espacios y toLowerCase() pasa a
  // minúsculas, así " Artista@MGW.com " encuentra a "artista@mgw.com".
  function findAccount(email) {
    return accounts.find(a => a.email === email.trim().toLowerCase())
  }

  // Devuelve true si el email y la contraseña coinciden con una cuenta.
  // En la sesión solo va nombre, rol y plan: nunca el email ni la contraseña.
  // LoginPage usa el true/false para mostrar el error o entrar.
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
    // [...accounts, account] = array nuevo con todas las cuentas + la nueva.
    setAccounts([...accounts, account])
    setUser({ name: account.name, role, plan: 'FREE' })
    return true
  }

  // Se llama al terminar el pago simulado de Premium. Cambia el plan en dos
  // lugares: en la cuenta (para que dure si sale y vuelve a entrar) y en la
  // sesión actual (para que se vea ya).
  function upgradeToPremium() {
    setAccounts(accounts.map(a => (a.name === user.name ? { ...a, plan: 'PREMIUM' } : a)))
    setUser({ ...user, plan: 'PREMIUM' })
  }

  // Cerrar sesión = volver user a null. Las páginas que miran `user` se
  // re-dibujan solas (por ejemplo, el Header vuelve a mostrar "Entrar").
  function logout() {
    setUser(null)
  }

  // Todo lo que se publica en el Context. Lo que no está acá (accounts,
  // findAccount) queda privado: solo se usa adentro de este archivo.
  const value = {
    user, login, register, upgradeToPremium, logout,
    beats, setBeats,
    comments, setComments,
    toplines, setToplines,
    submissions, setSubmissions,
  }

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

// Hook propio: atajo para no escribir useContext(AppContext) en cada página.
// Por convención, todo lo que empieza con "use" es un hook.
export function useApp() {
  return useContext(AppContext)
}
