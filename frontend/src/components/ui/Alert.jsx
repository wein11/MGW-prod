import './Alert.css'

const TITLES = { error: 'Error', success: 'Listo', info: 'Aviso' }

// Mensaje de estado. role="alert" interrumpe al lector de pantalla (errores);
// role="status" avisa sin interrumpir (éxito o información).
// kind: 'error' | 'success' | 'info'
function Alert({ kind = 'info', children }) {
  return (
    <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <strong className="alert-title">{TITLES[kind]}</strong>
      <span>{children}</span>
    </div>
  )
}

export default Alert
