# Guía técnica — mgw-prod (para el equipo)

> Este documento existe para que los 4 podamos explicar y defender el proyecto completo
> ante el profesor — no solo tu módulo, sino por qué está construido así en general y qué
> hace cada uno de los otros tres. La evaluación es individual aunque el trabajo sea grupal:
> si te preguntan algo de un módulo que no es el tuyo, acá está la respuesta.
>
> Complementa al `README.md` (que tiene el setup para correr el proyecto) y a
> `docs/api/*-API.md` (referencia de endpoints). Este documento se enfoca en el **por qué**
> de las decisiones y en la **lógica de negocio real** que está en el código, no solo en la
> lista de endpoints.

## 0. Lo primero que hay que saber

`mgw-prod` es una plataforma de descubrimiento y colaboración musical: productores suben
beats, artistas graban toplines (voces) sobre esos beats, se arma una colaboración, y hay
challenges semanales con jurado ponderado y ranking. **No es e-commerce** — el PDF de la
consigna (`docs/Trabajo Integrador de Aplicaciones Interactivas.pdf`) pide "funcionalidades
de e-commerce" porque es el enunciado genérico de la cátedra, pero el profesor autorizó
verbalmente el pivot el 2026-09-01. No hay carrito, checkout, ni pagos reales en ningún lado
del código (verificado por grep sobre todo `src/main/java`: cero coincidencias de
`carrito|checkout|cart|CartItem`). Si el profesor pregunta por qué el PDF dice una cosa y el
código otra, esa es la respuesta completa — diseño vigente en
`docs/design/specs/2026-09-01-mgw-prod-pivot-design.md`.

Es un monolito Spring Boot con MySQL, sin frontend todavía (Etapa 2, HTML/CSS/JS plano, sin
frameworks, es la próxima entrega).

## 1. Arquitectura general

### Flujo de una request

```
Cliente HTTP (Postman/curl)
  → SessionAuthInterceptor        (solo si viene header Authorization)
  → Controller (@RestController)  → valida con @Valid, lee el userId del request
  → Service (@Service)            → TODA la lógica de negocio y autorización vive acá
  → Repository (Spring Data JPA)  → sin lógica, solo queries derivadas por nombre
  → MySQL                         (esquema creado a mano, no por Hibernate)
  → la misma Entity JPA vuelve como respuesta (sin DTO intermedio)
```

Ejemplo real, trazado en el código: `POST /api/beats` entra a
`BeatController.createBeat` (`catalog/controller/BeatController.java`), que valida
`@Valid @RequestBody Beat` y que haya un `userId` autenticado. Llama a
`BeatService.create` (`catalog/service/BeatService.java`), que resuelve el `User` real por
FK, confirma que su rol sea `ARTIST`, le avisa a `SubscriptionService.recordProduction`
(de otro módulo — el límite del plan free aplica acá) y recién ahí guarda el beat con
`BeatRepository.save`.

**Por qué esta arquitectura y no otra: son las mismas 4 capas que enseñó la cátedra en
Clase 4** (Controller/Model/Repository/Service) — no inventamos nada nuevo, solo
reorganizamos cómo se agrupan los paquetes (ver más abajo).

### Por qué los paquetes están organizados por módulo y no por capa

El ejemplo de cátedra (`webcampus`, Clase 4) organiza todo **por capa, plano**:
`controller/`, `model/`, `repository/`, `service/` en la raíz, todo junto. Nosotros
organizamos **por módulo/dueño primero, y recién adentro por capa**:

```
com.mgwprod
├── users/        controller/ model/ repository/ service/ security/ config/
├── catalog/      controller/ model/ repository/ service/
├── collab/       controller/ model/ repository/ service/
├── challenges/   controller/ model/ repository/ service/
├── billing/      controller/ model/ repository/ service/ gateway/
└── common/       dto/ exception/  (compartido entre todos)
```

