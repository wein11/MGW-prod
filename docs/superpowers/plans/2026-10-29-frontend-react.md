# Frontend React de MGW Prod: plan de implementación para 4 personas

> **Para agentes:** sub-skill requerida: superpowers:subagent-driven-development o superpowers:executing-plans. Los pasos usan checkboxes (`- [ ]`).

**Objetivo:** construir en React el frontend de MGW Prod con las 14 pantallas del wireframe, más una Política de privacidad, y aprobar la rúbrica del frontend (mínimo 60/100, apuntamos a 85 o más).

**Arquitectura:** app React + Vite en `frontend/`, en la raíz del repo, con la misma estructura que el repo de clase (`lpueyoUADE/TodoList`): `pages/`, `components/` y `react-router-dom`. Todo funciona con **datos simulados** en memoria (la rúbrica lo permite y no puntúa la conexión al backend). La sesión es simulada con un `AuthContext` y tres roles: visitante, ARTIST y LABEL.

**Stack:** React 19, Vite, react-router-dom 7, CSS propio (sin frameworks), Vitest solo para `utils/validation.js`, oxlint.

**Spec:** `Rubrica Apis Interactivas.pdf` (cátedra) + `docs/wireframes/MGW-Prod-Wireframes.pdf` (14 pantallas) + las decisiones de Santiago del 2026-10-29 (listadas en las restricciones).

---

## Restricciones globales (aplican a TODAS las tareas)

1. **Sin texto chico encima de los títulos.** Ningún título (`h1`, `h2` ni `h3`) lleva un texto chico arriba: nada de "CATALOGO", "PASO 1 DE 3", "TABLA GENERAL", "PLATAFORMA PARA PRODUCTORES EMERGENTES", "BEAT", "PROXIMO EN CERRAR", "TOPLINES Y PROPUESTAS", "ALTA", "01/02/03", "MODIFICAR", "BORRAR", etc. Queda solo el título grande y, debajo, la bajada. `PageHeader` **no tiene** prop `eyebrow` a propósito.
2. **Política de privacidad obligatoria** (manejamos datos sensibles: email, contraseña y tarjeta):
   - Página `/privacidad`, enlazada desde el footer de todas las páginas.
   - En **Registro, Contacto y Pago (Premium paso 2)** hay un checkbox obligatorio: "Leí y acepto la [Política de privacidad]". Sin tildarlo no se envía y aparece un error.
   - Los datos de tarjeta **no se guardan nunca**: ni en `localStorage`/`sessionStorage`, ni en `console.log`, ni en el mock. Al terminar el pago se descartan y en el paso 3 solo se muestra `•••• 1111`.
   - La sesión simulada guarda en `sessionStorage` solo `{ name, role }`. Nunca el email ni la contraseña.
   - Inputs sensibles con `autocomplete` correcto: `cc-name`, `cc-number`, `cc-exp`, `cc-csc`, `new-password`, `current-password`, `email`. CVV y contraseña con `type="password"`.
3. **Las 14 pantallas del wireframe**: 1 Landing (escritorio + celular), 2 Beats, 3 Detalle de beat, 3b Colaboraciones, 4a/4b/4c Premium, 5 Desafíos, 6 Ranking, 7 Nosotros, 8 Contacto (éxito), 9a Login (error), 9b Registro. Más `/privacidad` y una 404.
4. **HTML semántico** (el profe desaprueba si se usan `div` en lugar de `h`/`p`): un solo `h1` por página, encabezados sin saltos de nivel, `header`/`nav`/`main`/`footer`/`section`/`article`, listas con `ul`/`ol`, tablas con `table`/`th scope`.
5. **Imágenes locales** en `frontend/src/assets/`, nunca links de Google. **Sin Google Fonts ni CDNs** (también lo pidió el profe).
6. **Paleta** (tokens en `styles/tokens.css`): `--dark #16110f`, `--light #f2e7df`, `--primary #d8381a`, `--primary-dark #af2a11`, `--accent #f0a020`, `--surface #e8d9ce`, `--muted #6b5a52`, `--border #d8c6bb`, `--success #1f6b3a`.
   - **Contraste:** el texto rojo sobre fondo claro usa `--primary-dark` (≈5,4:1), **no** `--primary` (≈3,8:1, no pasa AA). `--primary` queda para los fondos de botón con texto blanco.
