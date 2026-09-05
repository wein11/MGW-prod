# mgw-prod — Correcciones del profesor: roles, CRUD completo y suscripción/pago

**Fecha:** 2026-09-04
**Autores:** Santiago Weinbinder, Paolo Maffei, Dani Gariboldi
**Estado:** aprobado, pendiente de escribir los planes de implementación de CRUD y Billing
**Amplía:** `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` (sigue vigente; este
documento describe únicamente lo que cambia)

## Por qué este cambio

El profesor devolvió 3 correcciones sobre el diseño vigente:

1. Tienen que existir **3 roles**: admin, artista y discográfica.
2. **Todas las entidades** deben tener CRUD funcional (hoy ninguna tiene delete, y varias ni
   siquiera tienen update).
3. Tiene que haber un **flujo de pago** — simulado pero fácilmente reemplazable por una pasarela
   real (Mercado Pago u otra) — para un modelo de suscripción: plan free (hasta 50
   canciones/producciones) y plan premium ilimitado a 15 USD.

## 1. Roles

`Role` pasa de `{PRODUCER, ARTIST}` a `{ARTIST, DISCOGRAFICA, ADMIN}`:

- **`ARTIST`** absorbe todo lo que hoy hacen `PRODUCER` y `ARTIST` por separado — cualquier
  artista puede publicar beats/producciones y subir toplines/voces. Deja de existir la
  distinción productor/artista como roles separados.
- **`DISCOGRAFICA`** (rol nuevo): por ahora su única capacidad distintiva es **crear
  challenges** (antes solo `ADMIN` podía). No tiene profile propio ni gestiona artistas todavía
  — "firmar/representar artistas" queda fuera de esta entrega, documentado como próximo paso.
- **`ADMIN`** desaparece como flag booleano (`users.is_admin`) y pasa a ser un valor del enum
  `Role`. Mismas capacidades que hoy: verificar artistas, cerrar challenges, crear challenges.

### Perfiles: fusión de `producer_profiles` + `artist_profiles`

Como `PRODUCER` deja de existir como rol separado, las dos tablas de perfil se fusionan en una
sola `artist_profiles`, con la unión de columnas de ambas:

```
artist_profiles: id, user_id (FK única a users), genres, bio, bpm_min, bpm_max,
                  experience_level, verified
```

Todo usuario con `role=ARTIST` tiene exactamente un `ArtistProfile`, creado al registrarse (como
ya pasa hoy con `ProducerProfile`/`ArtistProfile` por separado). `DISCOGRAFICA` y `ADMIN` no
tienen profile de este tipo.

### Migración de datos

`docs/db/schema.sql` se reescribe completo con el modelo final (sin `is_admin`, sin
`producer_profiles`, con `artist_profiles` fusionada). No se migra el contenido de la base local
existente — es todo dato de smoke test, no hay nada productivo que preservar — cada integrante
recrea su base local (`DROP DATABASE` + recrear desde el `schema.sql` nuevo).

### Autorización actualizada

| Acción | Antes | Ahora |
|---|---|---|
| Publicar beat / subir topline | `role == PRODUCER` | `role == ARTIST` |
| Verificar artista (antes "verificar productor") | solo admin (`isAdmin == true`) | solo `role == ADMIN` |
| Crear challenge | solo admin | `role == ADMIN` **o** `role == DISCOGRAFICA` |
| Cerrar challenge / opportunity-pick | solo admin / guestArtistId | sin cambios |

`ProducerVerificationController` se renombra a `ArtistVerificationController`; el endpoint pasa
de `PUT /api/producers/{id}/verify` a `PUT /api/artists/{id}/verify`.

**Dueño de esta parte: Santiago.** Es la base — bloquea a las otras dos, porque tanto el CRUD
(autorización dueño-o-admin) como Billing (quién tiene suscripción) asumen el enum `Role` nuevo.
Plan de implementación: `docs/superpowers/plans/2026-09-04-mgw-prod-roles-migration.md`.

## 2. CRUD completo en las entidades principales

Se agrega update+delete donde falta, con autorización dueño-o-admin y **borrado físico** (no
soft-delete). Se prioriza el requisito de "CRUD funcional" sobre las entidades sustantivas del
dominio; las entidades de registro histórico (comentarios, submissions, votos) quedan como
create+read porque borrarlas/editarlas rompe la integridad de un challenge cerrado o de una
conversación, sin aportar nada a la evaluación de "CRUD por entidad".

| Entidad | Antes | Se agrega |
|---|---|---|
| `User` | C, R, U | **D** — `DELETE /api/users/{id}`. Devuelve 409 si el usuario tiene al menos un beat/topline/challenge propio (no se cascada: el blast radius de borrar un usuario con historial es demasiado grande). |
| `Beat` | C, R | **U** `PUT /api/beats/{id}` (solo el dueño). **D** `DELETE /api/beats/{id}` (dueño o admin) — cascada a `beat_comments` y a los `toplines` construidos sobre ese beat (que a su vez cascadean a `collaborations`/`comments`). |
| `Topline` | C, R | **U** `PUT /api/toplines/{id}` (solo el dueño). **D** `DELETE /api/toplines/{id}` (dueño o admin) — cascada a `collaborations` y `comments` de ese topline. |
| `Collaboration` | C(implícito), R, U(status) | **D** `DELETE /api/collaborations/{id}` (cualquiera de las dos partes — artista o dueño del beat — o admin). |
| `Challenge` | C, R, U(solo opportunity-pick) | **U genérico** `PUT /api/challenges/{id}` (título/theme/deadline, solo si no está cerrado; solo el creador o admin). **D** `DELETE /api/challenges/{id}` (creador o admin) — cascada a `submissions` → `votes` → `challenge_results`. |

