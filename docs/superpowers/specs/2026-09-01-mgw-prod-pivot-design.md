# mgw-prod — Pivot: de e-commerce a Music Discovery & Challenge Platform

**Fecha:** 2026-09-01
**Autores:** Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi
**Estado:** propuesto, pendiente de repartir con el resto del grupo
**Reemplaza:** `docs/superpowers/specs/2026-08-25-mgw-prod-tpo-design.md` (queda como referencia
histórica de por qué el proyecto empezó siendo e-commerce)

## Por qué este pivot

El profesor confirmó que el TPO **ya no tiene que ser una aplicación transaccional de
e-commerce** — el resto de los requisitos técnicos de la cátedra sigue igual (arquitectura por
capas, persistencia SQL, CRUD, validación de entrada, manejo de errores, dos etapas
backend/frontend, stack Java+Spring+MySQL). Esto libera al proyecto del corsé que lo obligaba a
modelar "catálogo de beats + carrito + checkout" y permite volver a la idea de producto
original: una plataforma de descubrimiento y colaboración musical para productores/artistas
emergentes — una mezcla de SoundCloud (publicar producciones) y LinkedIn (perfil como
portfolio), con desafíos semanales/mensuales como diferencial competitivo.

**Fuera de alcance para esta entrega** (parte de la visión de producto más amplia, no de este
TPO): Talent Discovery para sellos (rol `LABEL`, búsqueda paga de productores por criterios).
Es la pieza de mayor valor comercial a futuro, pero introduce un rol de usuario nuevo, un
modelo de monetización y un motor de búsqueda avanzada — no aporta nada nuevo a lo que evalúa
la cátedra (capas/CRUD/persistencia) y hubiera forzado a alguien del grupo a cargar con un
quinto módulo. Queda documentada en `docs/descripcion.md` como roadmap.

## Restricciones del TPO (vigentes al 2026-09-01)

Sin cambios respecto al spec anterior, salvo el punto de e-commerce:

- Java + Spring (framework fijado por la cátedra), SQL, comunicación HTTP, API cliente-servidor.
- Etapa 1: solo backend, demostrable con herramientas HTTP (Postman/curl), corre en localhost.
- Etapa 2: frontend en HTML5/CSS3/JavaScript plano (sin frameworks), consume la API de Etapa 1.
- Arquitectura por capas, CRUD, persistencia SQL, validación de entrada, manejo de errores,
  documentación básica de endpoints.
- ~~Aplicación web transaccional con funcionalidades de e-commerce~~ — **eliminado**. El dominio
  puede ser cualquiera, mientras cumpla los puntos anteriores.
- Grupo de 4; evaluación individual — cada integrante debe poder explicar y defender su módulo.
- Solo se usa lo visto en clase (ver `../../facultad/aplicaciones-interactivas/clases/` para el
  estado real de la cursada).

**Seguimiento continuo:** como antes, si el profesor cambia algo de la consigna hay que revisar
este documento antes de seguir implementando la parte afectada.

## Alcance del dominio (nuevo)

- **Perfil-portfolio**: cada usuario (PRODUCER o ARTIST) tiene un perfil público que funciona
  como portfolio — todo lo que publicó (beats, toplines, resultados de challenges) queda
  asociado a su perfil.
- **Publicaciones tipo red social**: productores suben beats; artistas suben "toplines"
  (su interpretación/voz sobre un beat existente); la comunidad comenta tanto beats como
  toplines.
- **Colaboración**: cuando un artista sube un topline sobre un beat, se genera automáticamente
  una propuesta de colaboración pendiente; el productor dueño del beat decide si la acepta.
- **Challenges con jueces ponderados** (el diferencial): desafíos con brief (BPM, tonalidad,
  tema, deadline), submissions de productores, votación con tres categorías de jurado con peso
  distinto (comunidad 30%, productores verificados 30%, artista invitado 40%), premios
  descriptivos por puesto, puntos acumulables (Music Score / ranking), badges, y el mecanismo
  "opportunity" (el artista invitado puede elegir trabajar con cualquiera de los participantes,
  no solo el ganador).

