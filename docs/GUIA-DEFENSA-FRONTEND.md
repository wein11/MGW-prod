# mgw-prod — Guía de defensa del frontend

Esta guía explica **todo** el frontend (carpeta `frontend/`): cada carpeta, cada archivo, cada
componente, cada hook y cada decisión, cruzado con los **8 criterios de la rúbrica** del profe.
Es la versión "front" de `GUIA-CODIGO-COMPLETA.md`.

**Cómo leerla:**

- Primera vez: leé en orden las secciones 1 a 5. Ahí está el mapa mental.
- Para defender tus pantallas: andá a tu parte en la sección 7 y después a la 9 (preguntas).
- Si no entendés un hook o una palabra: buscalo en la sección 6 (diccionario) o en la 12.
- La noche antes: sección 8 (rúbrica con evidencia), 9 (preguntas) y 10 (puntos flojos).

**Índice**

1. Qué es el frontend y qué pide la consigna
2. La rúbrica del profe (los 8 criterios)
3. Cómo correrlo y usuarios de prueba
4. Carpetas y archivos
5. Cómo viaja un click (de la pantalla al estado y de vuelta)
6. Diccionario de React: los conceptos que usamos
7. El código, parte por parte (A, B, C, D)
8. La rúbrica criterio por criterio, con dónde está la evidencia
9. Preguntas probables del profesor (y cómo responder)
10. Puntos flojos conocidos (decirlos antes de que los encuentre él)
11. Guion de demo (10 minutos)
12. Glosario

---

## 1. Qué es el frontend y qué pide la consigna

Es la **Etapa 2** del TPO: la interfaz web de MGW Prod, la plataforma donde productores y
artistas publican beats, suben toplines, compiten en desafíos y suben en un ranking.

Está hecho con **React 19 + Vite + react-router-dom 7**, con **CSS propio** (sin Bootstrap ni
Tailwind) y **datos simulados**: no se conecta al backend de la Etapa 1. La rúbrica lo permite
de forma explícita: *"En esta instancia no es obligatorio conectar el frontend con el backend.
Se permite trabajar con datos estáticos, locales o simulados."*

### La consigna (PDF "Aplicaciones Interactivas - Frontend") y dónde la cumplimos

| Pide la consigna | Dónde está en nuestro front |
|---|---|
| Landing Page con barra de navegación | `/` (`HomePage`) + `Header` en todas las páginas |
| Productos: listado | `/beats` (`BeatsPage`) con filtros |
| Productos: detalle | `/beats/:id` (`BeatDetailPage`) |
| Productos: borrado | "Borrar beat" en el detalle, con confirmación en 2 pasos (`ManageBeat` + `ConfirmDelete`) |
| Productos: modificación | "Editar datos" en el detalle (`ManageBeat`) |
| Flujo de compra simulado | `/premium`: 3 pasos Plan → Pago → Listo |
| Acerca de, con los integrantes | `/nosotros` (`AboutPage`) con los 4 integrantes |
| Diseño de pantallas previo (Figma o similar) | `docs/wireframes/` (PDF + HTML, entregado en la Etapa 2) |

**Por qué el "producto" es un beat y la "compra" es Premium:** el profe autorizó el pivot
(2026-09-01): no somos un e-commerce. El producto de la plataforma es el beat (se lista, se ve,
se edita, se borra) y lo único que se "compra" es la suscripción Premium. Si pregunta, decilo
así de directo.

### Extras que suman (no los pide la consigna pero la rúbrica los premia)

Además de lo mínimo hay: Colaboraciones (aceptar/rechazar toplines), Desafíos con votación,
Ranking, Login, Registro, Contacto, Política de privacidad y una página 404. Son **13 rutas**.

---

## 2. La rúbrica del profe (los 8 criterios)

Archivo: `Rubrica Apis Interactivas.pdf`. **Total 100, se aprueba con 60.**

| N.º | Criterio | Puntos | En una frase: qué mira |
|---|---|---|---|
| 1 | Adecuación al modelo de negocios | 15 | ¿La interfaz representa el negocio y sus usuarios? |
| 2 | Estructura y arquitectura de la interfaz | 10 | ¿Las páginas y la navegación están ordenadas? |
| 3 | Semántica y accesibilidad | 15 | Etiquetas HTML correctas, títulos en orden, labels, contraste, `alt`, teclado |
| 4 | Diseño visual y usabilidad | 15 | Consistencia, legibilidad, jerarquía visual |
| 5 | Adaptabilidad a distintos dispositivos | 10 | Celular, tablet y escritorio sin desbordes |
| 6 | Interacción y comportamiento | 15 | Botones, filtros, menús responden y dan feedback |
| 7 | Formularios, validaciones y estados | 10 | Labels, validación del lado del cliente, mensajes de error/éxito/espera |
| 8 | Calidad técnica del frontend | 10 | Código ordenado, legible, sin repetición |

**Reglas generales de la rúbrica que nos favorecen:**
- No se evalúa la conexión con el backend ni la persistencia.
- No dan puntos extra por usar React: hay que justificarlo por orden, no por moda.
- **"Los controles deben responder de acuerdo con la funcionalidad que el frontend declara
  ofrecer."** Esto es clave: cualquier botón que no hace nada resta en el criterio 6. Ver
  sección 10.

---

