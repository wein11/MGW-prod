import { Link } from 'react-router-dom'
import PageHeader from '../components/layout/PageHeader'
import './privacy/Privacy.css'

// Política de privacidad. Es obligatoria porque la app pide datos personales
// (email, contraseña, ciudad) y simula un pago con tarjeta. La enlazan el
// footer y los checkbox de Registro, Contacto y Pago.
// Cada tema es un h2 para que se pueda recorrer por encabezados.
function PrivacyPage() {
  return (
    <>
      <PageHeader
        title="Política de privacidad"
        description="Qué datos pedimos, para qué los usamos y qué derechos tenés sobre ellos."
        wave={false}
      />

      <article className="section">
        <div className="container privacy">
          <p>
            <strong>Última actualización:</strong> <time dateTime="2026-11-04">4 de noviembre de 2026</time>
          </p>

          <h2>Proyecto académico</h2>
          <p>
            MGW Prod es un trabajo práctico de la materia Aplicaciones Interactivas de la UADE.
            En esta versión los datos son simulados: no se guardan en ningún servidor y se borran
            al cerrar la pestaña. Igual explicamos cómo los trataríamos en una versión real.
          </p>

          <h2>Responsable y contacto</h2>
          <p>
            El responsable de los datos es el equipo de MGW Prod. Para cualquier consulta sobre
            esta política escribinos desde la página de <Link to="/contacto">Contacto</Link>.
          </p>

          <h2>Qué datos recolectamos</h2>
          <ul>
            <li>Nombre artístico.</li>
            <li>Email.</li>
            <li>Contraseña.</li>
            <li>Ciudad (opcional).</li>
            <li>Mensajes que nos mandás desde Contacto.</li>
            <li>Links de audio de tus beats y toplines.</li>
            <li>
              Datos de tarjeta en el pago de Premium: el pago es <strong>simulado</strong> y los
              datos de la tarjeta <strong>nunca se almacenan</strong>. Solo mostramos los últimos
              cuatro números en la confirmación.
            </li>
          </ul>

          <h2>Para qué los usamos</h2>
          <ul>
            <li>Crear tu cuenta y mantener tu sesión iniciada.</li>
            <li>Mostrar tu nombre en tus beats, comentarios, entregas y en el ranking.</li>
            <li>Responder los mensajes de Contacto.</li>
            <li>Activar el plan Premium.</li>
          </ul>

          <h2>Base legal</h2>
          <p>
            Tratamos tus datos con tu consentimiento, que das al tildar la casilla de aceptación
            en Registro, Contacto y Pago, según la Ley 25.326 de Protección de Datos Personales de
            la República Argentina.
          </p>

          <h2>Con quién se comparten</h2>
          <p>
            No vendemos ni cedemos tus datos. Lo único público es lo que vos publicás (nombre
            artístico, beats, comentarios y puntaje en el ranking).
          </p>

          <h2>Cuánto tiempo se guardan</h2>
          <p>
            Mientras tu cuenta esté activa. Si pedís darla de baja, borramos tus datos
            personales. En esta versión académica la sesión solo guarda tu nombre, tu tipo de
            cuenta y tu plan, y se borra al cerrar la pestaña.
          </p>

          <h2>Tus derechos</h2>
          <p>
            Podés pedir acceso, rectificación o supresión de tus datos en cualquier momento
            desde <Link to="/contacto">Contacto</Link>. La Agencia de Acceso a la Información
            Pública (AAIP) es el órgano de control de la Ley 25.326 y recibe denuncias por
            incumplimientos.
          </p>

          <h2>Seguridad</h2>
          <p>
            Las contraseñas y los datos de tarjeta nunca se guardan en el navegador ni se
            muestran en pantalla. En una versión real las contraseñas se guardarían cifradas y
            la conexión sería siempre por HTTPS.
          </p>

          <h2>Cambios a esta política</h2>
          <p>
            Si cambiamos esta política vamos a actualizar la fecha de arriba. Los cambios
            importantes se avisan en la página de inicio.
          </p>
        </div>
      </article>
    </>
  )
}

export default PrivacyPage