`BeatComment`, `Comment` (collab), `Submission`, `Vote` quedan sin cambios (create+read).

**Dueño de esta parte: Paolo.** Arranca desde `main` una vez mergeada la parte de Roles (el
chequeo dueño-o-admin usa `role == ADMIN`). Plan de implementación:
`docs/superpowers/plans/2026-09-04-mgw-prod-crud-completion.md`.

## 3. Suscripción y pago simulado (módulo `billing`)

Nuevo paquete vertical `com.mgwprod.billing` (mismo patrón que `catalog`/`collab`/`challenges`).

### Modelo

- **`Subscription`**: `id, userId (FK única a users), plan (FREE | PREMIUM), createdAt`. Se crea
  automáticamente en `FREE` cuando se registra un usuario `role=ARTIST` (mismo momento en que
  hoy se crea su `ArtistProfile`). `DISCOGRAFICA` y `ADMIN` no tienen suscripción — no suben
  contenido, no aplica el límite.
- **`PaymentGateway`** (interfaz): `PaymentResult charge(Long userId, BigDecimal amount)`.
  Implementación `SimulatedPaymentGateway`: siempre aprueba. El service depende de la interfaz,
  nunca de la implementación concreta — mañana, una `MercadoPagoGateway implements
  PaymentGateway` se enchufa sin tocar `SubscriptionService` ni ningún otro consumidor.
- **Límite del plan free**: 50 producciones combinadas (`beats` + `toplines` del mismo usuario,
  contadas juntas, no por separado). El conteo **vive dentro de `billing`**, como un campo
  `productionsCount` en `Subscription` — no se calcula sumando `COUNT` sobre las tablas de
  `catalog` y `collab`, porque eso obligaría a `catalog` y `collab` a importarse mutuamente para
  sumar sus conteos (violando la regla de dependencia unidireccional: hoy `collab` depende de
  `catalog`, nunca al revés). En cambio, `BeatService.create` y `ToplineService.create` llaman a
  `SubscriptionService.recordProduction(userId)` justo antes de guardar: ese método incrementa
  `productionsCount` y, si `plan == FREE` y ya llegó a 50, lanza `SubscriptionLimitExceededException`
  (403) sin persistir nada. Es la única dependencia cruzada — `catalog` y `collab` dependen de
  `billing`, igual que ya dependen de `users`, nunca al revés entre ellos. El contador no
  decrementa si se borra un beat/topline (refleja el total histórico publicado, no el catálogo
  activo — evita gamear el límite con publicar/borrar).
- El plan premium **no expira** — una vez aprobado el pago simulado, el usuario queda `PREMIUM`
  hasta que decida bajarlo (no hay fecha de renovación ni job de vencimiento).

### Endpoints

- `GET /api/subscriptions/me` — plan actual + uso (`37/50` si es `FREE`, `unlimited` si es
  `PREMIUM`).
- `POST /api/subscriptions/upgrade` — cobra 15 USD vía `PaymentGateway` (simulado, siempre
  aprueba), pasa a `PREMIUM`.
- `PUT /api/subscriptions/downgrade` — vuelve a `FREE`. Si el usuario ya tiene más de 50
  producciones, las conserva todas — solo deja de poder subir nuevas hasta volver a `PREMIUM`.

No hay `DELETE`: todo `ARTIST` tiene siempre exactamente una fila de `Subscription`; "cancelar"
es actualizar el plan a `FREE`, no borrar el registro.

**Dueño de esta parte: Dani.** Arranca desde `main` una vez mergeada la parte de Roles. Plan de
implementación: `docs/superpowers/plans/2026-09-04-mgw-prod-billing-module.md`.

## Orden de ejecución

```
Roles (Santiago) ──merge a main──> CRUD completion (Paolo)
                  └──merge a main──> Billing (Dani)
```

`CRUD completion` y `Billing` pueden desarrollarse en paralelo entre sí (tocan módulos
distintos: el primero toca `catalog`/`collab`/`challenges`/`users`, el segundo agrega
`billing` de cero y solo suma 2 chequeos en `catalog`/`collab`), pero ambos necesitan que
`Roles` ya esté en `main` antes de arrancar.

## Qué se borra / qué se agrega

- **Se elimina**: `Role.PRODUCER`, `users.is_admin`, la tabla `producer_profiles`, la clase
  `ProducerProfile`, `ProducerVerificationController`.
- **Se agrega**: `Role.DISCOGRAFICA`, `Role.ADMIN`, columnas fusionadas en `artist_profiles`,
  `ArtistVerificationController`, `DELETE`/`PUT` faltantes en `User`/`Beat`/`Topline`/
  `Collaboration`/`Challenge`, el paquete `com.mgwprod.billing` completo.
- `docs/db/schema.sql` se reescribe completo (no aditivo esta vez): nuevo `role` enum, sin
  `is_admin`, `artist_profiles` fusionada, sin `producer_profiles`, tabla `subscriptions` nueva.
- `docs/api/*.md` y `docs/api/*.http`: actualizar `users-API.md` (roles, endpoint de
  verificación renombrado), `catalog-API.md`/`collab-API.md`/`challenges-API.md` (nuevos
  verbos U/D), y crear `billing-API.md`.

## Preguntas abiertas / a confirmar

- "Firma/representa artistas" de `DISCOGRAFICA` queda fuera de esta entrega — si el profesor
  lo pide explícitamente más adelante, es una relación nueva (`Label` ↔ `ArtistProfile`) a
  diseñar aparte.
- Si en algún momento se necesita expiración/renovación real de la suscripción premium, hay que
  sumar `expiresAt` a `Subscription` y un job de chequeo — no está en el alcance actual.