Fuera de alcance para este TPO (visión de producto a futuro, documentada pero no implementada):
Talent Discovery/búsqueda paga para sellos, monetización/tiers PRO, matching automático
avanzado por ubicación.

## Modelo de dominio

### Módulo `users` (sin cambios estructurales)
- `User`: id, email, passwordHash, displayName, role (`PRODUCER` | `ARTIST`), city, isAdmin
  (boolean, default false — habilita crear challenges y verificar productores), createdAt.
- `ProducerProfile` (1:1 con User si role=PRODUCER): genres, bpmRange, experienceLevel,
  **verified** (boolean, default false — nuevo; solo lo activa un admin), musicScore (derivado,
  se calcula sumando `ChallengeResult.pointsAwarded` del producer, no se persiste como fuente de
  verdad).
- `ArtistProfile` (1:1 con User si role=ARTIST): genres, bio.
- `Session`: id, userId, token, expiresAt.

### Módulo `catalog`
- `Beat`: id, producerId (FK User), title, genre, bpm, key, audioUrl (link externo tipo
  SoundCloud/Drive), createdAt. **Se elimina `price`** (ya no aplica sin e-commerce).
- `BeatComment` (nueva): id, beatId (FK Beat), authorId (FK User), text, createdAt.

### Módulo `collab` (nuevo — reemplaza a `orders`)
- `Topline`: id, artistId (FK User), beatId (FK Beat, del módulo `catalog`), audioUrl,
  createdAt — la interpretación del artista sobre el beat de un productor.
- `Comment`: id, toplineId (FK Topline), authorId (FK User), text, createdAt.
- `Collaboration`: id, toplineId (FK Topline), status (`PENDING` | `ACCEPTED` | `REJECTED`),
  decidedAt (nullable) — se crea automáticamente en `PENDING` al crearse el `Topline`; solo el
  producer dueño del `Beat` referenciado puede aceptarla o rechazarla.

### Módulo `challenges`
- `Challenge`: id, title, genre, bpm, key, theme, deadline, **guestArtistId** (FK User,
  role=ARTIST — el jurado invitado de este challenge específico), **prizeFirst**,
  **prizeSecond**, **prizeThird** (texto libre, ej. "$100 USD + sesión de estudio"),
  **opportunityPickSubmissionId** (FK Submission, nullable — el pick del artista invitado,
  independiente del ranking), createdAt.
- `Submission`: id, challengeId (FK Challenge), producerId (FK User), audioUrl, submittedAt.
- `Vote`: id, submissionId (FK Submission), voterId (FK User), score (1–10), comment (opcional).
  La categoría del jurado **no se persiste en el voto** — se resuelve en el momento del cálculo
  mirando `voterId` contra `Challenge.guestArtistId` y `ProducerProfile.verified`.
- `ChallengeResult` (nueva): id, challengeId (FK Challenge), submissionId (FK Submission), rank
  (1/2/3), pointsAwarded (500/300/150), badge (texto, nullable), prizeText — se crea una única
  vez cuando el challenge se cierra (endpoint explícito, ver Endpoints). El Music Score de un
  producer es la suma de `pointsAwarded` de todos sus `ChallengeResult`.

**Cálculo del puntaje ponderado de una submission** (al cerrar el challenge):
```
score = 0.30 * avg(votos de comunidad)
      + 0.30 * avg(votos de productores verificados)
      + 0.40 * (voto del guestArtistId, o 0 si no votó esa submission)
```
"Comunidad" = todos los votantes que no son el `guestArtistId` ni un producer con
`verified=true`. No hay renormalización si falta el voto del invitado — esa porción vale 0, lo
cual incentiva a que el artista invitado vote todas las submissions antes del cierre.

