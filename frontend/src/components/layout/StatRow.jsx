// ============================================================================
// PARA ESTUDIAR: StatRow.jsx (fila de números grandes)
// ----------------------------------------------------------------------------
// QUÉ ES: los números destacados de los encabezados ("8 beats publicados").
// PROP stats: un array como [{ value: 8, label: 'Beats publicados' }].
// .map() recorre el array y devuelve un elemento por cada dato.
// key={stat.label}: React necesita una key única en cada elemento de una
//   lista para saber cuál cambió, cuál se agregó y cuál se borró.
// SEMÁNTICA: <dl> (lista de definiciones) con <dt> (término) y <dd> (valor):
//   es la etiqueta HTML correcta para pares nombre-valor.
// ============================================================================
import './StatRow.css'

// Fila de números grandes con su etiqueta (ej. "128 / beats publicados").
// Se usa <dl>: cada dato es un par término-valor. El orden visual (número
// arriba) se logra con CSS, sin romper el orden semántico dt -> dd.
function StatRow({ stats }) {
  return (
    <dl className="stat-row">
      {stats.map(stat => (
        <div key={stat.label} className="stat">
          <dt className="label-caps">{stat.label}</dt>
          <dd>{stat.value}</dd>
        </div>
      ))}
    </dl>
  )
}

export default StatRow