7. **Validaciones** (valores exactos): BPM entero entre 40 y 220 · contraseña de 8 caracteres o más · link de audio que empiece con `https://` · tarjeta de 16 dígitos que pase Luhn · vencimiento `MM/AA` no vencido · CVV de 3 o 4 dígitos · comentario de 1 a 500 caracteres · mensaje de contacto de 10 caracteres o más · puntaje de voto de 1 a 10.
8. **Estados en cada formulario:** error (`role="alert"`, rojo), éxito (`role="status"`, verde) y espera (botón deshabilitado con el texto "Enviando…" durante `fakeRequest(600)`).
9. **Código:** componentes en PascalCase `.jsx`, CSS por componente como en TodoList, comentarios en español explicando el qué y el por qué técnico. Commits en español.
10. **Breakpoints:** celular 375 px, tablet 768 px y escritorio 1280 px. Por debajo de 900 px el nav se colapsa en un botón hamburguesa con `aria-expanded`.

## Foco de revisión (los 5 casos que más pueden fallar delante del profe)

1. **Navegar solo con teclado** (Tab/Shift+Tab/Enter/Espacio): menú hamburguesa, botones de puntaje 1 a 10, confirmación de borrado en dos pasos y tarjetas Artista/Discográfica del registro. Todo tiene que poder usarse y mostrar el foco visible. Lo prueba cada uno en sus pantallas y lo repasa A en la Tarea 6.
2. **Enviar un formulario vacío:** cada campo obligatorio muestra su error al lado (`aria-invalid` + `aria-describedby`) y el foco va al primer campo con error.
3. **Pantalla de 375 px:** sin scroll horizontal. Las filas de beats y colaboraciones apilan los botones y la tabla de ranking no desborda.
4. **Sin sesión o con el rol equivocado:** "Publicar beat" y "Subir topline" solo para ARTIST, "Gestionar mi beat" solo para el dueño, Colaboraciones muestra los estados "sin sesión" y "cuenta discográfica" del wireframe, y Premium sin sesión manda a Login.
5. **Recargar la página (F5) en cualquier ruta:** la sesión simulada sigue (sessionStorage) y una ruta inexistente muestra la 404 con un link al inicio.

---

## Estructura de archivos

```
frontend/
  package.json, vite.config.js, index.html, .oxlintrc.json
  src/
    main.jsx                      # monta <App/> con <AuthProvider>
    App.jsx                       # rutas (Tarea 1)
    styles/tokens.css, global.css # paleta, tipografía, foco, utilidades
    assets/                       # portadas, fotos del equipo (locales)
    data/mockData.js              # beats, users, challenges, ranking, toplines, comments
    data/fakeRequest.js           # fakeRequest(ms) -> Promise que simula la espera
    context/AuthContext.jsx       # sesión simulada
    utils/validation.js (+ .test.js)
    components/layout/  LiveBar, Header, Footer, PageHeader, Waveform, StatRow
    components/ui/      Button, FormField, Alert, StatusBadge, ConfirmDelete, EmptyState
    pages/              una carpeta o archivo por pantalla (Tareas 2 a 5)
```

## Reparto

| Persona | Tareas | Pantallas | Criterios de rúbrica que "lleva" |
|---|---|---|---|
| **A (Santiago)** | 1 (base), 2, 6 (QA final) | Landing escritorio + celular, Nosotros, Política de privacidad, 404 | 2 Estructura, 3 Accesibilidad, 8 Calidad técnica |
| **B (Mateo)** | 3 | Beats, Detalle de beat | 1 Negocio, 6 Interacción (filtros, comentar, editar/borrar) |
| **C (Paolo)** | 4 | Colaboraciones, Desafíos, Ranking | 6 Interacción (aceptar/rechazar, votar, podio) |
| **D (Danilo)** | 5 | Premium 1-2-3, Login, Registro, Contacto | 7 Formularios, validaciones y estados; datos sensibles |

