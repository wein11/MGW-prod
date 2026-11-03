import PageHeader from '../components/layout/PageHeader'
import { useAuth } from '../context/AuthContext'
import { RANKING_TOTAL_PRODUCERS, ranking } from '../data/mockData'
import './ranking/Ranking.css'

// Ranking (wireframe 6). Es una tabla de verdad: tres columnas de datos con
// encabezado, no una lista maquetada. Eso le da a un lector de pantalla el
// "fila 4, puesto 4, productor Luna Sur, puntos 231" sin que lo armemos a mano.
function RankingPage() {
  const { user } = useAuth()
  const leader = ranking[0]

  return (
    <>
      <PageHeader
        title="Ranking"
        description="Los puntos se suman con los votos del jurado en cada desafío."
        stats={[
          { value: RANKING_TOTAL_PRODUCERS, label: 'Productores' },
          { value: leader.points, label: 'Puntos del primero' },
        ]}
      />

      <section className="section" aria-labelledby="producers-title">
        <div className="container">
          <div className="section-head">
            <h2 id="producers-title">Productores</h2>
            <p>Top {ranking.length}</p>
          </div>

          {/* A 375 px la tabla no entra: este contenedor la deja desplazarse
              sola en horizontal, sin que se mueva la página entera. */}
          <div className="table-scroll">
            <table className="ranking-table">
              {/* El caption nombra la tabla para quien no ve el h2 de arriba.
                  Se oculta a la vista para no repetir el título dos veces. */}
              <caption className="visually-hidden">
                Ranking de productores: los {ranking.length} primeros, con su puesto y sus puntos.
              </caption>
              <thead>
                <tr>
                  <th scope="col">Puesto</th>
                  <th scope="col">Productor</th>
                  <th scope="col" className="ranking-points">Puntos</th>
                </tr>
              </thead>
              <tbody>
                {ranking.map(entry => {
                  const isMe = user?.name === entry.name
                  return (
                    <tr key={entry.position} className={isMe ? 'ranking-me' : undefined}>
                      {/* th scope="row": el puesto identifica la fila. */}
                      <th scope="row">{entry.position}</th>
                      <td>
                        {entry.name}
                        {/* "(vos)" va en el texto y no solo en el color de
                            fondo: el resalte no se puede leer en voz alta. */}
                        {isMe && ' (vos)'}
                      </td>
                      <td className="ranking-points">{entry.points}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {!user && (
            <p className="ranking-hint">
              Entrá con tu cuenta para encontrarte más rápido en la tabla.
            </p>
          )}
        </div>
      </section>
    </>
  )
}

export default RankingPage
