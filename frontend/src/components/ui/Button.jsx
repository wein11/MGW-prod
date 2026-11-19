import { Link } from 'react-router-dom'
import './Button.css'

// Botón único de la app. Si recibe "to" se dibuja como link de navegación
// (porque lleva a otra página); si no, es un <button> real (porque hace una
// acción). Mezclar los dos confunde a teclado y lectores de pantalla.
// variant: 'primary' (rojo), 'secondary' (borde), 'link' (texto rojo con flecha).
function Button({ to, variant = 'primary', type = 'button', className = '', children, ...rest }) {
  const classes = `btn btn-${variant} ${className}`.trim()

  if (to) {
    return (
      <Link to={to} className={classes} {...rest}>
        {children}
      </Link>
    )
  }

  return (
    <button type={type} className={classes} {...rest}>
      {children}
    </button>
  )
}

export default Button