**Cómo se hace sin pisarse:** la Tarea 1 fija los contratos (props de cada componente y forma de los datos simulados). Con eso, **B, C y D arrancan el mismo día**: escriben sus páginas contra esas interfaces aunque A todavía no haya mergeado la base.

**Git:** A crea `front/react` desde `main`. Cada persona trabaja en `front/<letra>-<area>` (por ejemplo `front/b-beats`) y abre un PR a `front/react`. Ninguno mergea su propio PR: lo revisa otro, con el checklist de la Tarea 6.

**Calendario (próxima clase: jueves 05/11, a confirmar):**
- Viernes 30/10: A sube la Tarea 1. B, C y D arrancan.
- Lunes 02/11: todos los PR mergeados.
- Martes 03/11: QA cruzado (Tarea 6).
- Miércoles 04/11: correcciones y ensayo de la demo.

---

### Tarea 1 (A): Base, contratos y kit de componentes

**Archivos:** todo lo de la estructura salvo `pages/`. Las páginas quedan como placeholders con su `h1`.

**Interfaces que produce (el resto las usa tal cual):**
> ✅ **Hecha (2026-10-29).** Lo de abajo es el contrato real implementado. Además del plan original se sumaron: `DataContext`, `PrivacyConsent`, `utils/format.js`, `utils/form.js` y `utils/ids.js`.

- `<PageHeader title: string, description?: string, stats?: {value, label}[], wave?: boolean = true>{acciones}</PageHeader>`: renderiza `h1` + `p` + acciones + `StatRow` + `Waveform`, y pone el título de la pestaña. **Sin eyebrow.**
- `<Button to?: string, variant="primary"|"secondary"|"link", type, disabled, onClick, ref>`: con `to` es un `Link` (navega); sin `to` es un `<button>` (acción).
- `<PrivacyConsent checked: boolean, onChange: (bool) => void, error?: string />`: checkbox con `id="privacy"` y link a `/privacidad`.
- `useData() -> { beats, setBeats, comments, setComments, toplines, setToplines, submissions, setSubmissions }`: estado compartido entre páginas (aceptar un topline en el detalle se ve en Colaboraciones). `challenges`, `ranking` y `plans` se importan directo de `mockData.js` (no cambian).
- `utils/format.js`: `formatDate('2026-10-12') -> '12/10/2026'`, `formatPrice(15000) -> '$15.000'`, `plural(3, 'comentario') -> '3 comentarios'`, `todayIso()`.
- `utils/form.js`: `focusFirstError(errors)` (el id del campo tiene que ser igual a la clave del error) y `hasErrors(errors)`.
- `utils/ids.js`: `nextId(items)` para lo que se crea en la demo.
- `mockData.js` también exporta `GENRES`, `JURY_WEIGHTS`, `PRIZE_POINTS`, `RANKING_TOTAL_PRODUCERS` y `DEMO_PRODUCTIONS_USED`. Un tercer usuario de prueba: `Kira Beats / kira@mgw.com / mgw12345 / ARTIST`.
- `useAuth()` también tiene `upgradeToPremium()`, y `user` incluye `plan: 'FREE' | 'PREMIUM'`. `register({ name, role })`.
- `<FormField id, label, type="text"|"email"|"password"|"textarea"|"select", value, onChange, error?: string, hint?: string, options?: {value,label}[], ...inputProps />`: `label htmlFor`; si hay error, `aria-invalid="true"` + `aria-describedby` apuntando al mensaje.
- `<Alert kind="error"|"success"|"info">children</Alert>`: `role="alert"` para error, `role="status"` para el resto.
- `<StatusBadge status="PENDING"|"ACCEPTED"|"REJECTED"|"OPEN"|"VOTING"|"CLOSED" />`: texto en español ("Pendiente", "Aceptada", …).
- `<ConfirmDelete label: string, onConfirm: () => void />`: botón "Borrar" → "¿Seguro…? Sí, borrar / Cancelar". El foco pasa a "Cancelar".
- `<EmptyState title, children />`
- `useAuth() -> { user: {name, role} | null, login(email, password) -> boolean, register({name, role}), logout() }`
- `fakeRequest(ms = 600) -> Promise<void>`
- `mockData.js` exporta `users`, `beats`, `comments`, `toplines`, `challenges`, `submissions`, `ranking`, `plans`. Forma de un beat: `{ id, title, genre, bpm, key, audioUrl, cover, ownerName, publishedAt, description }`. Forma de un topline: `{ id, beatId, artistName, audioUrl, status, createdAt }`. Usuarios de demo (contraseña de prueba, simulada): `DJ Nova / artista@mgw.com / mgw12345 / ARTIST` y `Sello Sur / discografica@mgw.com / mgw12345 / LABEL`.
- `validation.js`: `isRequired, isValidEmail, isValidBpm, isValidHttpsUrl, isValidPassword, isValidCardNumber, isValidExpiry(value, today), isValidCvv, maskCard`.