Es deliberado, no un desvío: somos 4 integrantes con evaluación individual, y necesitamos
que cada uno tenga su paquete autocontenido para poder explicarlo sin pisar el código de
los demás en la misma carpeta `controller/`. La única diferencia real con el ejemplo de
cátedra es el nivel de anidamiento — las capas de adentro son exactamente las mismas.

### Por qué JPA y no DAO manual

Confirmado explícitamente por la cátedra en Clase 4: ya no se usa el patrón DAO manual.
Todos nuestros repositorios son interfaces `JpaRepository<Entity, Long>` sin
implementación propia (ej. `BeatRepository`), con **queries derivadas por nombre de
método** (`findByProducerId`, `findByGenreAndBpm`, etc.). Cero SQL manual en el código
Java — si necesitás una query nueva, primero probá si Spring Data la puede derivar del
nombre del método antes de escribir `@Query`.

### Por qué no hay DTOs

Los controllers reciben y devuelven la **entidad JPA directo**, calcando el patrón de
Clase 4 (`docs/design/plans/2026-08-29-remove-users-dtos.md`). Consecuencia técnica
importante — y esta es la parte que vale la pena poder explicar si preguntan:

`application.properties` tiene `spring.jpa.properties.jakarta.persistence.validation.mode=none`
seteado a nivel de toda la app. El motivo real no es solo estilo: Hibernate revalida el
grupo default de Bean Validation en **cada** `save()`, y `User.password` es un campo
`@Transient` con `@NotBlank` que solo se usa para el registro — nunca se carga desde la
base (es `WRITE_ONLY`). Si Hibernate validara automáticamente, cualquier `save()` sobre un
`User` que venga de un `findById` reventaría con un 500 porque `password` viene `null`.
Por eso la validación se hace **solo** en el controller con `@Valid @RequestBody`, nunca
confiando en que el `save()` la haga sola.

Consecuencia práctica de esto en cada entidad: los campos que el servidor asigna solos
(`Beat.producerId`, `Topline.artistId`, `Challenge.createdBy`, los `authorId` de
comentarios) **no** llevan `@NotNull` — si lo llevaran, el `@Valid` del controller
rechazaría el request antes de que el service tenga chance de completarlos. En vez de eso
usan `@Column(nullable = false)` a nivel de base de datos, y varios además usan
`@JsonProperty(access = JsonProperty.Access.READ_ONLY)` para que ni siquiera se acepten
si vienen en el JSON que manda el cliente (evita que alguien se haga pasar por otro
usuario mandando un `producerId` ajeno).

### Por qué el esquema SQL es manual

`spring.jpa.hibernate.ddl-auto=none` — Hibernate no crea ni modifica tablas, todo vive a
mano en `docs/db/schema.sql` (13 tablas, una por cada `@Entity`). Si agregás una entidad
nueva, tenés que sumar su `CREATE TABLE` ahí vos mismo — Spring no lo hace solo. Ojo con
un detalle real del schema: la columna de posición en `challenge_results` se llama
`rank_position` y no `rank`, porque `RANK` es palabra reservada en MySQL.

### Manejo de errores (compartido por los 5 módulos)

Vive en `common/exception/`:
- `ApiException` — clase abstracta base, cada excepción de negocio la extiende y lleva su
  propio `HttpStatus` (ej. `BeatNotFoundException` → 404, `ForbiddenOperationException` →
  403, `UnauthenticatedException` → 401, `SubscriptionLimitExceededException` → 403).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza todo: mapea cada
  `ApiException` a su status, `MethodArgumentNotValidException` (falla de `@Valid`) a 400,
  JSON malformado a 400, y cualquier excepción no prevista a 500 (logueada).
- `ErrorResponse` es el único "DTO" real de todo el proyecto — está justificado porque no
  es una entidad de dominio, es la forma de una respuesta de error (`status`, `message`,
  `timestamp`).

## 2. Autenticación — cómo funciona exactamente