## 3. Cómo correrlo y usuarios de prueba

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run lint     # oxlint
npm run build    # versión final en dist/
```

| Nombre | Email | Contraseña | Rol |
|---|---|---|---|
| DJ Nova | artista@mgw.com | mgw12345 | ARTIST (dueño de "Noche en Palermo") |
| Kira Beats | kira@mgw.com | mgw12345 | ARTIST |
| Sello Sur | discografica@mgw.com | mgw12345 | LABEL (discográfica) |

Hay **3 tipos de visitante** y la interfaz cambia según cuál sos: sin sesión, ARTIST y LABEL.
Eso es parte de "adecuación al negocio" (criterio 1): cada usuario ve lo que le sirve.

---

## 4. Carpetas y archivos

```
frontend/
├── index.html              ← la única página HTML; React dibuja todo adentro de <div id="root">
├── package.json            ← dependencias (react, react-dom, react-router-dom) y scripts
├── vite.config.js          ← Vite con el plugin de React
├── vercel.json             ← todas las rutas van a index.html (para el deploy del SPA)
└── src/
    ├── main.jsx            ← punto de entrada: monta <App /> dentro de <AppProvider>
    ├── App.jsx             ← las 13 rutas
    ├── styles/
    │   ├── tokens.css      ← colores, fuentes y espacios (variables CSS)
    │   └── global.css      ← estilos base, foco visible, .visually-hidden, .container
    ├── context/
    │   └── AppContext.jsx  ← estado compartido: sesión simulada (user, login, logout…) y
    │                         beats, comentarios, toplines y entregas
    ├── data/
    │   ├── mockData.js     ← todos los datos simulados (usuarios, beats, desafíos, ranking, planes)
    │   └── fakeRequest.js  ← simula la demora de un servidor (600 ms)
    ├── utils/
    │   ├── validation.js   ← validaciones puras (email, BPM, URL, tarjeta con Luhn, etc.)
    │   ├── form.js         ← focusFirstError y hasErrors
    │   ├── format.js       ← formatDate, formatPrice, plural, todayIso
    │   └── ids.js          ← nextId (id nuevo para lo que se crea en la demo)
    ├── components/
    │   ├── layout/         ← Layout, AuthLayout, Header, Footer, Logo, PageHeader, StatRow,
    │   │                     LiveBar, JuryBand, Waveform (+ su .css)
    │   └── ui/             ← Button, FormField, Alert, StatusBadge, ConfirmDelete,
    │                         EmptyState, PrivacyConsent (+ su .css)
    ├── pages/              ← una página por ruta; los bloques grandes en subcarpetas
    │   ├── HomePage, AboutPage, PrivacyPage, NotFoundPage           (A: Santiago)
    │   ├── BeatsPage, BeatDetailPage + beats/                       (B: Mateo)
    │   ├── CollaborationsPage + collaborations/, ChallengesPage +
    │   │   challenges/, RankingPage + ranking/                      (C: Paolo)
    │   └── PremiumPage + premium/, LoginPage, RegisterPage,
    │       ContactPage                                              (D: Dani)
    └── assets/             ← portadas (covers/*.svg) e ilustración de Nosotros (SVG locales)
```

**Por qué esta estructura:** es la misma del repo de clase (`lpueyoUADE/TodoList`): `pages/`,
`components/` y `react-router-dom`. Separamos `layout/` (lo que arma el esqueleto de la página)
de `ui/` (piezas chicas reusables). Cada página trae su propio `.css` al lado, así quien toca
una pantalla no pisa a otro.

**Por qué imágenes SVG locales y fuentes del sistema:** el profe pidió no depender de
recursos externos (nada de imágenes de Google ni Google Fonts).

---

## 5. Cómo viaja un click (de la pantalla al estado y de vuelta)

Ejemplo: DJ Nova publica un beat en `/beats`.

```
Usuario escribe en "Título"
  │  <input onChange> → FormField llama onChange(event.target.value)
  ▼
PublishBeatForm.setField('title')(valor)
  │  setForm(current => ({ ...current, title: valor }))   ← estado LOCAL del formulario
  ▼
React vuelve a dibujar el formulario con el valor nuevo (input "controlado")

Usuario toca "Publicar beat"
  │  <form onSubmit={handleSubmit}>
  ▼
handleSubmit
  │  1. event.preventDefault()          → que el navegador no recargue la página
  │  2. validate()                      → arma { title: 'Ingresá el título…', ... }
  │  3. si hay errores: setErrors + focusFirstError → el foco va al primer campo mal
  │  4. si está todo bien: setSending(true) → el botón dice "Enviando…" y se deshabilita
  │  5. await fakeRequest(600)          → simula que el servidor tarda
  │  6. setBeats(current => [newBeat, ...current])   ← estado GLOBAL (AppContext)
  │  7. setPublished(título) → aparece el Alert verde "Tu beat … ya está publicado"
  ▼
Como `beats` vive en AppContext, se actualizan solas TODAS las pantallas que lo leen:
la lista de /beats, el contador "Beats publicados" de la Landing y la franja "En vivo".
```

**La idea central para defender:** hay dos tipos de estado.

| Estado | Dónde vive | Ejemplos | Por qué ahí |
|---|---|---|---|
| Local (`useState` en el componente) | Adentro de cada formulario/página | Lo que escribís, los errores, "Enviando…", el paso de Premium, los filtros | Solo le importa a esa pantalla |
| Global (Context) | `AppContext`, arriba de todas las rutas | Quién está logueado; beats, comentarios, toplines, entregas | Lo leen varias pantallas: si acepto un topline en el detalle del beat, tiene que verse en Colaboraciones |

---

## 6. Diccionario de React: los conceptos que usamos

**Componente:** una función que devuelve JSX (algo parecido a HTML). Ej.: `function Button() {}`.
Se usa como etiqueta: `<Button>`.

**JSX:** la sintaxis tipo HTML dentro de JS. Diferencias que hay que saber: `className` en vez
de `class`, `htmlFor` en vez de `for`, `{ }` para meter JavaScript.

**Props:** los parámetros que un componente recibe del padre. Son de solo lectura.
Ej.: `<StatusBadge status="PENDING" />` → adentro, `status` vale `'PENDING'`.

**`children`:** la prop especial con lo que va *entre* las etiquetas.
Ej.: `<Alert kind="success">Tu voto se registró.</Alert>` → `children` es el texto.

**Estado (`useState`):** un dato que, cuando cambia, hace que React vuelva a dibujar el
componente. `const [step, setStep] = useState(1)`. **Props vs estado:** las props vienen de
afuera y no se cambian; el estado es propio y se cambia con su setter.

**Setter con función (`setX(current => ...)`):** usamos siempre esta forma cuando el nuevo
valor depende del anterior (agregar, editar, borrar de una lista). Así React nos da el valor
más reciente aunque haya varios cambios seguidos.

**Inmutabilidad:** nunca hacemos `beats.push(...)`. Creamos una lista nueva:
`[nuevo, ...current]` (agregar), `current.map(...)` (editar), `current.filter(...)` (borrar).
React compara referencias: si es la misma lista, no se entera de que cambió.

**`useEffect`:** código que corre *después* de dibujar. Lo usamos para cosas "de afuera de
React": cambiar `document.title` (`PageHeader`) y hacer scroll arriba al cambiar de página
(`Layout`). Son los únicos dos. El array del final (`[title]`) dice cuándo repetirlo: solo si
ese valor cambió.

**Context (`createContext` + `Provider` + `useContext`):** compartir datos con muchos
componentes sin pasarlos prop por prop. Tenemos **uno solo**, `AppContext`: `AppProvider`
envuelve la app y cualquier página hace `const { user, beats } = useApp()`. `useApp()` es
solo `useContext(AppContext)` con un nombre más corto.

**Renderizado condicional:** `{user && <Algo />}` o `cond ? <A /> : <B />`. Ej.: si no hay
sesión, el formulario de publicar se reemplaza por "Entrá con tu cuenta".

**Listas y `key`:** `beats.map(beat => <li key={beat.id}>…)`. La `key` le dice a React qué
elemento es cuál entre renders. Tiene que ser única y estable (usamos el `id`, no el índice).

**`key` para reiniciar un componente:** si cambia la `key`, React tira el componente y crea
uno nuevo con estado limpio. Lo usamos a propósito en `VoteForm` (al votar, el formulario
arranca de cero) y en `ManageBeat` (si cambia el beat, el formulario carga sus datos).

**Formulario controlado:** el `value` del input sale del estado y cada tecla pasa por
`onChange`. React es la única fuente de verdad del valor.

**Virtual DOM:** React arma en memoria cómo debería verse la pantalla, la compara con la
anterior y toca en el DOM real solo lo que cambió. (Teórico, lo preguntan.)

**react-router-dom:**
- `BrowserRouter` / `Routes` / `Route`: qué componente va en cada URL.
- `Outlet`: el "hueco" del layout donde se dibuja la página de la ruta actual.
- `Link` / `NavLink`: links internos sin recargar la página. `NavLink` agrega
  `aria-current="page"` al link activo.
- `useParams`: lee `:id` de `/beats/:id`.
- `useNavigate`: navegar desde código (después de borrar un beat, ir a `/beats`).
- `useLocation`: la URL actual (y el `state` que mandamos al navegar).
- `<Navigate to="/" replace />`: redirigir al dibujar (si ya estás logueado y entrás a `/login`).

**SPA (Single Page Application):** hay un solo `index.html`. Cambiar de página no pide otro
HTML al servidor: el router cambia el componente. Por eso `vercel.json` manda todas las rutas
a `index.html`.

**`StrictMode`:** en desarrollo React ejecuta dos veces algunos efectos para detectar bugs.
No afecta la versión final.

---

## 7. El código, parte por parte

Reparto: **A** Santiago · **B** Mateo · **C** Paolo · **D** Dani. Cada uno defiende su parte,
pero todos tienen que saber las secciones 5 y 6 y la "base" (7.0).

### 7.0 La base (la usa todo el mundo)

**`main.jsx`** monta la app: `<StrictMode><AppProvider><App/>`. El provider va afuera de
todo para que cualquier página pueda usar `useApp()`.

**`App.jsx`** define las rutas. Dos layouts:
- `<Layout>` (con menú, barra "En vivo" y footer) para casi todo, incluida la 404 (`path="*"`).
- `<AuthLayout>` (sin menú) para Login y Registro, como en el wireframe: que la persona se
  concentre en el formulario.

**`AppContext.jsx`** (todo el estado compartido, en un solo lugar):
- 5 `useState`: `user` (arranca en `null`), y `beats`, `comments`, `toplines`, `submissions`
  (arrancan con `mockData`). Expone los valores y sus setters; cada página arma su lógica con
  esos setters (igual que el TodoList de clase).
- `user` = `{ name, role, plan }` o `null` si no hay sesión.
- `login(email, password)`: busca en `users` de `mockData`; si coincide hace `setUser` y
  devuelve `true`.
- `register`, `upgradeToPremium`, `logout`: cada uno es un `setUser`.
- **Decisión de seguridad:** de la sesión se guarda solo nombre, rol y plan. **Nunca** email
  ni contraseña.
- Todo vive en memoria: con un F5 la app vuelve a los datos de demo y sin sesión. La rúbrica
  no pide persistencia.

**`mockData.js`**: los datos de prueba. Los mismos ejemplos que el wireframe, para que cada
pantalla se vea igual que el diseño. Constantes importantes: `GENRES`, `TOPLINE_STATUS`,
`JURY_WEIGHTS` (30/30/40), `PRIZE_POINTS` (500/300/150), `plans` (Free / Premium $15.000).

**`fakeRequest(ms)`**: `new Promise(resolve => setTimeout(resolve, ms))`. Sirve para mostrar
el estado **"Enviando…"** (criterio 7 pide estados de espera) aunque no haya servidor.

**`validation.js`**: funciones **puras** (reciben un valor, devuelven `true/false`), sin React,
así se reusan en todos los formularios. `isRequired`, `isValidEmail` (regex), `isValidBpm` (entero
40-220), `isValidHttpsUrl` (usa `new URL()` y exige `https:`), `isValidPassword` (8+),
`isValidCardNumber` (16 dígitos + **algoritmo de Luhn**), `isValidExpiry` (MM/AA, vale hasta
el último día del mes), `isValidCvv` (3 o 4), `maskCard` (`•••• 1111`).

**`form.js`**: `hasErrors(errors)` y `focusFirstError(errors)`: lleva el foco al primer campo
con error. Truco: **el `id` de cada campo es igual a su clave en el objeto `errors`**, así
`document.getElementById(primeraClave)` lo encuentra.

**Componentes de layout:**
- `Layout`: link **"Saltar al contenido"** (primer elemento con Tab), `LiveBar`, `Header`,
  `<main id="main">` con el `Outlet`, `Footer`. Un `useEffect` hace scroll arriba al cambiar
  de ruta.
- `Header`: `<nav aria-label="Principal">` con `NavLink`s armados con un `map` sobre
  `NAV_LINKS`. En celular, botón **"Menú"** con `aria-expanded` y `aria-controls`. Muestra
  "Entrar" o "Nombre + Salir" según la sesión.
- `PageHeader`: el **único `<h1>`** de cada página + descripción + números (`StatRow`) +
  acciones. También cambia el título de la pestaña con `useEffect`.
- `StatRow` y `JuryBand`: usan `<dl>/<dt>/<dd>` (lista de término-valor) porque son pares
  "etiqueta → número".
- `Waveform`: onda decorativa, `aria-hidden="true"` (el lector de pantalla la ignora).
- `Footer`: links a Nosotros, Contacto y Política de privacidad.

**Componentes de UI:**
- `Button`: si recibe `to` es un `Link` (lleva a otra página); si no, es un `<button>` (hace
  una acción). Mezclar los dos confunde a teclado y lectores de pantalla.
- `FormField`: label + input/textarea/select + ayuda + error. Conecta `label htmlFor`,
  `aria-invalid` y `aria-describedby` (el lector de pantalla lee el error y la ayuda).
  **Es la pieza que más repetición evita:** todos los formularios la usan.
- `Alert`: `role="alert"` para errores (interrumpe al lector), `role="status"` para éxito.
  Es HTML, no React: el lector de pantalla lee el mensaje solo cuando aparece. Por eso cada
  acción (aceptar, rechazar, borrar, guardar) muestra un `Alert` de confirmación.
- `StatusBadge`: el estado **escrito** ("Pendiente"), no solo con color.
- `ConfirmDelete`: borrado en 2 pasos. "Cancelar" tiene el atributo HTML **`autoFocus`**: al
  aparecer la pregunta recibe el foco (la opción segura), así un Enter accidental no borra.
- `EmptyState`: mensaje cuando no hay nada para mostrar o no tenés permiso.
- `PrivacyConsent`: checkbox obligatorio con link a la Política, en Registro, Contacto y Pago.

### 7.1 Parte A (Santiago): Landing, Nosotros, Privacidad, 404

**`HomePage` (`/`)** — la Landing.
- `PageHeader` con el mensaje principal y 3 números calculados de los datos
  (`beats.length`, desafíos no cerrados, productores). Coinciden con la franja "En vivo".
- Botón "Crear cuenta" solo si **no** hay sesión (si hay, muestra "Ver desafíos").
- "Qué podés hacer": 3 tarjetas desde el array `FEATURES` con un solo `map`.
- `JuryBand` (cómo se reparte el puntaje 30/30/40).
- "Últimos beats": `beats.slice(0, 3)` (los nuevos se agregan al principio).
- Links "Ver detalle" con texto oculto (`visually-hidden`) "de Noche en Palermo": si no, el
  lector de pantalla oye 3 links iguales sin saber a qué beat van.

**`AboutPage` (`/nosotros`)** — cumple "Acerca de": los 4 integrantes desde el array `TEAM`,
con su módulo del backend. Iniciales con `aria-hidden` (el nombre ya está escrito).
Ilustración SVG local con `alt` descriptivo.

**`PrivacyPage` (`/privacidad`)** — Política de privacidad (qué datos, para qué, base legal,
derechos, seguridad). Es obligatoria porque la app pide datos personales. Un `<article>` con
`<h2>` por tema.

**`NotFoundPage` (`*`)** — 404 con dos salidas: "Ir al inicio" y "Ver beats".

### 7.2 Parte B (Mateo): Beats (el "producto")

**`BeatsPage` (`/beats`)** — listado + publicar.
- Filtros por **género, BPM y productor**. Clave para defender: **no guardamos una lista
  filtrada aparte**. En cada render calculamos `visibleBeats = beats.filter(...)` a partir del
  estado. Así un beat recién publicado aparece solo, sin sincronizar dos listas.
- Opciones de productor: `[...new Set(beats.map(b => b.ownerName))].sort()` (sin repetidos).
- `setFilter(key)` devuelve una función: es un "generador de setters" para no escribir uno
  por campo.
- El contador de resultados tiene `aria-live="polite"`: el lector de pantalla anuncia el
  nuevo total al filtrar.
- El `<form>` de filtros hace `preventDefault` en submit (Enter no recarga).
- Lista vacía → `EmptyState` "No hay beats con esos filtros" + botón "Limpiar".
- Si venís de borrar un beat, muestra "El beat … se borró" (leído de `useLocation().state`).

**`PublishBeatForm`** — 3 casos según quién mira: sin sesión → "Entrá"; LABEL → "Solo
artistas pueden publicar"; ARTIST → el formulario. Valida título, URL https, BPM 40-220 y
tonalidad. Agrega el beat **al principio** de la lista.

**`BeatDetailPage` (`/beats/:id`)** — detalle.
- `useParams()` da el id **como texto**: por eso `Number(id)`.
- Si no existe → "Beat no encontrado" (no rompe).
- `isOwner = user?.name === beat.ownerName` decide si se muestra "Gestionar mi beat".
- El audio es un link externo (SoundCloud) con `target="_blank" rel="noopener noreferrer"`
  (seguridad: la pestaña nueva no puede tocar la nuestra).
- Arma la página con 3 bloques: `CommentsSection`, `ManageBeat` (solo dueño),
  `ToplinesSection`.

**`ManageBeat`** — **modificación y borrado** (lo que pide la consigna).
- Editar: el formulario arranca con los datos actuales; guarda con
  `setBeats(current => current.map(b => b.id === beat.id ? { ...b, ...changes } : b))`.
- Borrar: `ConfirmDelete` → borra el beat **y también sus comentarios y toplines** (para no
  dejar datos huérfanos, igual que haría la base con las FK) y navega a `/beats` mandando el
  título en el `state`.
- Los ids llevan prefijo `edit-` para no chocar con otros formularios de la misma página.

**`CommentsSection`** — lista de comentarios (`<time dateTime>` para la fecha) + formulario
(máx. 500 caracteres). Sin sesión: "Entrá para dejar un comentario".

**`ToplinesSection`** — toplines del beat. Dueño: Aceptar/Rechazar los pendientes. Autor del
topline: Borrar. Otro ARTIST: subir uno nuevo (queda PENDING). Después de aceptar, rechazar o
borrar aparece un `Alert` de confirmación ("Aceptaste el topline de Kira Beats."), guardado en
un `useState` llamado `message`.

### 7.3 Parte C (Paolo): Colaboraciones, Desafíos, Ranking

**`CollaborationsPage` (`/colaboraciones`)** — primero resuelve el acceso (sin sesión → "Iniciá
sesión"; LABEL → "solo para artistas"), después calcula:
- `received`: toplines que llegaron a **mis** beats.
- `myToplines`: los que subí **yo**.
- `beatsById = Object.fromEntries(...)`: mapa id → beat para nombrar cada fila sin recorrer la
  lista cada vez.

**`ProposalsSection`** — propuestas **agrupadas por beat** (el dueño piensa "qué me llegó para
este beat"). Filtro por estado. Acciones según el estado:
Pendiente → Aceptar / Rechazar / Quitar; Aceptada → Borrar topline (con confirmación);
Rechazada → Quitar. "Quitar" no pregunta (no destruye nada tuyo); "Borrar" sí pregunta (deshace
una colaboración aceptada).

**`MyToplinesSection`** — mis toplines: editar el link **en la misma fila** (un solo campo, no
amerita otra pantalla) o borrar.

**El mensaje de confirmación (ambas secciones):** un `useState` llamado `message`. Cada acción
(aceptar, rechazar, quitar, guardar, borrar) hace `setMessage('…')`, y arriba de la lista se
dibuja `{message && <Alert kind="success">{message}</Alert>}` (renderizado condicional). El
`Alert` tiene `role="status"`, así que el lector de pantalla lo lee solo: quien usa teclado se
entera de qué pasó aunque el botón que tocó haya desaparecido.

**`ChallengesPage` (`/desafios`)**:
- `pickFeatured`: el destacado es el que está **en votación**; si no hay, el abierto que
  cierra antes.
- `daysUntil`: días que faltan; compara a mediodía para que el huso horario no corra un día.
- Lista de desafíos con `StatusBadge`, entregas del destacado y por cada una: "Votar", "Ya
  votaste", "Es tu entrega" o "Entrá para votar".
- `canVote`: sesión + no votó + no es propia. El comentario aclara: el backend lo vuelve a
  controlar; acá es para no ofrecer un botón que después rebota.
- `votesSent` se usa como **`key` del Alert**: así, si votás dos veces seguidas, el aviso se
  vuelve a montar y el lector de pantalla lo anuncia de nuevo.
- `JuryBand` + `Podium` del desafío cerrado.

**`VoteForm`** — select de entrega + **10 radios dentro de un `<fieldset>` con `<legend>`**
(se recorre con flechas, un solo Tab, el lector dice "Puntaje de 1 a 10, 8"). El aspecto de
botón es solo CSS. Comentario opcional (máx. 500). El mensaje de éxito vive en la página, no
en el form, porque al votar la última entrega el form desaparece.

**`Podium`** — `<ol>` en orden 1, 2, 3 (es una lista ordenada de verdad). El orden visual
2-1-3 (ganador al medio) se hace con la propiedad CSS `order` de flexbox: **no toca el HTML**.

**`RankingPage` (`/ranking`)** — una **`<table>` de verdad**: `<caption>` (oculto),
`<th scope="col">` en el encabezado y `<th scope="row">` en el puesto. Tu fila se marca con
color **y** con el texto "(vos)" (el color no se lee en voz alta). En celular la tabla scrollea
en horizontal dentro de su contenedor (`.table-scroll`), sin mover la página.

### 7.4 Parte D (Dani): Premium (flujo de compra), Login, Registro, Contacto

**`PremiumPage` (`/premium`)** — el **flujo de compra simulado**, 3 pasos en una sola ruta.
- `step` es estado local (1, 2, 3). Sin sesión → `<Navigate to="/login" />`.
- `HEADERS[step]` cambia el título y los números según el paso.
- `Stepper`: `<ol>` con `aria-current="step"` en el paso actual (el lector de pantalla
  anuncia en qué paso estás).
- Cada paso es un componente distinto y se elige con renderizado condicional:
  `{step === 1 && <PlanStep … />}`.

**`PlanStep`** — compara Free vs Premium desde `plans` (precio y límites en un solo lugar).
Muestra "Tu plan actual" y "Usaste 12 de 50 producciones".

**`PaymentStep`** — datos de tarjeta.
- Formatea mientras escribís: `formatCardNumber` (espacios cada 4) y `formatExpiry` (pone la
  `/`). Lo hace `setField(key, format)`, que recibe una función de formato opcional.
- Valida: nombre, número (16 dígitos + **Luhn**), vencimiento (formato y no vencida), CVV (3-4),
  y privacidad aceptada. Mensajes distintos para "vacío" y "mal escrito".
- Alert de error general "Revisá los campos marcados" + error por campo.
- **Datos sensibles:** la tarjeta vive **solo** en el estado de este componente. Nunca va a
  storage ni a consola. Al pasar al paso 3 el componente se desmonta y React la descarta. Hacia
  afuera solo sale `•••• 1111`. CVV con `type="password"`. `autoComplete="cc-*"` para que el
  navegador ayude.
- Resumen del pago en un `<aside>`.

**`DoneStep`** — confirmación: "Tu plan ahora es PREMIUM", tarjeta enmascarada, y dos salidas.

**`LoginPage`** — valida email y contraseña; si no coinciden: Alert de error, **borra la
contraseña** y le da foco para reintentar sin mouse. Si ya hay sesión → redirige al inicio.

**`RegisterPage`** — nombre, email, contraseña (8+), **tipo de cuenta con radios reales**
(tarjetas visuales, radios ocultos con `visually-hidden`, siguen andando con teclado), ciudad
opcional, privacidad. Al terminar inicia sesión. Email y contraseña se validan pero **no se
guardan**.

**`ContactPage`** — nombre, email, motivo (select), mensaje (10+), privacidad. Al enviar, el
form se reemplaza por el `Alert` de éxito (que el lector de pantalla lee solo) y un botón
"Enviar otro".

---

## 8. La rúbrica criterio por criterio, con dónde está la evidencia

Autoevaluación honesta. "Mostrar" = qué enseñarle al profe si pregunta.

| # | Criterio | Pts | Evidencia (mostrar) | Riesgo |
|---|---|---|---|---|
| 1 | Negocio | 15 | 13 rutas que cubren el negocio completo: beats, toplines, colaboración, desafíos con jurado 30/30/40, ranking, Premium. La UI cambia según rol (visitante / ARTIST / LABEL) | Bajo |
| 2 | Estructura | 10 | Rutas en `App.jsx`, 2 layouts, menú igual en todas, footer, 404, `pages/` + `components/layout` + `components/ui` | Bajo |
| 3 | Semántica y accesibilidad | 15 | 1 `<h1>` por página, `header/nav/main/footer/section/article/aside`, `<table>` con `th scope`, `<dl>`, `<ol>`, `fieldset/legend`, labels en todo, `alt`, skip link, `:focus-visible`, `aria-current`, `aria-expanded`, `aria-invalid`, `role="alert"`/`"status"`, foco al primer error, contraste anotado en `tokens.css` (`--primary-dark` 5,4:1), `prefers-reduced-motion` | Bajo. Falta correr Lighthouse para tener un número |
| 4 | Diseño visual | 15 | Tokens en `tokens.css` (colores, fuentes, espacios en un solo lugar), mismo `PageHeader` en todas las páginas, mismos botones/badges | Medio: es subjetivo |
| 5 | Adaptabilidad | 10 | Media queries en 640/700/860/900 px, `clamp()` en los títulos, menú hamburguesa, tabla con scroll propio. Probado a 375/768/1280 sin desborde (04/11) | Bajo |
| 6 | Interacción | 15 | Filtros, menú, publicar/editar/borrar, aceptar/rechazar, votar, pasos de Premium, login/logout; todo con feedback (Alert, badge, contador) | Medio: ver sección 10, botón "Ver entregas" |
| 7 | Formularios | 10 | 9 formularios con `FormField`, validación propia (`noValidate` + `validation.js`), error por campo + foco al primero, "Enviando…" + botón deshabilitado, Alert de éxito | Bajo |
| 8 | Calidad técnica | 10 | Componentes reusables, validaciones puras en `validation.js`, un solo Context, solo lo visto en clase, lint limpio, build OK, comentarios que explican el porqué | Bajo |

**Estimación realista: 85-95 / 100.** Mínimo para aprobar: 60.

---

## 9. Preguntas probables del profesor (y cómo responder)

**¿Por qué React y no HTML/JS plano?**
No da puntos extra (la rúbrica lo dice) y no lo elegimos por eso. Lo elegimos porque con 13
pantallas y 4 personas necesitábamos **componentes reusables** (un solo `FormField`, un solo
`Button`) y **estado compartido** (aceptar un topline en un lado y verlo en otro). Con JS plano
hubiéramos repetido el mismo HTML en cada página. Es la misma estructura del repo de clase
(TodoList).

**¿Qué diferencia hay entre props y estado?**
Props vienen del padre y son de solo lectura. Estado es propio del componente y se cambia con
su setter; cuando cambia, React vuelve a dibujar.

**¿Qué es el Virtual DOM?**
Una copia en memoria de la pantalla. Cuando cambia el estado, React arma la nueva versión, la
compara con la anterior y aplica en el DOM real solo las diferencias.

**¿Qué hace `useEffect`? ¿Y el array del final?**
Corre código después de dibujar, para cosas de afuera de React. Lo usamos solo dos veces:
cambiar el título de la pestaña (`PageHeader`) y hacer scroll arriba al cambiar de página
(`Layout`). El array dice cuándo repetirlo: `[title]` = solo si cambió el título; `[pathname]`
= solo si cambió la URL.

**¿Por qué Context y no pasar props?**
Porque la sesión y los datos los usan casi todas las páginas. Pasarlos prop por prop por 3 o 4
niveles es "prop drilling". Tenemos **un solo** Context (`AppContext`) para que haya un único
lugar donde mirar: cualquier componente hace `const { user, beats } = useApp()`.

**¿Por qué no usaron Redux?**
Para este tamaño alcanza con Context + `useState`, que es lo que trae React. Agregar una
librería más no suma.

**¿Para qué sirve la `key` en las listas?**
Para que React sepa qué elemento es cuál entre renders. Usamos el `id`, no el índice, porque
si borrás uno el índice de los demás cambia.

**¿Por qué `current => ...` en los setters?**
Porque el valor nuevo depende del anterior. Así React nos da el último valor real, aunque haya
dos cambios seguidos.

**¿Por qué no hacen `beats.push()`?**
Porque React compara referencias: si modificás la misma lista, no se entera de que cambió.
Siempre creamos una nueva con spread, `map` o `filter`.

**¿Dónde se guardan los datos? ¿Qué pasa si recargo?**
En memoria (estado de React en `AppContext`). Al recargar vuelven los datos de ejemplo y se
cierra la sesión. La rúbrica no exige persistencia ("no se exige guardar datos de forma
permanente"). En la demo: **no apretar F5** después de loguearse.

**¿Cómo validan los formularios?**
Del lado del cliente con funciones puras en `validation.js`. Ponemos `noValidate`
en el form para usar nuestros mensajes en castellano en vez de los globitos del navegador. Si
hay error: mensaje debajo del campo, `aria-invalid`, y foco al primer campo con error.

**¿Qué es el algoritmo de Luhn?**
El dígito verificador de las tarjetas: se duplica uno de cada dos dígitos desde la derecha (si
da más de 9 se le resta 9), se suma todo y tiene que dar múltiplo de 10. Detecta números mal
tipeados sin servidor. Tarjeta de prueba: `4111 1111 1111 1111`.

**¿Cómo simulan la espera del servidor?**
`fakeRequest(600)`: una promesa que se resuelve a los 600 ms. Con `await` el botón muestra
"Enviando…" y está deshabilitado (y `if (sending) return` evita doble envío).

**¿Qué hicieron de accesibilidad?**
HTML semántico, un `h1` por página, labels en todos los campos, "Saltar al contenido", foco
visible, menú con `aria-expanded`, mensajes con `role="alert"`/`"status"`, estados escritos (no
solo color), radios reales con `fieldset`, tabla real con `th scope`, textos ocultos para links
repetidos, foco al primer campo con error, y "Cancelar" con `autoFocus` al confirmar un borrado.

**¿Cómo lo hicieron responsive?**
CSS propio con media queries (640, 700, 860, 900 px), `clamp()` para el tamaño de los títulos,
menú que se colapsa en celular, y la tabla de ranking scrollea sola. Lo probamos a 375, 768 y
1280 px.

**¿Por qué el producto es un beat y la compra es Premium? La consigna dice e-commerce.**
Porque usted autorizó el pivot: no somos un e-commerce. El beat es el producto (listado,
detalle, edición, borrado) y la suscripción es lo que se compra (flujo simulado de 3 pasos).

**¿Por qué no se conecta al backend?**
La rúbrica dice que en esta instancia no es obligatorio y no se penaliza. El backend existe
(Etapa 1) y la integración se evalúa después.

**¿Cómo funciona el login si no hay backend?**
`login` (en `AppContext`) compara con 3 usuarios de prueba en `mockData`. Si coincide, guarda
`{ name, role, plan }`. Es simulado a propósito.

**¿Qué pasa si escribo `/beats/999` o una URL inventada?**
`/beats/999` muestra "Beat no encontrado" con un botón para volver. Una URL inventada cae en la
ruta `*` y muestra la 404.

**¿Cómo lo probaron?**
Lint con oxlint (incluye reglas de accesibilidad), build sin errores, y prueba manual de las 13
rutas en 3 tamaños de pantalla.

---

## 10. Puntos flojos conocidos (decirlos antes de que los encuentre él)

1. **Botón "Ver entregas" en Desafíos** (`ChallengesPage.jsx`, `<Button to="#entregas">`):
   es un `Link` de react-router a un ancla. El router cambia la URL pero **no hace scroll** a
   `#entregas` (BrowserRouter no maneja anclas). Probalo: si no baja, es el único control que
   "declara algo y no responde" (criterio 6). Arreglo de 1 línea: usar un `<a href="#entregas">`
   común con la clase del botón, como ya hace el link "Votar" de la misma página.
2. **Lighthouse sin correr.** No tenemos el número de accesibilidad/rendimiento. Correrlo en
   Chrome (DevTools → Lighthouse) sobre `npm run build && npm run preview` y anotar el puntaje.
3. **Los datos y la sesión no persisten** al recargar (en memoria). Es aceptado por la
   rúbrica; decirlo como decisión, no como olvido. En la demo, no apretar F5.
4. **El rol LABEL (discográfica) no tiene pantalla propia de "crear desafío"**. En Registro se
   dice que "crea desafíos", pero en el front solo puede explorar, comentar y votar. Si
   pregunta: está en el backend (Etapa 1); en el front quedó fuera de alcance.
5. **Lo más difícil de explicar** (prepararlo bien): la `key` usada para reiniciar `VoteForm`
   y el `Alert`, y `setField(key)` que devuelve una función.

---

## 11. Guion de demo (10 minutos)

1. **Landing** (1 min): números, "Qué podés hacer", jurado 30/30/40. Achicar la ventana →
   menú hamburguesa.
2. **Beats sin sesión** (1 min): filtrar por Trap → contador cambia; filtro sin resultados →
   EmptyState → Limpiar. Publicar pide "Entrar".
3. **Login con error** (30 s): contraseña mala → Alert rojo, contraseña borrada, foco ahí.
   Después `artista@mgw.com / mgw12345`.
4. **Publicar beat** (1 min): enviar vacío → errores + foco al primero; BPM 300 → mensaje;
   completarlo → "Enviando…" → aparece primero.
5. **Detalle de "Noche en Palermo"** (2 min): comentar, editar el BPM, aceptar el topline de
   Kira (aparece "Aceptaste el topline de Kira Beats."), borrar con confirmación (mostrar que
   el foco queda en "Cancelar").
6. **Colaboraciones** (1 min): el topline aceptado aparece como "Aceptada"; filtro por estado.
7. **Desafíos** (1 min): votar sin puntaje → error; votar con 8 → "Tu voto se registró";
   podio.
8. **Premium** (1,5 min): plan → pago con tarjeta mala → error de Luhn; vencimiento pasado →
   error; `4111 1111 1111 1111`, `12/30`, `123`, aceptar privacidad → "Listo".
9. **Nosotros** (30 s): los 4 integrantes. **Teclado** (30 s): Tab desde arriba → aparece
   "Saltar al contenido".

---

## 12. Glosario

- **SPA:** aplicación de una sola página; el router cambia el contenido sin recargar.
- **Vite:** la herramienta que levanta el servidor de desarrollo y arma la versión final.
- **JSX:** sintaxis tipo HTML adentro de JavaScript.
- **Hook:** función de React que empieza con `use`. Usamos `useState`, `useEffect`,
  `useContext` (a través de nuestro `useApp`) y los de react-router (`useParams`,
  `useNavigate`, `useLocation`).
- **Provider:** componente que "reparte" un Context a todo lo que tiene adentro.
- **Prop drilling:** pasar una prop por muchos niveles solo para que llegue abajo.
- **Controlado (input):** su valor sale del estado de React.
- **Mock / datos simulados:** datos de ejemplo en un archivo, en vez de venir de un servidor.
- **ARIA:** atributos (`aria-*`, `role`) que le dan información extra al lector de pantalla.
- **Lector de pantalla:** programa que lee la página en voz alta (lo usan personas ciegas).
- **`visually-hidden`:** clase CSS que oculta algo a la vista pero lo deja para el lector.
- **Skip link:** "Saltar al contenido", para no recorrer todo el menú con Tab.
- **Foco:** qué elemento recibe el teclado en este momento.
- **Media query:** regla CSS que se aplica solo en ciertos tamaños de pantalla.
- **Token de diseño:** variable CSS con un color/medida que se usa en todo el sitio.
- **Luhn:** algoritmo del dígito verificador de las tarjetas.
- **Lint:** revisión automática del código en busca de errores y malas prácticas.
