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
