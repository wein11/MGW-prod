// Formatos de presentación compartidos, para que fechas y precios se vean
// igual en todas las pantallas.

// '2026-10-12' -> '12/10/2026'. Se parte el string a mano para evitar el
// corrimiento de un día que produce new Date() con zona horaria.
export function formatDate(isoDate) {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

// 15000 -> '$15.000'
export function formatPrice(amount) {
  return `$${amount.toLocaleString('es-AR')}`
}

// 1 -> '1 comentario', 3 -> '3 comentarios'
export function plural(count, singular, pluralWord = `${singular}s`) {
  return `${count} ${count === 1 ? singular : pluralWord}`
}

// Fecha de hoy en ISO, para lo que se crea durante la demo.
export function todayIso() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
