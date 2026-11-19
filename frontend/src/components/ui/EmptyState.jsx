// ============================================================================
// PARA ESTUDIAR: EmptyState.jsx ("estado vacío")
// ----------------------------------------------------------------------------
// QUÉ ES: lo que se muestra cuando no hay nada para mostrar: un filtro sin
//   resultados, una página que necesita sesión, un rol sin permiso.
// POR QUÉ IMPORTA: una pantalla en blanco confunde; un mensaje que explica
//   qué pasa y qué hacer (ej. botón "Entrar") es parte de "comunicar los
//   estados" que pide la rúbrica (criterio 7).
// children: todo lo que se pone entre <EmptyState> y </EmptyState>.
// ============================================================================
import './EmptyState.css'

// Mensaje para cuando no hay nada que mostrar (lista vacía, sin sesión,
// rol sin acceso). children: texto o acciones extra (ej. un botón "Entrar").
function EmptyState({ title, children }) {
  return (
    <section className="empty-state">
      <h2>{title}</h2>
      {children}
    </section>
  )
}

export default EmptyState
