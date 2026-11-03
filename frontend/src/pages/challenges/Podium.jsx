import { plural } from '../../utils/format'

// Podio del desafío cerrado.
// El HTML va en orden 1, 2, 3 porque es una lista ordenada de verdad: así la
// lee un lector de pantalla y así queda si falla el CSS. El orden visual
// 2 - 1 - 3 (el ganador al medio y más alto) se hace con la propiedad "order"
// de flexbox en Challenges.css, que no toca el orden del documento.
function Podium({ challenge }) {
  return (
    <section className="section" aria-labelledby="podium-title">
      <div className="container">
        <div className="section-head">
          <h2 id="podium-title">Podio</h2>
          <p>{challenge.title} &middot; cerrado</p>
        </div>

        <ol className="podium">
          {challenge.podium.map(place => (
            <li key={place.place} className={`podium-place podium-place-${place.place}`}>
              <p className="podium-name">{place.name}</p>
              <p className="podium-points">{plural(place.points, 'punto')}</p>
              <p className="podium-number" aria-hidden="true">{place.place}</p>
              <span className="visually-hidden">Puesto {place.place}</span>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

export default Podium
