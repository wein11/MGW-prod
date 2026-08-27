# mgw-prod — Diseño TPO Aplicaciones Interactivas

**Fecha:** 2026-08-25 (actualizado 2026-08-27 — grupo pasó a ser de 4)
**Autores:** Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi
**Estado:** aprobado por Santiago, pendiente de repartir módulos con el resto del grupo (el
split de 3 paquetes verticales de este spec necesita revisarse para 4 personas)

## Contexto y origen

Este documento diseña la primera versión (académica) de una idea de producto más amplia: una
plataforma de descubrimiento y colaboración musical para productores/artistas emergentes, con
challenges semanales, rankings por Music Score, y a futuro un marketplace de servicios y
búsqueda de talento para sellos.

El TPO de la materia exige una **aplicación web transaccional con funcionalidades de
e-commerce**, en dos etapas (backend, luego frontend), con stack fijado por la cátedra (Java +
Spring, SQL). El pitch original no es e-commerce en su núcleo, así que este diseño recorta el
alcance a lo que sí lo es y es defendible con el stack impuesto, dejando el resto del pitch
(matching avanzado, monetización, marketplace de servicios) para una versión posterior fuera
de la materia.

## Restricciones del TPO (según la consigna vigente al 2026-08-25)

**Estas restricciones vienen del profesor y pueden cambiar durante la cursada** (el stack
exigido, las tecnologías obligatorias, o los requisitos mínimos de cada etapa). Este spec
refleja la consigna conocida hoy — si el profesor anuncia un cambio (ej. cambia el framework,
agrega/saca un requisito, o define Spring Security como obligatorio), hay que revisar este
documento y las secciones afectadas antes de seguir implementando esa parte. No tratar nada acá
como definitivo más allá de lo que efectivamente comunique la cátedra en cada clase.

- Java + Spring (framework fijado por la cátedra), SQL, comunicación HTTP, API cliente-servidor.
- Etapa 1: solo backend, demostrable con herramientas HTTP (Postman/curl), corre en localhost.
- Etapa 2: frontend en HTML5/CSS3/JavaScript plano (sin frameworks), consume la API de Etapa 1.
- Arquitectura por capas, CRUD, persistencia SQL, validación de entrada, manejo de errores,
  documentación básica de endpoints.
- Grupo de 4 (Santiago, Mateo, Paolo, Dani); evaluación individual — cada integrante debe poder
  explicar y justificar arquitectura, patrones, y el código de su parte.
- Solo se usa lo visto en clase. Estado real de la cursada al momento de este diseño: Clase 1
  (Java básico), Clase 2 (JDBC/Maven + patrón DAO manual), Clase 3 (esqueleto Spring Boot
  vacío, sin controllers/services todavía). Se asume que Spring Data JPA se cubre más
  adelante en la cursada — si no fuera así, este diseño requiere revisión.

## Alcance del dominio (recortado para el TPO)

- **Marketplace de beats** (núcleo e-commerce): catálogo, carrito, checkout simulado
  (sin gateway de pago real — la orden se crea directamente como pagada).
- **Challenges simplificados**: challenge con brief, submissions de productores, votos de la
  comunidad con comentario opcional, ranking por Music Score. Sin jueces ponderados
  (comunidad/productores verificados/artista invitado) — eso es del pitch original completo,
  fuera de este TPO.
- **Perfiles**: dos roles, `PRODUCER` y `ARTIST`, cada uno con su propio perfil.

Fuera de alcance para este TPO (parte del pitch original, para una versión posterior):
matching avanzado por BPM/género/ubicación, tiers PRO/monetización, búsqueda paga para
sellos/artistas, marketplace de servicios, jueces ponderados con artista invitado real.

## Modelo de dominio

### Módulo `users`
- `User`: id, email, passwordHash, displayName, role (`PRODUCER` | `ARTIST`), city, isAdmin
  (boolean, default false — habilita crear challenges; no es un rol nuevo, solo un flag),
  createdAt.
- `ProducerProfile` (1:1 con User si role=PRODUCER): genres, bpmRange, experienceLevel,
  musicScore (derivado, no se persiste como fuente de verdad — se calcula desde `challenges`).
- `ArtistProfile` (1:1 con User si role=ARTIST): genres, bio.
- `Session`: id, userId, token, expiresAt — soporte de la auth casera.

### Módulo `marketplace`
- `Beat`: id, producerId (FK User), title, genre, bpm, key, price, audioUrl (link externo tipo
  SoundCloud/Drive), createdAt.
- `CartItem`: id, artistId (FK User), beatId (FK Beat), addedAt.
- `Order`: id, artistId (FK User), totalAmount, status (`PAID`), createdAt.
- `OrderItem`: id, orderId (FK Order), beatId (FK Beat), priceAtPurchase.

### Módulo `challenges`
- `Challenge`: id, title, genre, bpm, key, theme, deadline, createdAt.
- `Submission`: id, challengeId (FK Challenge), producerId (FK User), audioUrl, submittedAt.
- `Vote`: id, submissionId (FK Submission), voterId (FK User), score (1–10), comment (opcional).
- Music Score: no es una tabla — se calcula al pedir el ranking o el perfil, así: cada
  `Submission` tiene un puntaje = promedio de sus `Vote.score` (1–10); el Music Score del
  producer = suma de los puntajes de todas sus submissions en todos los challenges.

