import { useEffect } from 'react'
import StatRow from './StatRow'
import Waveform from './Waveform'
import './PageHeader.css'

// Encabezado de cada página: título grande (el único h1), bajada opcional,
// números opcionales y acciones (children). A propósito NO acepta un texto
// chico encima del título: es una decisión de diseño del equipo.
// También actualiza el título de la pestaña del navegador.
function PageHeader({ title, description, stats, children, wave = true }) {
  useEffect(() => {
    document.title = `${title} | MGW Prod`
  }, [title])

  return (
    <header className="page-header">
      <div className="container">
        <h1>{title}</h1>
        {description && <p className="page-header-description">{description}</p>}
        {children && <div className="page-header-actions">{children}</div>}
        {stats && <StatRow stats={stats} />}
      </div>
      {wave && <Waveform />}
    </header>
  )
}

export default PageHeader
