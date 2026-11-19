// Validaciones del lado del cliente. Son funciones puras (reciben un valor y
// devuelven true/false) para poder testearlas sin React y reusarlas en todos
// los formularios. Los mensajes de error los arma cada página.

export function isRequired(value) {
  return String(value ?? '').trim() !== ''
}

export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value ?? '').trim())
}

// BPM: entero entre 40 y 220 (mismo rango que muestra el wireframe).
export function isValidBpm(value) {
  const text = String(value ?? '').trim()
  if (!/^\d+$/.test(text)) return false
  const bpm = Number(text)
  return bpm >= 40 && bpm <= 220
}

// Los links de audio tienen que ser https para no mezclar contenido inseguro.
export function isValidHttpsUrl(value) {
  try {
    const url = new URL(String(value ?? '').trim())
    return url.protocol === 'https:' && url.hostname.includes('.')
  } catch {
    return false
  }
}

export function isValidPassword(value) {
  return String(value ?? '').length >= 8
}

function digitsOnly(value) {
  return String(value ?? '').replace(/\s/g, '')
}

// Tarjeta: 16 dígitos y algoritmo de Luhn (el dígito verificador que usan
// todas las tarjetas). Así detectamos números mal tipeados sin un servidor.
export function isValidCardNumber(value) {
  const digits = digitsOnly(value)
  if (!/^\d{16}$/.test(digits)) return false
  let sum = 0
  for (let i = 0; i < digits.length; i++) {
    let digit = Number(digits[digits.length - 1 - i])
    if (i % 2 === 1) {
      digit *= 2
      if (digit > 9) digit -= 9
    }
    sum += digit
  }
  return sum % 10 === 0
}

// Vencimiento MM/AA: la tarjeta sirve hasta el último día de ese mes.
export function isValidExpiry(value, today = new Date()) {
  const match = /^(\d{2})\/(\d{2})$/.exec(String(value ?? '').trim())
  if (!match) return false
  const month = Number(match[1])
  const year = 2000 + Number(match[2])
  if (month < 1 || month > 12) return false
  // El día 0 del mes siguiente es el último día del mes indicado.
  const lastDay = new Date(year, month, 0, 23, 59, 59)
  return lastDay >= today
}

export function isValidCvv(value) {
  return /^\d{3,4}$/.test(String(value ?? '').trim())
}

// Solo se muestran los últimos 4 dígitos: el número completo nunca se guarda.
export function maskCard(value) {
  return `•••• ${digitsOnly(value).slice(-4)}`
}
