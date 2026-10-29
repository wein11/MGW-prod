// Id nuevo para lo que se crea en la demo (el siguiente al máximo actual).
export function nextId(items) {
  return items.reduce((max, item) => Math.max(max, item.id), 0) + 1
}