Regla de dependencia entre módulos (sin cambios): `catalog` y `challenges` solo son
referenciados por FK, nunca conocen a quien los referencia. `collab` referencia tanto `User`
como `Beat`, igual que antes lo hacía `orders` — la dirección de dependencia se mantiene.

## Portfolio (perfil tipo LinkedIn+SoundCloud)

No se agrega un endpoint de agregación en el backend — eso obligaría a que `users` dependa de
`catalog`/`collab`/`challenges`, invirtiendo la regla de dependencias unidireccional que rige
todo el proyecto. En cambio, los endpoints existentes ya soportan filtrar por usuario
(`producerId`/`artistId`), y `profile.html` (Etapa 2) arma el portfolio combinando esos fetch en
el cliente — mismo patrón de "cada módulo dueño de sus datos" que ya usa el resto del diseño.

## Endpoints

**`users`**
- `POST /api/auth/register` — crea User + profile según role.
- `POST /api/auth/login` — valida credenciales, devuelve token.
- `GET /api/users/{id}` — perfil público.
- `PUT /api/users/{id}` — editar perfil propio.
- `PUT /api/producers/{id}/verify` — marca `ProducerProfile.verified` (solo admin).

**`catalog`**
- `GET /api/beats` — catálogo (filtros: genre, bpm, producerId).
- `POST /api/beats` — publicar beat (rol PRODUCER).
- `GET /api/beats/{id}` — detalle.
- `POST /api/beats/{id}/comments` — comentar un beat.
- `GET /api/beats/{id}/comments` — listar comentarios de un beat.

**`collab`**
- `POST /api/toplines` — subir interpretación sobre un beat (rol ARTIST); crea el `Topline` y
  su `Collaboration` en `PENDING`.
- `GET /api/toplines` — listado (filtros: beatId, artistId).
- `GET /api/toplines/{id}` — detalle.
- `POST /api/toplines/{id}/comments` — comentar un topline.
- `GET /api/toplines/{id}/comments` — listar comentarios de un topline.
- `PUT /api/collaborations/{id}` — aceptar/rechazar (solo el producer dueño del beat).
- `GET /api/collaborations` — listado (filtros: producerId, artistId, status).

**`challenges`**
- `GET /api/challenges` — listado activos/pasados.
- `POST /api/challenges` — crear challenge, incluye guestArtistId y premios (flag admin).
- `GET /api/challenges/{id}` — detalle + submissions.
- `POST /api/challenges/{id}/submissions` — enviar producción (rol PRODUCER).
- `POST /api/submissions/{id}/votes` — votar + comentario opcional.
- `PUT /api/challenges/{id}/close` — cierra el challenge, calcula los puntajes ponderados y crea
  los `ChallengeResult` del top 3 (solo admin; sin scheduler/batch — se dispara a mano, según el
  requisito de Etapa 1 de "tiempo real, sin batch/async").
- `PUT /api/challenges/{id}/opportunity-pick` — fija `opportunityPickSubmissionId` (solo el
  `guestArtistId` de ese challenge).
- `GET /api/challenges/results` — histórico de resultados (filtro: producerId) — usado por el
  portfolio y para reconstruir el Music Score.
- `GET /api/ranking` — ranking global, suma de `ChallengeResult.pointsAwarded` por producer.

Códigos HTTP estándar: 200/201 éxito, 400 validación, 401 no autenticado, 403 rol/ownership
incorrecto, 404 no encontrado.

## Arquitectura y capas

Sin cambios respecto al diseño anterior: monolito Spring Boot, cuatro paquetes verticales, uno
por integrante:

```
com.mgwprod.users/{controller,service,repository,model}
com.mgwprod.catalog/{controller,service,repository,model}
com.mgwprod.collab/{controller,service,repository,model}      <- reemplaza a orders
com.mgwprod.challenges/{controller,service,repository,model}
```