No usamos Spring Security (no se vio en clase todavía). Es casera:

1. **Hasheo de password**: `PasswordHasher` usa SHA-256 con salt aleatorio de 16 bytes
   (`SecureRandom`), guardado como `base64(salt):base64(hash)`. La comparación al hacer
   login usa `MessageDigest.isEqual` (comparación a tiempo constante — buena práctica
   contra timing attacks, vale la pena mencionarlo si preguntan de seguridad).
2. **Login** genera una `Session` con `token = UUID` random y vencimiento a las 24hs.
3. **`SessionAuthInterceptor`** (registrado sobre `/api/**`, excepto `/api/auth/**`)
   intercepta cada request:
   - Sin header `Authorization: Bearer <token>` → deja pasar la request **sin userId**.
   - Con header pero token inválido/vencido → responde **401 directo desde el
     interceptor**, ni siquiera llega al controller.
   - Con token válido → guarda `userId` (y `userRole`) en el request y deja pasar.
4. Cada endpoint que escribe datos lee ese `userId` con
   `@RequestAttribute(required = false)`, y si es `null` tira `UnauthenticatedException`
   (401) él mismo.

**Detalle importante para poder explicar si preguntan:** con este diseño, todos los
endpoints `GET` son efectivamente públicos — no exigen token, porque sin header el
interceptor simplemente deja pasar. Solo los endpoints que escriben (`POST`/`PUT`/`DELETE`)
exigen un `userId` real, y eso solo pasa si mandaste un `Bearer` válido.

**Otro detalle curioso:** el interceptor también guarda `userRole` en el request, pero
**ningún controller ni service lo usa** — toda la autorización por rol se re-consulta
siempre desde la base de datos dentro de cada Service (nunca confiando en lo que puso el
interceptor). Es una defensa en profundidad correcta (no confiar en datos que ya
"decidiste" antes), pero si te preguntan por qué existe `userRole` sin que nadie lo lea, es
por eso — quedó como pieza vestigial de una versión anterior del diseño.

## 3. Los 5 módulos

### `users` — dueño: Santiago
Auth, perfiles de artista, verificación.

**Entidades:** `User` (email único, `passwordHash` oculto en las respuestas, rol
`ARTIST`/`DISCOGRAFICA`/`ADMIN`), `ArtistProfile` (1 a 1 con `User`, solo lo tienen los
`ARTIST` — géneros, bio, rango de BPM, nivel de experiencia, `verified`), `Session` (token
+ vencimiento).

**Regla no obvia:** al registrarte, solo si tu rol es `ARTIST` se crea automáticamente tu
`ArtistProfile` — `DISCOGRAFICA` y `ADMIN` se registran sin perfil.

**Borrado de usuario:** es físico (no hay soft-delete). Si tenés contenido asociado (beats,
toplines, etc.) el borrado falla con 409 en vez de romper por una excepción cruda de MySQL
— hay un `flush()` explícito para forzar el `DELETE` dentro de la misma transacción y poder
capturarlo limpio.

### `catalog` — dueños: Santiago + Mateo
Publicar y listar beats, comentarios sobre beats.

**Entidades:** `Beat` (dueño = `producerId`, sin `price` — se sacó en el pivot),
`BeatComment`.

**Regla no obvia:** publicar un beat primero le pregunta a `billing` (`SubscriptionService
.recordProduction`) si el artista todavía tiene lugar en su plan — si llegó al límite de 50
del plan free, el beat **no se guarda** y devuelve 403.

### `collab` — dueño: Dani
Toplines de artistas sobre beats ajenos, comentarios, colaboraciones. Reemplazó al módulo
`orders` del diseño e-commerce original que se descartó en el pivot.

**Entidades:** `Topline` (artista + beat), `Collaboration` (estado `PENDING` →
`ACCEPTED`/`REJECTED`), `Comment`.

