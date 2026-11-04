import { useState } from 'react'
import { Link } from 'react-router-dom'
import JuryBand from '../components/layout/JuryBand'
import PageHeader from '../components/layout/PageHeader'
import Alert from '../components/ui/Alert'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import StatusBadge from '../components/ui/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { useData } from '../context/DataContext'
import { JURY_WEIGHTS, PRIZE_POINTS, challenges } from '../data/mockData'
import { formatDate, plural } from '../utils/format'
import Podium from './challenges/Podium'
import VoteForm from './challenges/VoteForm'
import './challenges/Challenges.css'

// Días que faltan para una fecha ISO. Devuelve 0 si ya pasó.
// Se compara a mediodía para que el cambio de huso no corra un día.
function daysUntil(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number)
  const target = new Date(year, month - 1, day, 12)
  const today = new Date()
  today.setHours(12, 0, 0, 0)
  const days = Math.round((target - today) / 86400000)
  return days > 0 ? days : 0
}

// El destacado es el desafío que está en votación, porque es donde hay algo
// para hacer ahora. Si no hay ninguno, el próximo en cerrar de los abiertos.
function pickFeatured(list) {
  const voting = list.find(challenge => challenge.status === 'VOTING')
  if (voting) return voting

  const open = list.filter(challenge => challenge.status === 'OPEN')
  if (open.length === 0) return null
  return open.reduce((soonest, challenge) =>
    challenge.closesAt < soonest.closesAt ? challenge : soonest
  )
}

