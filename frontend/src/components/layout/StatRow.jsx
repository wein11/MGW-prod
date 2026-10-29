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
