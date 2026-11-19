// ============================================================================
// PARA ESTUDIAR: form.js (ayudas para todos los formularios)
// ----------------------------------------------------------------------------
// CÓMO VALIDAMOS UN FORMULARIO (patrón que se repite en todas las páginas):
//   1. Al enviar, armamos un objeto `errors` vacío.
//   2. Por cada campo que está mal, agregamos errors[idDelCampo] = 'mensaje'.
//   3. hasErrors(errors) → si hay alguno, no se envía.
//   4. focusFirstError(errors) → mueve el cursor al primer campo con error.
// POR QUÉ EL FOCO: alguien que usa teclado o lector de pantalla no "ve" el
//   error en rojo; al mover el foco al campo, el lector lee la etiqueta y el
//   mensaje de error, y la persona puede corregirlo directo. (Accesibilidad,
//   criterio 3 y 7 de la rúbrica.)
// IMPORTANTE: funciona porque la CLAVE del error es igual al id del input.
//   Ej.: errors.email ↔ <input id="email">.
// ============================================================================

// Ayudas para formularios.

// Lleva el foco al primer campo con error, para que quien navega con teclado
// o lector de pantalla llegue directo al problema. Requiere que el id de cada
// campo sea igual a su clave en el objeto de errores.
export function focusFirstError(errors) {
  const firstKey = Object.keys(errors)[0]
  // ?. = "si existe": si no encuentra el elemento, no hace nada (no rompe).
  if (firstKey) document.getElementById(firstKey)?.focus()
}

// true si el objeto de errores tiene al menos una clave.
export function hasErrors(errors) {
  return Object.keys(errors).length > 0
}
