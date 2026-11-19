import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import studioImage from '../assets/about/studio.svg'
import './about/About.css'

// Integrantes reales del grupo y el módulo del backend que armó cada uno.
const TEAM = [
  { name: 'Santiago Weinbinder', initials: 'SW', role: 'Usuarios y autenticación · Catálogo de beats' },
  { name: 'Mateo Galluzzo', initials: 'MG', role: 'Catálogo de beats' },
  { name: 'Paolo Maffei', initials: 'PM', role: 'Desafíos, votación y ranking' },
  { name: 'Danilo Gariboldi', initials: 'DG', role: 'Colaboraciones, toplines y Premium' },
]

// Nosotros (board 7 del wireframe): qué es MGW Prod y quiénes lo hacen.
function AboutPage() {
  return (
    <>
      <PageHeader
        title="Nosotros"
        description="MGW Prod es una plataforma web donde productores y artistas musicales emergentes publican sus beats, compiten en desafíos votados por un jurado y suman puntos en un ranking."
      />

      <section className="section" aria-labelledby="what-title">
        <div className="container about-intro">
          <div>
            <h2 id="what-title">Qué hacemos</h2>
            <p>
              Nació para que quien recién empieza tenga un lugar donde mostrar su trabajo y
              recibir devoluciones reales de un jurado.
            </p>
            <p>
              Cuando un productor arranca, su música suele quedar perdida entre millones de
              publicaciones. En MGW Prod cada beat tiene su ficha, cualquier artista puede
              sumarle una voz (un topline) y los desafíos semanales hacen que alguien escuche
              y puntúe lo que hiciste.
            </p>
            <Button to="/contacto" variant="secondary">Escribinos</Button>
          </div>
          <img
            src={studioImage}
            alt="Ilustración de una consola de mezcla con una onda de audio en pantalla"
            width="640"
            height="360"
          />
        </div>
      </section>

      <section className="section" aria-labelledby="team-title">
        <div className="container">
          <h2 id="team-title">Integrantes</h2>
          <ul className="team-list">
            {TEAM.map(member => (
              <li key={member.name}>
                {/* Las iniciales son decorativas: el nombre ya está escrito
                    abajo, así que el lector de pantalla no las repite. */}
                <span className="team-avatar" aria-hidden="true">{member.initials}</span>
                <h3>{member.name}</h3>
                <p>{member.role}</p>
              </li>
            ))}
          </ul>
          <p className="about-note">
            Proyecto académico de la materia Aplicaciones Interactivas (UADE).
          </p>
        </div>
      </section>
    </>
  )
}

export default AboutPage
