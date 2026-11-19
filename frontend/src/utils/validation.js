// ============================================================================
// PARA ESTUDIAR: validation.js (validaciones del lado del cliente)
// ----------------------------------------------------------------------------
// QUÉ ES: una función por regla. Cada una recibe un valor y devuelve true
//   (válido) o false (inválido). No muestran nada en pantalla.
// QUIÉN ARMA EL MENSAJE: cada página. Ej. en el Login:
//     if (!isValidEmail(email)) errors.email = 'El email no es válido.'
// POR QUÉ ASÍ: la misma regla (ej. email) se usa en Login, Registro y
//   Contacto. Escrita una vez acá, no se repite (criterio 8: sin duplicación).
//   La rúbrica (criterio 7) evalúa justamente estas validaciones del cliente.
// TRUCO QUE SE REPITE: String(value ?? '').trim()
//   - `?? ''`: si value es null o undefined, usa '' (así no rompe).
//   - String(...): lo pasa a texto por si vino un número.
//   - .trim(): saca espacios al principio y al final.
// REGEX (expresiones regulares) usadas:
//   - /^[^\s@]+@[^\s@]+\.[^\s@]+$/ → "algo@algo.algo" sin espacios.
//   - /^\d+$/ → solo dígitos.  /^\d{16}$/ → exactamente 16 dígitos.
// ============================================================================

// Validaciones del lado del cliente. Son funciones puras (reciben un valor y
// devuelven true/false) para reusarlas en todos los formularios. Los mensajes de error los arma cada página.

// Campo obligatorio: no vacío ni solo espacios.
export function isRequired(value) {
  return String(value ?? '').trim() !== ''
}

// Email con forma usuario@dominio.algo.
export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value ?? '').trim())
}

// BPM: entero entre 40 y 220 (mismo rango que muestra el wireframe).
export function isValidBpm(value) {
  const text = String(value ?? '').trim()
  if (!/^\d+$/.test(text)) return false // rechaza "12.5", "-3", "abc"
  const bpm = Number(text)
  return bpm >= 40 && bpm <= 220
}

// Los links de audio tienen que ser https para no mezclar contenido inseguro.
// new URL(...) tira un error si el texto no es una URL; el try/catch lo
// atrapa y devuelve false.
export function isValidHttpsUrl(value) {
  try {
    const url = new URL(String(value ?? '').trim())
    return url.protocol === 'https:' && url.hostname.includes('.')
  } catch {
    return false
  }
}

// Contraseña: mínimo 8 caracteres (no se hace trim: un espacio cuenta).
export function isValidPassword(value) {
  return String(value ?? '').length >= 8
}

// Saca los espacios: "4111 1111" → "41111111". Es privada (no tiene export).
function digitsOnly(value) {
  return String(value ?? '').replace(/\s/g, '')
}

// Tarjeta: 16 dígitos y algoritmo de Luhn (el dígito verificador que usan
// todas las tarjetas). Así detectamos números mal tipeados sin un servidor.
// Luhn en criollo: de derecha a izquierda, se duplica un dígito sí y uno no;
// si al duplicar da más de 9 se le resta 9; se suma todo y el total tiene que
// terminar en 0 (múltiplo de 10).
export function isValidCardNumber(value) {
  const digits = digitsOnly(value)
  if (!/^\d{16}$/.test(digits)) return false
  let sum = 0
  for (let i = 0; i < digits.length; i++) {
    let digit = Number(digits[digits.length - 1 - i]) // recorre de derecha a izquierda
    if (i % 2 === 1) {
      digit *= 2
      if (digit > 9) digit -= 9
    }
    sum += digit
  }
  return sum % 10 === 0
}

// Vencimiento MM/AA: la tarjeta sirve hasta el último día de ese mes.
// `today` tiene valor por defecto para poder probar con otra fecha.
export function isValidExpiry(value, today = new Date()) {
  const match = /^(\d{2})\/(\d{2})$/.exec(String(value ?? '').trim())
  if (!match) return false
  const month = Number(match[1])
  const year = 2000 + Number(match[2]) // "30" → 2030
  if (month < 1 || month > 12) return false
  // El día 0 del mes siguiente es el último día del mes indicado.
  // (new Date usa meses de 0 a 11, por eso `month` ya es "el mes siguiente".)
  const lastDay = new Date(year, month, 0, 23, 59, 59)
  return lastDay >= today
}

// CVV: 3 o 4 dígitos (American Express usa 4).
export function isValidCvv(value) {
  return /^\d{3,4}$/.test(String(value ?? '').trim())
}

// Solo se muestran los últimos 4 dígitos: el número completo nunca se guarda.
export function maskCard(value) {
  return `•••• ${digitsOnly(value).slice(-4)}`
}