Regla de dependencia entre módulos: `marketplace` y `challenges` solo referencian `User` por
FK, nunca al revés — así `users` no conoce a los otros dos módulos.

## Endpoints

Formato: JSON (serialización Jackson estándar de Spring, sin justificación adicional
necesaria más allá de "es el formato de facto para APIs REST con Spring").

**`users`**
- `POST /api/auth/register` — crea User + profile según role.
- `POST /api/auth/login` — valida credenciales, devuelve token.
- `GET /api/users/{id}` — perfil público.
- `PUT /api/users/{id}` — editar perfil propio.

**`marketplace`**
- `GET /api/beats` — catálogo (filtros: genre, bpm).
- `POST /api/beats` — publicar beat (rol PRODUCER).
- `GET /api/beats/{id}` — detalle.
- `POST /api/cart/items` — agregar al carrito.
- `DELETE /api/cart/items/{id}` — quitar del carrito.
- `GET /api/cart` — ver carrito.
- `POST /api/orders` — checkout (crea Order + OrderItems desde carrito, status PAID, vacía carrito).
- `GET /api/orders` — historial de compras.

**`challenges`**
- `GET /api/challenges` — listado activos/pasados.
- `POST /api/challenges` — crear challenge (flag admin simple en User, no es un rol nuevo).
- `GET /api/challenges/{id}` — detalle + submissions.
- `POST /api/challenges/{id}/submissions` — enviar producción (rol PRODUCER).
- `POST /api/submissions/{id}/votes` — votar + comentario opcional.
- `GET /api/ranking` — ranking global por Music Score.

Códigos HTTP estándar: 200/201 éxito, 400 validación, 401 no autenticado, 403 rol incorrecto,
404 no encontrado.

## Arquitectura y capas

Monolito Spring Boot (un `pom.xml`, un `mvn spring-boot:run`, una base MySQL), tres paquetes
verticales — cada uno dueño de un integrante:

```
com.mgwprod.users/{controller,service,repository,model,dto}
com.mgwprod.marketplace/{controller,service,repository,model,dto}
com.mgwprod.challenges/{controller,service,repository,model,dto}
```

Responsabilidad de cada capa (misma estructura en los 3 paquetes):

- **Controller**: solo HTTP. Mapea JSON → DTO, valida con `@Valid`, llama al Service, devuelve
  `ResponseEntity<T>`. Nunca toca la Entity ni el Repository directamente.
- **Service**: lógica de negocio (reglas, validaciones de dominio, orquestación de
  Repositories). Si esta lógica viviera en el Controller, quedaría imposible de testear sin
  levantar HTTP y mezclaría dos responsabilidades distintas.
- **Repository**: interfaces `JpaRepository<Entity, Long>` (Spring Data). Sin lógica — solo
  acceso a datos. Si cambia el motor de base de datos, es la única capa (más la config de
  conexión) que se toca.
- **Entity**: clases `@Entity` que representan las tablas 1:1, con `@OneToMany`/`@ManyToOne`
  para las FKs (ej. `Beat` → `User`, `OrderItem` → `Order`).

Ejemplo de flujo de request — `POST /api/orders` (checkout):
1. `OrderController` recibe el request con el token de sesión, llama a `OrderService.checkout(userId)`.
2. `OrderService` busca los `CartItem` del usuario vía `CartRepository`; si está vacío, lanza
   `EmptyCartException`; si no, calcula el total y crea `Order` + `OrderItem`s vía
   `OrderRepository`, y vacía el carrito.
3. Un `@RestControllerAdvice` global captura cualquier excepción de negocio y devuelve el JSON
   de error con el HTTP status correspondiente — centralizado, no repetido por Controller.
4. Validación de entrada con Bean Validation (`@NotBlank`, `@Min`, etc.) en los DTOs.

## Persistencia

Spring Data JPA sobre MySQL (localhost:3306, mismas credenciales/convención que
`GestorDeInventario` de Clase 2). Se elige JPA en vez de DAO manual porque:
- La cursada ya arrancó Spring Boot en Clase 3, y `spring-boot-starter-data-jpa` es el camino
  natural desde ahí.
- Las preguntas guía del TPO comparan explícitamente DAO vs ORM — usar JPA da material real
  para responder "¿DAO y ORM son lo mismo?" (el `Repository` de Spring Data implementa el
  patrón DAO, pero generado por el framework en vez de escrito a mano).

**Confirmado en Clase 4 (2026-08-27, "Arquitectura Spring"):** la cátedra dijo explícitamente
que ya no se usa el patrón DAO ("No se usa más el modelo arquitectónico DAO. Responde a ORM.")
— la apuesta por JPA fue la correcta, sin necesidad de ningún cambio acá.

