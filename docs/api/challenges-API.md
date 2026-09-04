# Challenges module API

Base path: `/api`. All request/response bodies are JSON.

Auth: same session-token scheme as the users module. Send `Authorization: Bearer <token>`
on protected endpoints. `SessionAuthInterceptor` reads the header on every request; if it's
absent the request proceeds unauthenticated (no `userId` attribute) rather than being
rejected at the interceptor level — each endpoint decides for itself whether auth is required.

This module owns desafíos con jurado ponderado, submissions, votos, resultados y ranking, más
la extensión de `users` que marca a un productor como **verificado** (mayor peso de voto).

Regla de dependencia: `challenges` referencia a `users` (por FK plana `guestArtistId`,
`producerId`, `voterId`), nunca al revés.

---

## PUT /api/producers/{id}/verify

Marca `ProducerProfile.verified = true`. Vive en el módulo `users` pero es parte del pivot de
challenges (el peso de voto de productores verificados).

- **Auth:** required. Solo un **admin** (`User.isAdmin = true`) puede verificar.
- **Path params:** `id` — user id del productor a verificar.
- **Request body:** ninguno.
- **Response body:** `ProducerProfile` (`{ id, genres, bpmMin, bpmMax, experienceLevel, verified }`).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Productor verificado |
  | 401 | No autenticado (sin token válido) |
  | 403 | Autenticado pero no admin, o el `id` no corresponde a un usuario con rol PRODUCER |
  | 404 | No existe usuario con ese `id` |

---

## GET /api/challenges

Lista todos los challenges (activos y pasados).

- **Auth:** not required.
- **Response body:** array de `Challenge`.
- **Status codes:** 200.

## POST /api/challenges

Crea un challenge. El `guestArtistId` debe ser un usuario con rol ARTIST.

- **Auth:** required. Solo un **admin** puede crear challenges.
- **Request body** (`Challenge`):

  | Field | Type | Notes |
  |---|---|---|
  | `title` | string | required, no vacío |
  | `genre` | string | required, no vacío |
  | `bpm` | number | required, >= 1 |
  | `key` | string | opcional (tonalidad) |
  | `theme` | string | opcional |
  | `deadline` | ISO-8601 timestamp | required |
  | `guestArtistId` | number | required, user id con rol ARTIST |
  | `prizeFirst` / `prizeSecond` / `prizeThird` | string | opcionales (texto libre) |

- **Response body:** `Challenge` creado (con `id`, `createdAt`).
- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Challenge creado |
  | 400 | Validación (title/genre vacío, bpm < 1, deadline/guestArtistId nulos) o JSON malformado |
  | 401 | No autenticado |
  | 403 | Autenticado pero no admin, o `guestArtistId` no tiene rol ARTIST |
  | 404 | `guestArtistId` no existe |

## GET /api/challenges/{id}

Detalle de un challenge. (Las submissions se piden aparte, ver abajo — cada endpoint devuelve
una sola entidad o lista, sin objetos combinados ad hoc, por la regla "sin DTOs".)

- **Auth:** not required.
- **Response body:** `Challenge`.
- **Status codes:** 200; 404 si no existe.

---

## GET /api/challenges/{challengeId}/submissions

Lista las submissions de un challenge.

- **Auth:** not required.
- **Response body:** array de `Submission`.
- **Status codes:** 200.

## POST /api/challenges/{challengeId}/submissions

Envía una producción a un challenge. `challengeId` (path) y `producerId` (del token) los
completa el servidor; el cliente solo manda `audioUrl`.

- **Auth:** required. Solo rol **PRODUCER**. El deadline no debe haber pasado.
- **Request body:** `{ "audioUrl": string }` (required, no vacío).
- **Response body:** `Submission` (`{ id, challengeId, producerId, audioUrl, submittedAt }`).
- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Submission creada |
  | 400 | `audioUrl` vacío o JSON malformado |
  | 401 | No autenticado |
  | 403 | No es PRODUCER, o el deadline del challenge ya pasó |
  | 404 | El challenge no existe |

---

## POST /api/submissions/{submissionId}/votes

Vota una submission (1–10) con comentario opcional. `submissionId` (path) y `voterId` (del
token) los completa el servidor. La categoría del jurado **no** se persiste: se resuelve al
cerrar el challenge comparando `voterId` contra `guestArtistId` y contra los verificados.

- **Auth:** required. Un usuario no puede votar dos veces la misma submission.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `score` | number | required, 1–10 |
  | `comment` | string | opcional |

- **Response body:** `Vote` (`{ id, submissionId, voterId, score, comment }`).
- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Voto registrado |
  | 400 | `score` fuera de 1–10 o nulo, o JSON malformado |
  | 401 | No autenticado |
  | 403 | El votante ya votó esa submission |
  | 404 | La submission no existe |

---

## PUT /api/challenges/{id}/close

Cierra el challenge: calcula el puntaje ponderado de cada submission, arma el top 3 y persiste
un `ChallengeResult` por puesto (500/300/150 puntos). El ganador (#1) queda verificado
automáticamente. Se dispara a mano (sin batch/async, requisito de Etapa 1).

**Cálculo del puntaje** (por submission):
```
score = 0.30 * avg(votos de comunidad)
      + 0.30 * avg(votos de productores verificados)
      + 0.40 * (voto del guestArtist, o 0 si no votó)
```
"Comunidad" = todo votante que no es el `guestArtist` ni un productor verificado. Si el
invitado no votó una submission, esa porción vale 0 (sin renormalización).

- **Auth:** required. Solo un **admin**.
- **Request body:** ninguno.
- **Response body:** array de `ChallengeResult`
  (`{ id, challengeId, submissionId, rank, pointsAwarded, badge, prizeText }`), top 3 ordenado.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Challenge cerrado, resultados creados |
  | 401 | No autenticado |
  | 403 | Autenticado pero no admin |
  | 404 | El challenge no existe |

## PUT /api/challenges/{id}/opportunity-pick

Fija el `opportunityPickSubmissionId` del challenge — el pick del artista invitado, independiente
del ranking ("quiero trabajar con el #7 aunque no haya ganado").

- **Auth:** required. Solo el **`guestArtistId`** de ese challenge.
- **Query params:** `submissionId` — la submission elegida (debe pertenecer al challenge).
- **Response body:** `Challenge` actualizado.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Pick fijado |
  | 401 | No autenticado |
  | 403 | No es el artista invitado, o la submission no pertenece a este challenge |
  | 404 | El challenge o la submission no existen |

---

## GET /api/challenges/results

Histórico de resultados. Usado por el portfolio y para reconstruir el Music Score.

- **Auth:** not required.
- **Query params:** `producerId` — opcional; filtra por productor.
- **Response body:** array de `ChallengeResult`.
- **Status codes:** 200.

## GET /api/ranking

Ranking global: suma de `pointsAwarded` por productor, descendente.

- **Auth:** not required.
- **Response body:** array de `RankingEntry` (`{ producerId, totalPoints }`), ordenado desc.
- **Status codes:** 200.

---

## Nota sobre `@Valid` y campos seteados por el servidor

`Submission` (`challengeId`/`producerId`) y `Vote` (`submissionId`/`voterId`) llevan esos campos
con `@Column(nullable = false)` pero **sin** `@NotNull`: los completa el service desde el path y
el token, no vienen en el body. Ponerles `@NotNull` rompería el `@Valid @RequestBody` del
controller, que corre antes de que el service los asigne. La integridad la garantizan la columna
NOT NULL y el service, que siempre los setea.
