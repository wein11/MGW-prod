import { useEffect, useRef, useState } from 'react'
import { Navigate } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import { useAuth } from '../context/AuthContext'
import { formatPrice } from '../utils/format'
import { plans } from '../data/mockData'
import DoneStep from './premium/DoneStep'
import PaymentStep from './premium/PaymentStep'
import PlanStep from './premium/PlanStep'
import Stepper from './premium/Stepper'
import './premium/Premium.css'

const PREMIUM = plans.find(plan => plan.id === 'PREMIUM')

// Textos del encabezado de cada paso (el h1 cambia con el paso).
const HEADERS = {
  1: {
    title: 'Elegí tu plan',
    description: 'Compará los dos planes y seguí con el que mejor se ajuste a lo que producís.',
    stats: [
      { value: '50', label: 'Producciones en Free' },
      { value: 'Sin límite', label: 'Producciones en Premium' },
    ],
  },
  2: {
    title: 'Datos de pago',
    description: 'Es un pago simulado: no se hace ningún cobro real.',
  },
  3: {
    title: 'Bienvenido a Premium',
    stats: [
      { value: formatPrice(PREMIUM.price), label: 'Por mes' },
      { value: 'Sin límite', label: 'Producciones' },
    ],
  },
}

// Premium en 3 pasos dentro de una sola ruta (/premium): 1 plan, 2 pago,
// 3 confirmación. El paso es estado local; el formulario de pago vive en su
// propio componente para que sus datos se descarten al salir del paso 2.
function PremiumPage() {
  const { user, upgradeToPremium } = useAuth()
  const [step, setStep] = useState(1)
  const [maskedCard, setMaskedCard] = useState('')
  const previousStep = useRef(step)

  // Al cambiar de paso el foco pasa al título: quien usa teclado o lector de
  // pantalla se entera de que la pantalla cambió. (Se compara con el paso
  // anterior para no mover el foco al cargar la página.)
  useEffect(() => {
    if (previousStep.current === step) return
    previousStep.current = step
    const heading = document.querySelector('h1')
    if (heading) {
      heading.tabIndex = -1
      heading.focus()
    }
  }, [step])

  // Sin sesión no se puede pagar: se manda a Login.
  if (!user) return <Navigate to="/login" replace />

  // Igual que en el backend, los planes son solo para artistas: una
  // discográfica no publica producciones, así que no tiene nada que ampliar.
  if (user.role !== 'ARTIST') {
    return (
      <>
        <PageHeader title="Premium" description="El plan Premium amplía las producciones que puede publicar un artista." />
        <section className="section">
          <div className="container">
            <EmptyState title="Premium es solo para artistas.">
              <p>Tu cuenta es de discográfica. Podés seguir los desafíos y el ranking para encontrar productores.</p>
              <Button to="/desafios">Ver desafíos</Button>
            </EmptyState>
          </div>
        </section>
      </>
    )
  }

  function handlePaid(card) {
    upgradeToPremium()
    setMaskedCard(card)
    setStep(3)
  }

  const header = HEADERS[step]
  return (
    <>
      <Stepper current={step} />
      <PageHeader title={header.title} description={header.description} stats={header.stats} />
      {step === 1 && <PlanStep currentPlan={user.plan} onContinue={() => setStep(2)} />}
      {step === 2 && <PaymentStep onBack={() => setStep(1)} onPaid={handlePaid} />}
      {step === 3 && <DoneStep maskedCard={maskedCard} />}
    </>
  )
}

export default PremiumPage