- [ ] **Paso 1:** `npm create vite@latest frontend -- --template react`, después `npm i react-router-dom` y `npm i -D vitest oxlint`. Script `"test": "vitest run"`.
- [ ] **Paso 2: tests de validación que fallan** en `utils/validation.test.js`:
  ```js
  expect(isValidBpm('39')).toBe(false); expect(isValidBpm('40')).toBe(true)
  expect(isValidBpm('220')).toBe(true); expect(isValidBpm('300')).toBe(false); expect(isValidBpm('12.5')).toBe(false)
  expect(isValidCardNumber('4111 1111 1111 1111')).toBe(true); expect(isValidCardNumber('4111 1111 1111 1112')).toBe(false)
  expect(isValidExpiry('10/26', new Date(2026, 9, 29))).toBe(true); expect(isValidExpiry('09/26', new Date(2026, 9, 29))).toBe(false); expect(isValidExpiry('13/27', new Date(2026, 9, 29))).toBe(false)
  expect(isValidCvv('12')).toBe(false); expect(isValidCvv('1234')).toBe(true)
  expect(isValidPassword('1234567')).toBe(false); expect(isValidHttpsUrl('http://x.com')).toBe(false)
  expect(maskCard('4111 1111 1111 1111')).toBe('•••• 1111')
  ```
- [ ] **Paso 3:** `npm test` → FALLA ("is not a function").
- [ ] **Paso 4:** implementar `validation.js`. `isValidExpiry`: el vencimiento es válido hasta el último día del mes indicado.
- [ ] **Paso 5:** `npm test` → PASA.
- [ ] **Paso 6:** layout + kit + `AuthContext` + rutas: `/`, `/beats`, `/beats/:id`, `/colaboraciones`, `/desafios`, `/ranking`, `/premium`, `/nosotros`, `/contacto`, `/login`, `/registro`, `/privacidad`, `*`. El Header incluye el skip link "Saltar al contenido" y el link activo con `aria-current="page"`. Login y Registro van sin nav, como en los boards 9a y 9b.
- [ ] **Paso 7: verificación.** `npm run dev`: todas las rutas cargan con Tab visible, el menú hamburguesa abre y cierra con teclado a 375 px y `npm run lint` da 0 errores.
- [ ] **Paso 8:** commit "feat(front): base React, layout y kit de componentes" → PR a `front/react`.

### Tarea 2 (A): Landing, Nosotros, Política de privacidad y 404

- **Landing (boards 1 y 1b):** hero (h1 "Tu música, frente a quien la tiene que escuchar"), botones "Ver beats" y "Crear cuenta", stats, "Qué podés hacer" (3 tarjetas con link), banda del jurado (30/30/40 %) y "Últimos beats" (los 3 primeros de `beats`). En celular todo pasa a una columna.
- **Nosotros:** equipo real (Santiago, Mateo, Paolo, Dani) con fotos locales y `alt` descriptivo.
- **Política de privacidad**, con estas secciones (`h2`): responsable y contacto · qué datos recolectamos (nombre artístico, email, contraseña, ciudad, mensajes de contacto, links de audio, datos de tarjeta **solo simulados y nunca almacenados**) · para qué los usamos · base legal: consentimiento, Ley 25.326 de Protección de Datos Personales · con quién se comparten (no se venden) · cuánto tiempo se guardan · derechos de acceso, rectificación y supresión, y que la **AAIP** es el órgano de control · seguridad · cambios a la política · aclaración "proyecto académico UADE" · fecha de última actualización.
- **404:** h1 "Página no encontrada" + link al inicio.
- [ ] Verificación: cada página tiene un solo h1, el footer enlaza a Nosotros, Contacto y Privacidad, y a 375 px no hay scroll horizontal. Commit + PR.

