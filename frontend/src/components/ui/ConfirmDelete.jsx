import { useEffect, useRef, useState } from 'react'
import Button from './Button'
import './ConfirmDelete.css'

// Borrado en dos pasos: primero "Borrar", después "¿Seguro…? Sí, borrar / Cancelar".
// Al abrir la confirmación el foco va a "Cancelar" (la opción segura), así un
// Enter accidental no borra nada.
// label: lo que se borra, para el texto de la pregunta (ej. 'el beat "Noche en Palermo"').
function ConfirmDelete({ label, buttonText = 'Borrar', onConfirm }) {
  const [confirming, setConfirming] = useState(false)
  const cancelRef = useRef(null)

  useEffect(() => {
    if (confirming) cancelRef.current?.focus()
  }, [confirming])

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
        <Button variant="secondary" ref={cancelRef} onClick={() => setConfirming(false)}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}

export default ConfirmDelete
