import PageHeader from '../components/layout/PageHeader'
import Button from '../components/ui/Button'

// 404: se muestra para cualquier ruta que no existe (Route path="*" en App).
function NotFoundPage() {
  return (
    <PageHeader
      title="Página no encontrada"
      description="La dirección que buscás no existe o cambió. Revisá el link o volvé al inicio."
    >
      <Button to="/">Ir al inicio</Button>
      <Button to="/beats" variant="secondary">Ver beats</Button>
    </PageHeader>
  )
}

export default NotFoundPage
