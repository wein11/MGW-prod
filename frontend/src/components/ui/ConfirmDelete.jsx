// ============================================================================
// PARA ESTUDIAR: ConfirmDelete.jsx (borrar en dos pasos)
// ----------------------------------------------------------------------------
// QUÉ ES: el botón "Borrar" que antes de borrar pregunta "¿Seguro?".
//   Se usa en beats, comentarios y toplines.
// CÓMO: un useState `confirming` (true/false).
//   - false → se ve solo el botón "Borrar".
//   - Al tocarlo pasa a true → se ve la pregunta con "Sí, borrar" y "Cancelar".
//   - "Cancelar" lo vuelve a false; "Sí, borrar" llama a onConfirm, que es la
//     función que le pasó la página (la que realmente borra el dato).
//   Esto es renderizado condicional con un `if` antes del return.
// autoFocus en Cancelar: al aparecer la pregunta, el foco queda en Cancelar.
//   Si alguien aprieta Enter sin mirar, cancela en vez de borrar.
// POR QUÉ NO window.confirm(): el cuadro del navegador no se puede diseñar,
//   corta la página y no sigue el estilo de la app.
// PREGUNTA PROBABLE: "¿Por qué onConfirm viene por props?"
//   → Porque el componente no sabe QUÉ se borra. Cada página le pasa su función
//     (borrar beat, borrar topline...). Así el mismo componente sirve para todo.
// ============================================================================
import { useState } from 'react'
import Button from './Button'
import './ConfirmDelete.css'

// Borrado en dos pasos: primero "Borrar", después "¿Seguro…? Sí, borrar / Cancelar".
// "Cancelar" lleva autoFocus (atributo de HTML): al aparecer recibe el foco,
// así un Enter accidental no borra nada.
// label: lo que se borra, para el texto de la pregunta (ej. 'el beat "Noche en Palermo"').
function ConfirmDelete({ label, buttonText = 'Borrar', onConfirm }) {
  const [confirming, setConfirming] = useState(false)

  if (!confirming) {
    return (
      <Button variant="secondary" onClick={() => setConfirming(true)}>
        {buttonText}
      </Button>
    )
  }

  return (
    <div className="confirm-delete">
      <p>¿Seguro que querés borrar {label}? No se puede deshacer.</p>
      <div className="confirm-delete-actions">
        <Button onClick={onConfirm}>Sí, borrar</Button>
        {/* El linter avisa de autoFocus porque al cargar una página desorienta.
            Acá aparece solo después de tocar "Borrar", así que es lo esperado. */}
        {/* oxlint-disable-next-line jsx-a11y/no-autofocus */}
        <Button variant="secondary" autoFocus onClick={() => setConfirming(false)}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}

export default ConfirmDelete
