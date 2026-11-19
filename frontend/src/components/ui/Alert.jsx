// ============================================================================
// PARA ESTUDIAR: Alert.jsx (mensajes de error, éxito o aviso)
// ----------------------------------------------------------------------------
// QUÉ ES: la caja de color que aparece arriba de un formulario o lista para
//   avisar qué pasó ("Listo: tu beat se publicó", "Error: revisá los campos").
// CÓMO SE USA EN LAS PÁGINAS (patrón de toda la app):
//     const [message, setMessage] = useState('')
//     ...al terminar una acción: setMessage('Aceptaste la propuesta.')
//     ...en el JSX: {message && <Alert kind="success">{message}</Alert>}
//   `message && ...` = renderizado condicional: si message está vacío no se
//   dibuja nada; si tiene texto, se dibuja el Alert.
// ACCESIBILIDAD: el atributo role hace que el lector de pantalla lo lea solo,
//   sin que la persona tenga que ir a buscarlo:
//   - role="alert": urgente, interrumpe lo que se está leyendo (errores).
//   - role="status": educado, espera a que termine (éxito, información).
// RETROALIMENTACIÓN (criterio 6 y 7): cada acción le muestra a la persona
//   qué pasó, por eso esto es clave en la rúbrica.
// ============================================================================
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