**Máquina de estados real:** crear un `Topline` crea automáticamente, en la misma
transacción, una `Collaboration` en `PENDING`. Quien decide aceptar/rechazar tiene que ser
específicamente **el productor dueño del beat** (no el artista, no un tercero) — el service
resuelve `Collaboration → Topline → Beat` para verificarlo.

**Depende de `catalog`** (importa `BeatRepository` para validar el beat) — la dependencia
va en un solo sentido, `catalog` nunca importa nada de `collab`.

### `challenges` — dueño: Paolo
El módulo más complejo: desafíos con jurado ponderado, submissions, votos, resultados,
ranking.

**Entidades:** `Challenge` (incluye un `guestArtistId` que tiene que ser rol `ARTIST`),
`Submission`, `Vote` (score 1-10, un voto por persona por submission — constraint `UNIQUE`
en la base), `ChallengeResult`.

**El cálculo ponderado, tal cual está en el código** (`ChallengeScoringService`):

```
score = 0.30 × promedio de votos de "comunidad"
      + 0.30 × promedio de votos de productores/artistas verificados
      + 0.40 × voto del artista invitado (0.0 si no votó — no se renormaliza)
```

La categoría del votante no se guarda en la base — se calcula al momento de cerrar el
challenge, comparando el `voterId` contra el `guestArtistId` del challenge y contra la
lista de artistas verificados.

**Al cerrar un challenge** (solo `ADMIN` puede, endpoint `PUT .../close`): se ordenan las
submissions por score, se toma el top 3 (hardcodeado), y se reparten puntos fijos
(500/300/150). **Dato que solo está en el código, no en ningún doc de diseño:** el
ganador (puesto 1) queda automáticamente **verificado** (`ArtistProfile.verified = true`)
como efecto secundario del cierre — si el profesor pregunta "¿cómo te verificás?", la
respuesta real incluye "ganando un challenge", no solo la verificación manual de admin.

Un challenge que ya tiene resultados **no se puede editar ni borrar**. El ranking
(`GET /api/ranking`, el "Music Score") no está guardado en ninguna columna — se recalcula
sumando puntos en memoria cada vez que se pide.

### `billing` — dueño: Dani (5to módulo, se sumó el 2026-09-04)
Suscripción free/premium, límite de producciones, "pago" simulado. Este módulo es la
respuesta a la corrección del profesor que pedía algún flujo de pago, sin volver al
e-commerce transaccional descartado en el pivot.

**Entidad:** `Subscription` (plan `FREE`/`PREMIUM`, contador de producciones).

**Gateway simulado:** `PaymentGateway` es una interfaz con una sola implementación,
`SimulatedPaymentGateway`, que **siempre aprueba** (nunca falla, no hay integración real
con ninguna pasarela). Está diseñada a propósito para que en el futuro se pueda enchufar
una implementación real (ej. Mercado Pago) sin tocar `SubscriptionService` — es el patrón
Strategy que separa "qué hace el negocio" de "cómo se cobra".

**Reglas de negocio:**
- Solo los `ARTIST` tienen suscripción — `DISCOGRAFICA`/`ADMIN` no.
- `upgrade()` es idempotente: si ya estás en `PREMIUM`, no te vuelve a cobrar.
- El contador de producciones (`productionsCount`) **nunca baja** — ni al hacer downgrade
  ni al borrar un beat/topline. Es un total histórico, no el tamaño del catálogo activo.
- Límite del plan free: 50 producciones (constante hardcodeada en el service).

**`billing` nunca depende de `catalog` ni `collab`** — la dependencia va al revés (ellos le
preguntan a `billing`), así se evita cualquier ciclo entre módulos hermanos.

## 4. Mapa de dependencias entre módulos

```
users  ←─────────┬── catalog
  ↑               │
  │               ├── collab ──→ catalog (FK a Beat)
  │               │
  ├── billing ←───┴── collab   (ambos llaman a billing.recordProduction)
  │
  └── challenges  (solo lee User/ArtistProfile por FK, no importa catalog ni collab)
```

