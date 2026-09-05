# mgw-prod — Etapa 2: frontend del módulo `collab`

**Fecha:** 2026-09-03 (revisado 2026-09-05)
**Autor:** Dani Gariboldi
**Estado:** propuesto
**Depende de:** `docs/superpowers/plans/2026-09-01-mgw-prod-collab-module.md` (backend `collab`,
mergeado — [wein11/MGW-prod#4](https://github.com/wein11/MGW-prod/pull/4))

**Revisión 2026-09-05:** desde que se escribió la v1 de este spec, `catalog`/`challenges` se
mergearon a `main`, y el profesor pidió 3 correcciones (roles, CRUD completo, billing —
`docs/superpowers/specs/2026-09-04-mgw-prod-professor-corrections-design.md`). La más relevante
acá: `Role` pasó de `{PRODUCER, ARTIST}` a `{ARTIST, DISCOGRAFICA, ADMIN}` — ya no existe un rol
"productor" separado, cualquier `ARTIST` publica beats y sube toplines. Esta revisión actualiza
el spec para reflejar eso y aprovecha que `GET /api/beats` ya es real (ya no hace falta el input
manual de `beatId` de la v1). El resto del diseño (alcance, `api.js`, estructura de archivos) no
cambió.

## Por qué este spec

Es el primer código de frontend de todo el proyecto — ningún otro módulo (`users`, `catalog`,
`challenges`, `billing`) tiene todavía ninguna página de Etapa 2 (confirmado de nuevo en esta
revisión: `src/main/resources/` solo tiene `application.properties`). El spec del pivot
(`docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md`) asigna a Dani dos páginas
concretas: `topline-upload.html` y `collaborations.html`. Como este trabajo va a sentar el
patrón que el resto del equipo copie cuando llegue a su propia Etapa 2, el alcance de este spec
es deliberadamente chico: las 2 páginas de `collab` más el mínimo compartido indispensable para
que sean navegables (un `api.js` y una `login.html`) — no un frontend base para todo el
proyecto, y no las páginas de otros módulos.

## Alcance

**Incluye:**
- `login.html` — login mínimo, necesario para que las páginas de `collab` sean usables sin
  depender de que otro módulo arme su propio login primero.
- `topline-upload.html` — el artista sube un topline sobre un beat.
- `collaborations.html` — bandeja del productor: colaboraciones `PENDING` propias, aceptar/rechazar.
- `js/api.js` — wrapper de `fetch` compartido (token, parseo de errores).
- `css/style.css` — estilos mínimos compartidos.
- Backend: agregar filtros `producerId`/`artistId` a `GET /api/collaborations` (gap entre el
  spec del pivot, que ya los promete, y `docs/superpowers/plans/2026-09-01-mgw-prod-collab-module.md`
  Task 4, que solo implementó `status`). Sin este filtro, `collaborations.html` no tiene forma
  de mostrar "solo mis colaboraciones" sin depender de `catalog`.

**Fuera de alcance (a propósito):**
- Registro de usuario, perfil/portfolio, catálogo de beats (página propia), challenges, billing
  — páginas de otros módulos/dueños.
- Historial de colaboraciones aceptadas/rechazadas en `collaborations.html` — solo `PENDING`
  por ahora (es el caso de uso real: decidir sobre lo pendiente).
- Cualquier framework de testing de frontend — no está en el stack de la cátedra.
- Vistas para `DISCOGRAFICA`/`ADMIN` — ninguno de los dos roles interactúa con toplines o
  colaboraciones; sus páginas (crear challenges, verificar artistas) son de otros módulos.

## Arquitectura y estructura de archivos

Servido como estático por el mismo Spring Boot (`src/main/resources/static/`, sin CORS, un
solo puerto — como ya establece `CLAUDE.md`). Sin build step ni bundler: HTML5 + CSS3 + JS
plano, cada página standalone.

```
src/main/resources/static/
├── css/style.css
├── js/api.js
├── login.html
├── topline-upload.html
└── collaborations.html
```

## `api.js` — auth y manejo de errores

Un único wrapper de `fetch`, sin dividir en módulos adicionales (YAGNI para esta primera
versión — se puede partir en `auth.js`/`api.js` más adelante si el equipo lo necesita):

```js
const TOKEN_KEY = "mgw_token";

async function apiFetch(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  const token = localStorage.getItem(TOKEN_KEY);
  if (auth && token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;

  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.message || `Error ${res.status}`);
  return data;
}
```

- `login.html` llama `apiFetch("/api/auth/login", { method: "POST", body, auth: false })`.
  Éxito: guarda `token`, `userId`, `role` (del `LoginResponse`, ver `docs/api/users-API.md`) en
  `localStorage`. Como ya no hay rol "productor" separado (cualquier `ARTIST` publica beats y
  sube toplines — ver la revisión al principio de este doc), no hay redirect distinto por rol:
  si `role === "ARTIST"` redirige a `topline-upload.html` (con un link a `collaborations.html`
  en la misma página — ambas son del mismo usuario). Si `role` es `DISCOGRAFICA`/`ADMIN`, muestra
  un mensaje ("estas páginas son para artistas") en vez de redirigir — esos roles no tienen nada
  que hacer en `collab`.
- `topline-upload.html`/`collaborations.html` chequean al cargar si hay `token` en
  `localStorage`; si no, redirigen a `login.html` (chequeo client-side; el 401 real lo sigue
  decidiendo el backend).
- Errores de `apiFetch` (red, 400/401/403/404/500) se muestran en un `<div class="error">` por
  página — nunca `alert()`.
- Loading state: deshabilitar el botón de submit + texto "Enviando..." mientras la promesa
  está pendiente.
- Validación en dos capas: HTML5 nativo (`required`, `type="url"`, `type="number"`) más lo que
  devuelva el backend si algo pasa igual — el mismo patrón de "defensa en profundidad" que ya
  usa el backend con `@Valid`.

## Páginas

### `login.html`
Form `email` + `password`. Éxito → guarda sesión, redirige según `role`. Error → mensaje del
backend (401 credenciales inválidas, 400 validación).

### `topline-upload.html`
Al cargar, `GET /api/beats` puebla un `<select>` con los beats existentes (label:
`"{title} — {genre}, {bpm} BPM"`, value: `id`) — reemplaza el input manual de `beatId` de la v1
de este spec, ahora que `catalog` es real. Form: ese `<select>` + `audioUrl`. Submit →
`POST /api/toplines`. Éxito → confirmación + link "ver mis toplines"
(`GET /api/toplines?artistId={userId}`, usando el `userId` guardado en el login). Redirige a
`login.html` si no hay token. Si `GET /api/beats` devuelve vacío, mostrar "todavía no hay beats
publicados" en vez de un `<select>` vacío.

### `collaborations.html`
Al cargar: `GET /api/collaborations?producerId={userId}&status=PENDING` (endpoint nuevo, ver
abajo — `producerId` es el nombre del query param, no implica un rol "productor": es el
`Beat.producerId` del beat detrás de cada colaboración, y el `{userId}` que se pasa es el del
`ARTIST` logueado). Por cada colaboración, resuelve `GET /api/toplines/{toplineId}` para mostrar
el `audioUrl` (poder "escucharla" antes de decidir). Botones Aceptar/Rechazar →
`PUT /api/collaborations/{id}?status=ACCEPTED|REJECTED`; éxito saca la fila de la lista.
Redirige a `login.html` si no hay token.

## Backend: filtros `producerId`/`artistId` en `GET /api/collaborations`

Extensión del módulo `collab` ya existente (mismo dueño, mismo patrón TDD que
`docs/superpowers/plans/2026-09-01-mgw-prod-collab-module.md`):

- `CollaborationService.listByProducer(Long producerId, CollaborationStatus status)` — recorre
  las colaboraciones (filtradas por `status` si viene), resuelve
  `collaboration.toplineId → ToplineService.getById → topline.beatId → BeatRepository.findById → beat.producerId`
  y se queda con las que matchean. Usa el `BeatRepository` ya inyectado en `CollaborationService`
  (ya es el `catalog` real, mergeado desde el
  [wein11/MGW-prod#6](https://github.com/wein11/MGW-prod/pull/6) — no hace falta ningún ajuste
  para esto). El nombre del método/parámetro (`producerId`) refleja el nombre del campo
  `Beat.producerId`, no un rol — con el modelo de roles actual cualquier `ARTIST` puede ser
  "el productor" de un beat.
- `CollaborationService.listByArtist(Long artistId, CollaborationStatus status)` — más directo:
  filtra por `Topline.artistId` sin pasar por `catalog` en absoluto.
- `CollaborationController`: `GET /api/collaborations` suma `producerId`/`artistId` como query
  params opcionales, junto al `status` que ya existía. Precedencia si se combinan: `producerId`
  o `artistId` (mutuamente excluyentes en la práctica, ya que cada colaboración solo importa a
  un productor y a un artista) tiene prioridad sobre listar todo; `status` siempre filtra
  encima.
- `docs/api/collab-API.md` se actualiza con los query params nuevos.

## Testing

- **Backend:** TDD igual que el resto del módulo — test de servicio para `listByProducer`/
  `listByArtist` (mocks de `BeatRepository`/`ToplineService`/`CollaborationRepository`), test de
  controller para los query params nuevos. Corre contra la MySQL local ya configurada
  (`replace = Replace.NONE`, ver nota en el commit de Task 1 del plan de backend).
- **Frontend:** sin test automatizado (no hay framework de testing JS en el stack de la
  cátedra). Verificación manual end-to-end en browser: login → subir topline → ver en la
  bandeja del productor → aceptar/rechazar, antes de dar el trabajo por terminado.

## Riesgos / seguimiento

- Los dos riesgos de la v1 de este spec (forma final de `Beat`/`BeatRepository`, y reemplazar el
  input manual de `beatId` por un `<select>` real) ya se resolvieron: `catalog` está mergeado y
  esta revisión ya diseña `topline-upload.html` contra `GET /api/beats` real.
- `feature/billing-module` ([wein11/MGW-prod#10](https://github.com/wein11/MGW-prod/pull/10),
  todavía sin mergear al escribir esto) agrega un límite de 50 producciones (beats+toplines) al
  plan `FREE` — `POST /api/toplines` puede empezar a devolver 403
  (`SubscriptionLimitExceededException`) para un artista que llegó al límite. El manejo de
  errores de `api.js` ya cubre cualquier 403 genérico mostrando `data.message`, así que no hace
  falta un caso especial en `topline-upload.html` — el mensaje del backend ya explica el límite.
