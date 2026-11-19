// ============================================================================
// PARA ESTUDIAR: format.js (cómo se muestran fechas, precios y plurales)
// ----------------------------------------------------------------------------
// QUÉ ES: funciones chicas que transforman un dato en texto para mostrar.
// POR QUÉ EN UN ARCHIVO APARTE: si cada página formateara a su manera, una
//   fecha podría verse "12/10/2026" en una pantalla y "2026-10-12" en otra.
//   Al centralizarlo, se ve igual en toda la app y se cambia en un solo lugar
//   (separación de responsabilidades, criterio 8 de la rúbrica).
// ============================================================================

// Formatos de presentación compartidos, para que fechas y precios se vean
// igual en todas las pantallas.

// '2026-10-12' -> '12/10/2026'. Se parte el string a mano para evitar el
// corrimiento de un día que produce new Date() con zona horaria.
// (new Date('2026-10-12') se interpreta en UTC y en Argentina, UTC-3, daría
// el 11/10 a las 21 h.)
export function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

// 15000 -> '$15.000'. toLocaleString('es-AR') pone el punto de miles.
export function formatPrice(amount) {
  return `$${amount.toLocaleString('es-AR')}`
}

// 1 -> '1 comentario', 3 -> '3 comentarios'
// pluralWord es opcional: por defecto agrega una "s".
export function plural(count, singular, pluralWord = `${singular}s`) {
  return `${count} ${count === 1 ? singular : pluralWord}`
}

// Fecha de hoy en ISO, para lo que se crea durante la demo.
// padStart(2, '0') convierte 5 en '05' para que quede '2026-05-03'.
export function todayIso() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0') // getMonth() va de 0 a 11
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
