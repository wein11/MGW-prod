// ============================================================================
// PARA ESTUDIAR: ids.js
// ----------------------------------------------------------------------------
// QUÉ ES: genera el id de algo nuevo (un beat, un comentario, un topline).
// CÓMO: recorre la lista con reduce, se queda con el id más alto y le suma 1.
//   Ejemplo: ids [1, 2, 5] → devuelve 6.
// POR QUÉ: en una base de datos el id lo pone el servidor (autoincremental).
//   Como acá no hay servidor, lo calculamos nosotros. Además React necesita
//   un id único para la prop `key` cuando dibuja listas con .map().
// ============================================================================

// Id nuevo para lo que se crea en la demo (el siguiente al máximo actual).
export function nextId(items) {
  return items.reduce((max, item) => Math.max(max, item.id), 0) + 1
}
