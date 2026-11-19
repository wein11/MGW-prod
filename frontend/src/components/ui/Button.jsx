// ============================================================================
// PARA ESTUDIAR: Button.jsx (componente reutilizable)
// ----------------------------------------------------------------------------
// QUÉ ES: el único botón de toda la app. Todas las páginas lo usan, así todos
//   los botones se ven y se comportan igual (consistencia visual, criterio 4).
// PROPS (los "parámetros" de un componente):
//   - to: si viene, se dibuja como <Link> (navega a otra página).
//   - variant: 'primary' | 'secondary' | 'link' → cambia la clase CSS.
//   - type: 'button' por defecto. En formularios se pasa type="submit".
//   - children: lo que va entre <Button> y </Button> (el texto).
//   - ...rest: cualquier otra prop (onClick, disabled, aria-label...) pasa
//     tal cual al elemento final. Se llama "spread" de props.
// POR QUÉ LINK O BUTTON: un link lleva a otro lugar; un botón hace una acción.
//   El lector de pantalla los anuncia distinto ("enlace" vs "botón") y el
//   teclado también cambia (Enter activa los dos, Espacio solo el botón).
// PREGUNTA PROBABLE: "¿Por qué type='button' por defecto?"
//   → Porque un <button> dentro de un <form> sin type es "submit" y enviaría
//     el formulario sin querer (por ejemplo, el botón "Cancelar").
// ============================================================================
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