⚠️ **Conflicto pendiente de resolver:** la misma Clase 4 enseñó `application.properties` con
`spring.jpa.hibernate.ddl-auto=none` (esquema manual, evitando que Hibernate cree/modifique
tablas solo) — nuestro bootstrap (ya en `main`) usa `ddl-auto=update`, que nunca fue contenido
de clase. Decidir con Santiago si se migra a `none` + scripts SQL manuales antes de avanzar con
más módulos.

## Autenticación

Auth casera simple, sin Spring Security: tabla `Session` con token generado al hacer login,
password hasheada con SHA-256 + salt usando solo `java.security.MessageDigest`/`SecureRandom`
del JDK (sin BCrypt ni `spring-security-crypto` — se descartó esa opción al planificar para no
sumar ninguna dependencia de seguridad externa al esqueleto que generó la cátedra en Clase 3).
Se eligió auth casera en general porque Spring Security es sustancialmente más avanzado que lo
visto hasta Clase 3 — si la cátedra lo cubre más adelante en la cursada, migrar a Spring
Security queda como mejora post-TPO, no bloqueante para la entrega.

## Checkout / transacciones

El checkout es simulado: `POST /api/orders` crea la `Order` con status `PAID` directamente, sin
integrar un gateway de pago real. Esto cumple el requisito de "transaccional" (persiste una
operación de compra real, con sus `OrderItem`s y el vaciado del carrito) sin la complejidad de
integrar Mercado Pago u otro proveedor en un trabajo académico de alcance acotado.

## Frontend (Etapa 2)

HTML5/CSS3/JavaScript plano, sin frameworks, organizado por página según el mismo split de
módulos:

- `users`: `login.html`, `register.html`, `profile.html`.
- `marketplace`: `catalog.html`, `cart.html`, `orders.html`.
- `challenges`: `challenges.html`, `challenge-detail.html`, `ranking.html`.

Un `api.js` compartido envuelve `fetch()`, agrega el token de sesión (guardado en
`localStorage`) a cada request, y centraliza el manejo de errores HTTP (401 → redirige a
login, 400 → muestra el mensaje de validación).

Cada página maneja 3 estados: loading, error, success — manipulación directa del DOM
(`querySelector` + template strings), sin librerías de estado.

Validación en la interfaz: atributos HTML5 nativos (`required`, `min`, `pattern`) antes de
enviar el fetch, más el manejo del error si la validación del servidor de todos modos falla
(defensa en profundidad).

**Hosting:** el frontend estático se sirve desde `src/main/resources/static` del mismo
proyecto Spring Boot — un solo puerto (8080), sin configuración de CORS. El JS sigue hablando
con el backend vía `fetch()`/HTTP a los mismos endpoints, por lo que la arquitectura
cliente-servidor se mantiene aunque ambos vivan en el mismo proceso/deploy.

## Etapa 1 — checklist de requisitos mínimos

| # | Requisito | Cobertura en este diseño |
|---|---|---|
| 1 | Arquitectura por capas | Controller/Service/Repository/Entity por módulo |
| 2 | Persistencia SQL | MySQL vía Spring Data JPA |
| 3 | Modelado de entidades | Ver sección Modelo de dominio |
| 4 | CRUD | `Beat`, `Challenge`, `User` con create/read/update; el resto al menos create/read |
| 5 | Lógica de negocio | Checkout, cálculo de Music Score, validación de roles en Service |
| 6 | Endpoints expuestos | Ver sección Endpoints |
| 7 | Manejo HTTP request/response | `@RestController` + `ResponseEntity` con status codes |
| 8 | Validación de entrada | Bean Validation (`@Valid` + anotaciones en DTOs) |
| 9 | Manejo de errores | `@RestControllerAdvice` global |
| 10 | Config localhost | `application.properties`, datasource MySQL local, puerto 8080 |
| 11 | Doc básica de endpoints | Un `API.md` por módulo (Swagger/OpenAPI como stretch, no obligatorio) |
| 12 | Separación negocio/datos/comunicación | Ídem punto 1 |
| 13 | Tiempo real | Spring Boot embebido (Tomcat), sin batch/async |

**Setup técnico:** un proyecto Maven (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
`mysql-connector-j`, `spring-boot-starter-validation`), paquetes
`com.mgwprod.{users,marketplace,challenges}.{controller,service,repository,model,dto}`.

**Demo de Etapa 1:** cada integrante arma una colección de Postman (o archivo `.http`) para los
endpoints de su módulo — sirve como prueba de funcionamiento y como guión para el oral.

## Preguntas abiertas / a confirmar

- **Seguimiento continuo:** después de cada clase, chequear si el profesor cambió algo de la
  consigna (stack, requisitos obligatorios, fechas) y actualizar este spec antes de seguir
  implementando la parte afectada.
- Repartición concreta de módulos entre Santiago, Mateo y Paolo — pendiente, a definir por el
  grupo después de este diseño.
- Fechas límite de Etapa 1 y Etapa 2 — aún no confirmadas por la cátedra.
- Si Spring Security se cubre en clases posteriores, evaluar migrar la auth casera.
- Documentación de endpoints como Swagger/OpenAPI vs. Markdown manual — queda como stretch
  goal a decidir según tiempo disponible, no bloquea Etapa 1.
