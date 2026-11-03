import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'

// Paso 3: confirmación. Recibe solo la tarjeta ya enmascarada ("•••• 1111"),
// nunca el número completo.
function DoneStep({ maskedCard }) {
  return (
    <section className="section" aria-label="Confirmación">
      <div className="container">
        <Alert kind="success">¡Listo! Tu plan ahora es PREMIUM.</Alert>
        <p>
          Ya podés publicar beats y toplines sin límite. Pagaste con la tarjeta {maskedCard}.
          No guardamos ningún dato de tu tarjeta.
        </p>
        <div className="form-actions">
          <Button to="/beats">Publicar un beat</Button>
          <Button to="/desafios" variant="secondary">Ver desafíos</Button>
        </div>
      </div>
    </section>
  )
}

export default DoneStep