### Tarea 3 (B): Beats y Detalle de beat

**Consume:** `PageHeader`, `FormField`, `Alert`, `ConfirmDelete`, `useAuth`, `beats`, `comments`, `toplines`, `isValidBpm`, `isValidHttpsUrl`, `isRequired`, `fakeRequest`.

- **Beats (board 2):** formulario "Publicar beat" (título, género, link de audio, BPM, tonalidad), visible solo para ARTIST. Si no hay sesión, muestra un aviso con link a Login. Con un BPM de 300 aparece el error exacto del board: "El BPM tiene que estar entre 40 y 220. Revisalo y volvé a intentar." Al publicar con éxito, el beat se agrega arriba de la lista.
- **Filtros:** género (select), BPM (número) y productor (select), más un botón "Limpiar". El contador "N beats" se actualiza. Si no hay resultados: `EmptyState` "No hay beats con esos filtros".
- **Detalle (board 3):** "← Volver a beats", h1 con el título, metadatos y stats, portada local con `alt`, reproductor (`<audio controls>` o un link "Escuchar" si el audio es externo). **Comentarios**: listar y agregar (1 a 500 caracteres). **Gestionar mi beat** (solo el dueño): editar con mensaje de éxito y borrar con `ConfirmDelete`, que después navega a `/beats`. **Toplines**: lista con `StatusBadge`; el dueño puede Aceptar o Rechazar; un ARTIST puede subir un topline (link `https://`) y ve el mensaje "Listo: topline subido. Se creó una propuesta de colaboración pendiente."
- Si el beat no existe (`/beats/999`): `EmptyState` + link a Beats.
- [ ] Verificación: probar con un visitante, con DJ Nova (dueño) y con otro ARTIST; BPM 300, BPM 140 y formulario vacío; Tab por todos los controles. Commit + PR.

### Tarea 4 (C): Colaboraciones, Desafíos y Ranking

**Consume:** `PageHeader`, `StatusBadge`, `ConfirmDelete`, `Alert`, `FormField`, `useAuth`, `toplines`, `beats`, `challenges`, `submissions`, `ranking`, `fakeRequest`.

- **Colaboraciones (board 3b):** "Propuestas sobre mis beats", agrupadas por beat, con un filtro de estado (Todas, Pendiente, Aceptada, Rechazada) y las acciones Aceptar, Rechazar, Quitar propuesta y Borrar topline (con `ConfirmDelete`). "Mis toplines" permite editar el link (inline, con validación `https://`, Guardar/Cancelar) y borrar. Estados: sin sesión → aviso + botón Entrar; LABEL → "Las colaboraciones son solo para artistas."
- **Desafíos (board 5):** destacado "Mejor beat de trap" (sin el texto chico arriba) con stats, banda del jurado, lista de desafíos con `StatusBadge` y una lista de entregas con "Votar", "Ya votaste" o "Es tu entrega". **Votar** es un `fieldset` con `legend` "Puntaje de 1 a 10" y 10 radios con estilo de botón (no divs), más comentario y "Enviar voto" con estado de espera y el mensaje "Tu voto se registró." Después de votar, esa entrega pasa a "Ya votaste". **Podio** para el desafío cerrado: `ol` con 1°, 2° y 3° (el orden visual 2-1-3 se hace por CSS, no en el HTML).
- **Ranking (board 6):** `table` con `caption`, `th scope="col"` y el usuario actual resaltado con "(vos)". A 375 px la tabla no desborda.
- [ ] Verificación: votar sin elegir puntaje da un error; votar con 8 da éxito; aceptar una propuesta cambia el badge y los contadores del header. Probar como visitante, ARTIST y LABEL. Commit + PR.

