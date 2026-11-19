// Simula la demora de un servidor para poder mostrar el estado de espera
// ("Enviando…") en los formularios, aunque no haya backend.
export function fakeRequest(ms = 600) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
