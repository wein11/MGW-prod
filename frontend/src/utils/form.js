// Ayudas para formularios.

// Lleva el foco al primer campo con error, para que quien navega con teclado
// o lector de pantalla llegue directo al problema. Requiere que el id de cada
// campo sea igual a su clave en el objeto de errores.
export function focusFirstError(errors) {
  const firstKey = Object.keys(errors)[0]
  if (firstKey) document.getElementById(firstKey)?.focus()
}

export function hasErrors(errors) {
  return Object.keys(errors).length > 0
}
