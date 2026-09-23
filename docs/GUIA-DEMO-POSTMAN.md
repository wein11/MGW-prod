# Guion de la demo en vivo (Etapa 1)

Colección: `docs/api/mgw-prod-demo.postman_collection.json` (16 requests).
En `entrega/etapa1` esa misma colección se llama `mgw-prod.postman_collection.json`.
La colección completa de 53 requests (`mgw-prod.postman_collection.json` en main) queda para
repasar en casa, no para la defensa.

## Antes de empezar (5 minutos antes)

1. MySQL prendido y la base creada:
   ```bash
   mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
   mysql -u root -padmin mgw_prod < docs/db/schema.sql
   ```
2. `./mvnw spring-boot:run` y esperar `Started MgwProdApplication`.
3. En Postman: importar la colección + `mgw-prod.postman_environment.json`, y elegir el
   environment **mgw-prod local** arriba a la derecha.
4. Ensayo: correr la colección entera una vez (Run collection). Tiene que dar 16/16 en verde.
   Se puede correr las veces que haga falta: cada corrida usa emails nuevos.

**Mandar los requests en orden.** Cada uno guarda en el environment lo que necesita el
siguiente (token, id del beat, id del desafío...). Si alguien se saltea uno, el siguiente falla.

## Lo que hay que saber explicar de Postman

- **`{{base_url}}`**: variable del environment (`http://localhost:8080`). Si mañana el backend
  estuviera en otro servidor, se cambia solo ahí.
- **Pestaña Tests** (se ejecuta *después* de la respuesta): chequea el código de estado
  (`pm.response.to.have.status(201)`) y guarda datos para los requests siguientes
  (`pm.environment.set("beatId", pm.response.json().id)`).
- **Pestaña Pre-request Script** (se ejecuta *antes* de mandar el request): se usa para generar
  un email distinto cada vez (`Date.now()`) y, en el DELETE, para crear el beat que se va a borrar
  (lo mismo que se vio en la Clase 5).
- **Header `Authorization: Bearer {{artistToken}}`**: el token que devolvió el login. Sin él,
  los endpoints que necesitan sesión responden 401.

## Request por request

### 1. Usuarios — Santiago

| # | Request | Qué mostrar / decir | Código que corre |
|---|---|---|---|
| 1 | Registrar artista → **201** | Body con email, password, rol. En la respuesta **no** aparece la contraseña: se guarda solo el hash. | `AuthController.register` → `AuthService.register` → `PasswordHasher.hash` |
| 2 | Login artista → **200** | Devuelve un `token`. Postman lo guarda en `artistToken`. | `AuthService.login` crea una `Session` |
| 3 | Login admin → **200** | El admin no se registra: viene cargado en `schema.sql` (`admin@mgw.com` / `admin1234`). Guarda `adminToken`. | |

Pregunta probable: *¿de dónde sale el admin?* → viene cargado en `docs/db/schema.sql` con la
contraseña ya hasheada; registrarse como ADMIN da 403 (lo mostramos al final, request 16).

Pregunta probable: *¿cómo sabe el backend quién hace cada request?* → `SessionAuthInterceptor`
lee el header `Authorization`, busca el token en la tabla `sessions` y deja el `userId` en el
request; el controller lo recibe con `@RequestAttribute`.

### 2. Beats (CRUD completo) — Santiago + Mateo

| # | Request | Qué mostrar / decir | Código que corre |
|---|---|---|---|
| 4 | Crear beat → **201** | POST con token de artista. El `producerId` no va en el body: lo saca el backend de la sesión. | `BeatController.createBeat` → `BeatService.create` |
| 5 | Listar beats → **200** | GET público, sin token. Acepta filtros `?genre=Trap&bpm=140`. | `BeatService.list` |
| 6 | Editar beat → **200** | PUT parcial: solo cambia `title` y `bpm`, el resto queda igual. | `BeatService.update` |
| 7 | Borrar beat → **204** | Mostrar el Pre-request Script: primero crea un beat y después lo borra. 204 = OK sin body. | `BeatService.canModify` + `delete` |

Pregunta probable: *¿qué pasa si otro usuario quiere editar tu beat?* → `canModify` devuelve
false y el controller responde **403**.

### 3. Toplines y suscripción — Dani

| # | Request | Qué mostrar / decir | Código que corre |
|---|---|---|---|
| 8 | Crear topline sobre el beat → **201** | El body lleva `"beat": {"id": ...}` porque `Topline` tiene una relación `@ManyToOne` con `Beat`. Al crearlo se crea sola una `Collaboration` en PENDING. | `ToplineService.create` |
| 9 | Pasar a plan premium → **200** | El pago es simulado (`SimulatedPaymentGateway`, siempre aprueba). La respuesta muestra `plan: PREMIUM`. | `SubscriptionService.upgrade` |

Pregunta probable: *¿para qué sirve el plan?* → el plan FREE tiene un límite de 50
producciones (beats + toplines); PREMIUM no tiene límite.

### 4. Desafíos — Paolo

| # | Request | Qué mostrar / decir | Código que corre |
|---|---|---|---|
| 10 | Crear desafío (admin) → **201** | Solo ADMIN o DISCOGRAFICA pueden crear. `guestArtistId` es el artista invitado que hace de jurado. | `ChallengeService.canCreateChallenge` + `create` |
| 11 | Enviar entrega → **201** | El artista sube su audio antes del deadline. | `SubmissionService.create` |
| 12 | Votar la entrega → **201** | Puntaje de 1 a 10. Un mismo usuario no puede votar dos veces. | `VoteService.alreadyVoted` + `create` |
| 13 | Cerrar desafío y ver podio → **200** | Calcula el puntaje de cada entrega y devuelve el top 3 con puntos y premio. | `ChallengeResultService.close` + `ChallengeScoringService.computeScore` |

Pregunta probable: *¿cómo se calcula el puntaje?* → 30% promedio de la comunidad + 30%
promedio de productores verificados + 40% voto del artista invitado. El podio se ordena con un
ordenamiento por selección (dos `for`).

### 5. Errores — cualquiera

| # | Request | Qué decir |
|---|---|---|
| 14 | Crear beat sin token → **401** | No hay header `Authorization`, entonces `userId` llega null y el controller corta con 401. |
| 15 | Beat que no existe → **404** | `BeatService.getById` devuelve null y el controller responde 404. |
| 16 | Registrar un admin → **403** | `AuthController.register` no deja crear admins por la API: si cualquiera pudiera, cualquiera tendría permisos de administrador. |

Si piden otro error en vivo, se puede armar a mano:
- **400**: Crear beat con `"title": ""`.
- **403**: "Crear desafío" pero con `{{artistToken}}` en vez de `{{adminToken}}` (un artista no
  puede crear desafíos). Ojo: editar un beat con el token de admin **no** da 403, porque el
  admin puede editar cualquier beat.
- **409**: Registrar dos veces el mismo email.

## Si algo falla en vivo

- **Connection refused**: la app no está corriendo (`./mvnw spring-boot:run`).
- **401 en todo**: no está elegido el environment, o no se corrió el login.
- **404 en un request del medio**: se salteó un request anterior que guardaba ese id. Volver a
  correr desde "Crear beat".
