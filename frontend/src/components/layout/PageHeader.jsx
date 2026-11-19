// ============================================================================
// PARA ESTUDIAR: PageHeader.jsx (encabezado de cada página)
// ----------------------------------------------------------------------------
// QUÉ ES: el título grande de arriba de cada página, con bajada, números y
//   la onda decorativa. Todas las páginas lo usan → mismo diseño en todas.
// SEMÁNTICA (criterio 3): acá está el ÚNICO <h1> de cada página. Después las
//   secciones usan <h2>, y adentro <h3>. Esa jerarquía es la que usa el
//   lector de pantalla para navegar por títulos.
// useEffect (EL OTRO useEffect DE LA APP): cambia document.title, que es el
//   texto de la pestaña del navegador ("Beats | MGW Prod"). Es un "efecto"
//   porque toca algo de afuera de React (el documento). Se repite solo si
//   cambia `title` (por eso [title] en las dependencias).
// RENDERIZADO CONDICIONAL: {description && <p>...}: solo se dibuja si la
//   página pasó una descripción. Lo mismo con stats y children.
// ============================================================================
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
