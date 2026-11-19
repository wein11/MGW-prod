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