### Tarea 5 (D): Premium 1-2-3, Login, Registro y Contacto

**Consume:** `FormField`, `Alert`, `Button`, `useAuth`, `plans`, `fakeRequest` y todas las funciones de `validation.js`.

- **Premium:** stepper `ol` con `aria-current="step"`. Se quita el texto "Paso N de 3" de arriba del título; el stepper ya lo indica.
  - **4a:** comparación Free ($0, hasta 50 producciones) contra Premium ($15.000/mes, sin límite) + "Continuar con Premium". Sin sesión redirige a `/login`.
  - **4b:** formulario de tarjeta (nombre, número, vencimiento MM/AA, CVV) + resumen + checkbox de privacidad + "Pagar $15.000" (con espera) y "Volver". Debajo del h1: "Es un pago simulado: no se hace ningún cobro real."
  - **4c:** confirmación "Ya sos Premium" con `•••• 1111`. El estado del formulario se limpia al salir del paso 4b.
- **Login (board 9a):** email + contraseña; si no coinciden, el error del board ("Email o contraseña incorrectos"). Si salen bien, va al inicio con el nombre en el header. Link a Registro.
- **Registro (board 9b):** nombre artístico, email, contraseña (mín. 8, con el hint visible), tipo de cuenta como `fieldset` con 2 radios estilo tarjeta (Artista/Discográfica), ciudad opcional, checkbox de privacidad y "Crear cuenta" (con espera). Al terminar, inicia la sesión y va al inicio.
- **Contacto (board 8):** nombre, email, motivo (select), mensaje (10 caracteres o más) y checkbox de privacidad. Al enviar: estado de espera y después el éxito del board, reemplazando el formulario por un mensaje y un botón "Enviar otro".
- [ ] Verificación: enviar cada formulario vacío; probar la tarjeta `4111 1111 1111 1112` (error) y la `4111 1111 1111 1111` (éxito); vencimiento `09/26` (vencida); el checkbox sin tildar bloquea el envío; en DevTools → Application no aparece ningún dato de tarjeta ni contraseña. Commit + PR.

### Tarea 6 (A + revisión cruzada): QA final contra la rúbrica

- [ ] `npm test` y `npm run lint` en verde. `npm run build` sin errores.
- [ ] **Lighthouse** (Chrome DevTools) en `/`, `/beats`, `/premium` y `/registro`: **Accesibilidad ≥ 90** en las cuatro.
- [ ] Recorrido de las 14 pantallas a 375, 768 y 1280 px. Revisar que no haya texto chico arriba de ningún título (buscar en el código: no debería existir ninguna clase `eyebrow`/`kicker`).
- [ ] Los 5 casos del "Foco de revisión", uno por uno.
- [ ] Cada persona revisa el PR de otra (B→C, C→D, D→B, B→A) con la tabla de abajo.
- [ ] Ensayo de la demo: el recorrido dura 5 minutos y cada uno presenta sus pantallas.

## Rúbrica → qué la cubre

| Criterio (pts) | Qué lo asegura en este plan |
|---|---|
| 1 Negocio (15) | Las 14 pantallas del wireframe con datos realistas de productores, beats, desafíos y premium |
| 2 Estructura (10) | Rutas claras, un nav único, link activo, breadcrumb "← Volver", 404 |
| 3 Semántica/accesibilidad (15) | Restricción 4, skip link, foco visible, labels, `aria-*`, contraste de la restricción 6, Lighthouse ≥ 90 |
| 4 Diseño (15) | Tokens compartidos, mismo kit en todas las páginas, jerarquía título → bajada sin texto chico |
| 5 Responsive (10) | Breakpoints de la restricción 10, nav hamburguesa, board 1b, tabla sin desbordar |
| 6 Interacción (15) | Filtros, comentar, editar/borrar en dos pasos, aceptar/rechazar, votar, stepper, login/logout simulado |
| 7 Formularios (10) | Restricciones 7 y 8: errores por campo, éxito y espera en todos los formularios |
| 8 Calidad técnica (10) | Componentes reutilizables, datos separados de la vista, validaciones testeadas, lint en 0 |