Mismas responsabilidades por capa (Controller solo HTTP, Service con la lógica de negocio,
Repository `JpaRepository` sin lógica, Entity mapeada 1:1 a tabla) y mismas convenciones ya
validadas en `users`: sin DTOs (controllers reciben/devuelven la entidad JPA directo),
`jakarta.persistence.validation.mode=none` a nivel app (validar siempre con `@Valid
@RequestBody` en el controller, nunca confiar en Hibernate), `ddl-auto=none` con schema manual
en `docs/db/schema.sql`, auth casera con `Session` (SHA-256 + salt, sin Spring Security todavía).

## Qué se borra / qué se agrega

- **Se elimina** el paquete `com.mgwprod.orders` completo (`CartItem`, `Order`, `OrderItem`,
  controllers, services, repositories, tests) — ya no representa nada del dominio nuevo. No se
  archiva: no aporta como referencia de patrón porque `collab` no es un CRUD simple sino que
  tiene el flujo de estados (`Collaboration.status`) que sí vale la pena mirar en `challenges`
  como precedente de "entidad con estados".
- **Se agrega** el paquete `com.mgwprod.collab` de cero (Dani), sin plan previo que migrar.
- **Se modifica** `catalog`: se quita `price` de `Beat`, se agrega `BeatComment`.
- **Se modifica** `challenges`: se agregan `guestArtistId`/premios/`opportunityPickSubmissionId`
  a `Challenge`, se agrega `ChallengeResult`, se agrega el cálculo ponderado y el endpoint
  `close`.
- **Se modifica** `users`: se agrega `verified` a `ProducerProfile` y el endpoint de admin para
  activarlo.
- `docs/db/schema.sql` necesita reescribirse: sacar las tablas de `orders`, sumar
  `beat_comments`, `toplines`, `comments` (collab), `collaborations`, `challenge_results`, y las
  columnas nuevas de `beats`/`challenges`/`producer_profiles`.

## Frontend (Etapa 2) — impacto del pivot

- `catalog`: `catalog.html` pierde cualquier CTA de compra; suma sección de comentarios por beat.
- `collab` (nuevo, páginas de Dani): `topline-upload.html` (subir interpretación sobre un beat),
  `collaborations.html` (bandeja del productor para aceptar/rechazar).
- `challenges`: `challenge-detail.html` muestra premios, jurado invitado, y el resultado final
  con badges cuando el challenge está cerrado; `ranking.html` sin cambios de fondo.
- `users`: se agrega `profile.html` como portfolio — combina `GET /api/beats?producerId=`,
  `GET /api/toplines?artistId=`, `GET /api/challenges/results?producerId=` en el cliente.
- Resto del patrón de Etapa 2 sin cambios: `api.js` compartido, localStorage para el token,
  manejo de loading/error/success por página, validación HTML5 + defensa en profundidad.

## Etapa 1 — checklist de requisitos mínimos

Igual que el spec anterior (arquitectura por capas, persistencia SQL, CRUD, lógica de negocio,
endpoints, validación, manejo de errores, doc básica) — el pivot no afecta ninguno de estos
puntos, solo cambia el dominio sobre el que se aplican. La única fila que cambia de contenido es
"Lógica de negocio": antes era checkout + Music Score, ahora es cálculo ponderado de challenges
+ flujo de estados de `Collaboration` + verificación de productores.

## Preguntas abiertas / a confirmar

- Repartición concreta de los 4 módulos entre Santiago, Mateo, Paolo y Dani bajo el nuevo
  dominio — pendiente, a definir por el grupo (Dani queda como dueño natural de `collab`, dado
  que reemplaza a su módulo anterior).
- Fechas límite de Etapa 1 y Etapa 2 — aún no confirmadas por la cátedra.
- Si Spring Security se cubre en clases posteriores, evaluar migrar la auth casera (sin cambios
  respecto al spec anterior).
- El merge pendiente de `refactor/users-remove-dtos` es independiente de este pivot — no lo
  toca, pero conviene mergearlo antes de arrancar `collab`/`catalog`/`challenges` para que todos
  partan del mismo patrón sin DTOs.