Regla de diseño explícita y verificada en el código: **la dependencia entre módulos
hermanos siempre va en un solo sentido, nunca hay ciclos.** `billing` y `challenges` son
los módulos "de abajo" — otros dependen de ellos, ellos no dependen de nadie más que
`users`.

## 5. Tests

**164 tests, 0 failures** (verificado corriendo `./mvnw test`), organizados por módulo
calcando la estructura de `src/main`, con estos tipos:

- **`*ServiceTest`** — unit tests con Mockito, mockean repositorios y services de otros
  módulos.
- **`*ControllerTest`** — `@WebMvcTest` + `MockMvc`, verifican status codes y forma del
  JSON de respuesta.
- **`*RepositoryTest`** — `@DataJpaTest` contra H2 en memoria (nota: **solo en los tests**
  se deja que Hibernate genere el schema automáticamente sobre H2 — la app real sigue
  usando el schema manual sobre MySQL).
- Tests puntuales de seguridad (`PasswordHasherTest`, `SessionAuthInterceptorTest`) y de
  serialización JSON (que `password` nunca salga en una respuesta, por ejemplo).

## 6. Limitaciones conocidas (ya documentadas, no son sorpresa si preguntan)

Del `README.md`, sección "Limitaciones conocidas" — dos condiciones de carrera
identificadas y no corregidas todavía porque la solución (locking pesimista o un `UPDATE`
atómico) es una técnica de JPA que no vimos en clase:

- **`SubscriptionService.recordProduction`**: dos publicaciones casi simultáneas del mismo
  artista justo en el límite de 50 pueden pasar ambas la validación antes de que se guarde
  el contador.
- **`SubscriptionService.getOrCreate`**: dos requests simultáneos de un usuario nuevo
  pueden hacer que el segundo falle con 500 en vez de devolver la suscripción que ya creó
  el primero.

Ninguna corrompe datos ni afecta el uso normal/demo.

**Deuda técnica menor, no documentada en ningún otro lado (detectada al mapear el
código):**
- El método `requireOwnerOrAdmin` está duplicado casi idéntico en `BeatService`,
  `ToplineService` y `ChallengeService` — podría extraerse a una clase compartida.
- El campo se sigue llamando `producerId` en `Beat`/`Submission` aunque el rol `PRODUCER`
  ya no existe (se unificó en `ARTIST` en la migración de roles del 2026-09-04) — es solo
  naming heredado, no un bug, pero puede generar una pregunta tipo "¿por qué dice
  `producerId` si el rol es `ARTIST`?".

## 7. Ojo con documentación vieja desactualizada

Dos archivos del repo quedaron desactualizados después de la migración de roles del
2026-09-04 (que unificó `PRODUCER`+`ARTIST` en un solo rol `ARTIST`, y agregó
`DISCOGRAFICA`):

- `docs/descripcion.md` — su sección "Roles de usuario" todavía describe `PRODUCER`
  separado de `ARTIST`.
- El `CLAUDE.md` de la raíz del proyecto — la tabla de arquitectura todavía dice
  `roles (PRODUCER/ARTIST)`.

**El código real y el `README.md` son la fuente de verdad**: los roles que existen hoy son
`ARTIST`, `DISCOGRAFICA`, `ADMIN` — nada más. Si en la defensa alguien lee esos dos
archivos viejos y pregunta por `PRODUCER`, esa es la explicación.

## 8. Cómo correr y probar todo

Ver `README.md` para el setup completo. Resumen: `./mvnw spring-boot:run` (con MySQL
corriendo y el schema cargado), y para probar el CRUD completo en vivo está la Collection
de Postman en `docs/api/` — ver `docs/INFORME-TPO.md` para el detalle de cómo la corrimos y
validamos hoy (48 requests, 0 errores, con Newman y con Postman CLI).
