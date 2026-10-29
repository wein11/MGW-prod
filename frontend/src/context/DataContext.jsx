import { createContext, useContext, useState } from 'react'
import * as mock from '../data/mockData'

// Estado compartido de los datos simulados. Vive arriba de las rutas para que
// un cambio hecho en una pantalla (por ejemplo, aceptar un topline en el
// detalle del beat) se vea también en otra (Colaboraciones) sin backend.
// Cada página arma su lógica usando los setters, como en el TodoList de clase.
const DataContext = createContext(null)

export function DataProvider({ children }) {
  const [beats, setBeats] = useState(mock.beats)
  const [comments, setComments] = useState(mock.comments)
  const [toplines, setToplines] = useState(mock.toplines)
  const [submissions, setSubmissions] = useState(mock.submissions)

  const value = {
    beats, setBeats,
    comments, setComments,
    toplines, setToplines,
    submissions, setSubmissions,
  }

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>
}

export function useData() {
  return useContext(DataContext)
}

