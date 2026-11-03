import Button from '../../components/ui/Button'
import { DEMO_PRODUCTIONS_USED, plans } from '../../data/mockData'
import { formatPrice } from '../../utils/format'

// Paso 1: comparación de planes. Los datos salen de mockData (plans) para que
// el precio y los límites estén en un solo lugar.
function PlanStep({ currentPlan, onContinue }) {
  return (
    <section className="section" aria-label="Planes disponibles">
      <div className="container">
        <ul className="plan-grid">
          {plans.map(plan => {
            const isCurrent = plan.id === currentPlan
            const isPremium = plan.id === 'PREMIUM'
            return (
              <li key={plan.id} className={isPremium ? 'plan-card plan-card-premium' : 'plan-card'}>
                {/* El título va primero y la etiqueta después: nunca hay texto chico arriba de un título. */}
                <div className="plan-head">
                  <h2>{plan.name}</h2>
                  {isCurrent && <span className="plan-badge plan-badge-current">Tu plan actual</span>}
                  {isPremium && !isCurrent && <span className="plan-badge plan-badge-recommended">Recomendado</span>}
                </div>
                <p className="plan-price">
                  {formatPrice(plan.price)} <span>/ mes</span>
                </p>
                <ul className="plan-features">
                  {plan.features.map(feature => (
                    <li key={feature}>{feature}</li>
                  ))}
                  {plan.missing.map(item => (
                    <li key={item} className="plan-missing">{item}</li>
                  ))}
                </ul>
                {plan.id === 'FREE' && isCurrent && (
                  <p className="plan-usage">Usaste {DEMO_PRODUCTIONS_USED} de {plan.productionLimit} producciones.</p>
                )}
                {isPremium && !isCurrent && (
                  <Button onClick={onContinue}>Continuar con Premium</Button>
                )}
              </li>
            )
          })}
        </ul>
        <p className="plan-note">
          Las producciones suman beats y toplines, en total histórico: no bajan si borrás uno.
        </p>
      </div>
    </section>
  )
}

export default PlanStep
