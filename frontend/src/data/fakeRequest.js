// ============================================================================
// PARA ESTUDIAR: fakeRequest.js (simula que hay un servidor)
// ----------------------------------------------------------------------------
// QUÉ ES: una función que devuelve una Promise que se resuelve después de
//   `ms` milisegundos. No hace nada más: solo espera.
// PARA QUÉ: la rúbrica pide mostrar el estado de "espera" en los formularios.
//   Sin backend la respuesta sería instantánea y no se vería. Con esto el botón
//   muestra "Enviando…" durante 0,6 segundos, como si hubiera un servidor.
// CÓMO SE USA (en un formulario):
//     setSending(true)          // botón dice "Enviando…" y queda deshabilitado
//     await fakeRequest(600)    // espera 600 ms (async/await)
//     setSending(false)         // vuelve el botón y se muestra el éxito
// CONCEPTOS: Promise, setTimeout, async/await.
// PREGUNTA PROBABLE: "Si se conectara al backend, ¿qué cambia?"
//   → Se reemplaza `await fakeRequest()` por `await fetch('/api/...')`. El
//     resto del formulario (validaciones, estados) queda igual.
// ============================================================================

// Simula la demora de un servidor para poder mostrar el estado de espera
// ("Enviando…") en los formularios, aunque no haya backend.
export function fakeRequest(ms = 600) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