// Desafíos (wireframe 5): el destacado arriba, la lista de los tres, las
// entregas del destacado con su voto, y el podio del que ya cerró.
function ChallengesPage() {
  const { user } = useAuth()
  const { submissions, setSubmissions } = useData()
  // Cuántos votos se enviaron en esta visita. Sirve de dos cosas: saber si hay
  // que mostrar la confirmación, y como key del Alert, para que al votar dos
  // veces seguidas el mensaje se vuelva a montar y el lector de pantalla lo
  // anuncie de nuevo aunque el texto sea el mismo.
  const [votesSent, setVotesSent] = useState(0)

  const featured = pickFeatured(challenges)
  const closed = challenges.find(challenge => challenge.status === 'CLOSED' && challenge.podium)
  const entries = featured
    ? submissions.filter(submission => submission.challengeId === featured.id)
    : []

  // Quién puede votar qué: hace falta sesión, no haber votado ya y que la
  // entrega no sea propia. El backend real lo vuelve a controlar; acá es para
  // no ofrecer un botón que después rebota.
  function canVote(submission) {
    return Boolean(user) && !submission.votedByMe && submission.producerName !== user.name
  }

  const votableOptions = entries.filter(canVote).map(submission => ({
    value: submission.id,
    label: `${submission.beatTitle}, ${submission.producerName}`,
  }))

  function registerVote(submissionId) {
    setSubmissions(current =>
      current.map(submission =>
        submission.id === submissionId ? { ...submission, votedByMe: true } : submission
      )
    )
    setVotesSent(count => count + 1)
  }

  // Texto de la derecha de cada entrega: o se puede votar, o se explica por qué no.
  function entryAction(submission) {
    if (submission.producerName === user?.name) return <span className="beat-meta">Es tu entrega</span>
    if (submission.votedByMe) return <span className="beat-meta">Ya votaste</span>
    if (!user) {
      return <Link to="/login">Entrá para votar</Link>
    }
    return <a href="#votar">Votar</a>
  }

  if (!featured) {
    return (
      <>
        <PageHeader title="Desafíos" description="Competí con tu beat y sumá puntos." />
        <section className="section">
          <div className="container">
            <EmptyState title="No hay desafíos abiertos">
              <p>Cuando se publique uno nuevo lo vas a ver acá.</p>
            </EmptyState>
          </div>
        </section>
      </>
    )
  }

  const days = daysUntil(featured.closesAt)
  const stats = [
    // Si la fecha ya pasó, "0 días para el cierre" no dice nada útil: en ese
    // caso el dato que importa es en qué etapa está el desafío.
    days > 0
      ? { value: days, label: 'Días para el cierre' }
      : { value: featured.status === 'VOTING' ? 'En votación' : 'Cerrado', label: 'Estado' },
    { value: entries.length, label: 'Entregas' },
    { value: featured.prize, label: '1er premio' },
  ]

  return (
    <>
      <PageHeader
        title={featured.title}
        description={`${featured.genre} / ${featured.bpmRange} / ${featured.rules}`}
        stats={stats}
      >
        <Button to="#entregas">Ver entregas</Button>
      </PageHeader>

      {/* Cómo se reparte el puntaje. Va pegado al destacado porque explica de
          dónde salen los números que se ven arriba. */}
      <JuryBand>
        Al cerrar se reparten {PRIZE_POINTS.join(', ')} puntos. Ganar un desafío verifica tu
        perfil.
      </JuryBand>

      <section className="section" aria-labelledby="challenges-title">
        <div className="container">
          <div className="section-head">
            <h2 id="challenges-title">Desafíos</h2>
            <p>{plural(challenges.length, 'desafío', 'desafíos')}</p>
          </div>

          <ul className="item-list">
            {challenges.map(challenge => (
              <li key={challenge.id} className="topline-row">
                <div>
                  <h3>{challenge.title}</h3>
                  <p className="beat-meta">
                    {challenge.status === 'CLOSED' ? 'Terminó el ' : 'Cierra el '}
                    <time dateTime={challenge.closesAt}>{formatDate(challenge.closesAt)}</time>
                  </p>
                </div>
                <div className="topline-actions">
                  <StatusBadge status={challenge.status} />
                  <span className="beat-meta">{challenge.genre} &middot; {challenge.bpmRange}</span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section" id="entregas" aria-labelledby="entries-title">
        <div className="container">
          <div className="section-head">
            <h2 id="entries-title">Entregas</h2>
            <p>{plural(entries.length, 'entrega')}</p>
          </div>

          {entries.length === 0 ? (
            <p>Todavía no hay entregas en este desafío.</p>
          ) : (
            <ul className="item-list">
              {entries.map(submission => (
                <li key={submission.id} className="topline-row">
                  <div>
                    <h3>{submission.beatTitle}</h3>
                    <p className="beat-meta">por {submission.producerName}</p>
                  </div>
                  <div className="topline-actions">
                    <span className="beat-meta">{submission.bpm} BPM</span>
                    {entryAction(submission)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="section" id="votar" aria-labelledby="vote-title">
        <div className="container">
          <div className="section-head">
            <h2 id="vote-title">Votar</h2>
            <p>{plural(votableOptions.length, 'entrega')} para votar</p>
          </div>

          {votesSent > 0 && (
            <Alert key={votesSent} kind="success">Tu voto se registró.</Alert>
          )}

          {!user && (
            <EmptyState title="Entrá para votar">
              <p>El voto de la comunidad pesa el {JURY_WEIGHTS.community}% del puntaje final.</p>
              <Button to="/login">Entrar</Button>
            </EmptyState>
          )}

          {user && votableOptions.length === 0 && (
            <EmptyState title="No te queda ninguna entrega por votar">
              <p>Ya votaste todas las entregas que no son tuyas en este desafío.</p>
            </EmptyState>
          )}

          {user && votableOptions.length > 0 && (
            // key: al votar, la entrega sale de la lista y el formulario tiene
            // que volver a arrancar con la primera opción que quede.
            <VoteForm
              key={votableOptions.map(option => option.value).join('-')}
              options={votableOptions}
              onVote={registerVote}
            />
          )}
        </div>
      </section>

      {closed && <Podium challenge={closed} />}
    </>
  )
}

export default ChallengesPage
