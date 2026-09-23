# mgw-prod — Guía completa del código

Esta guía explica **todo** el proyecto: cada carpeta, cada archivo, cada clase, cada método,
cada import y cada anotación, más Postman y los tests. Está pensada para estudiar y para que
cualquiera del grupo pueda defender cualquier parte.

**Cómo leerla:**

- Si es la primera vez: leé en orden las secciones 1 a 7. Ahí está el "mapa mental".
- Si tenés que defender tu módulo: andá directo a tu paquete en la sección 9 y a la sección 13
  (Postman).
- Si no entendés un import o una anotación: buscalo en la sección 8 (diccionario).
- Antes de la defensa: leé la sección 15 (preguntas probables) y la 16 (limitaciones).

**Índice**

1. Qué es el proyecto
2. Cómo viaja un request (de Postman a la base y de vuelta)
3. Las carpetas y archivos de la raíz
4. `pom.xml`: dependencias y plugins
5. `application.properties`: la configuración
6. La base de datos (`schema.sql`), tabla por tabla
7. Conceptos que se repiten en todo el código
8. Diccionario de imports y anotaciones
9. El código, paquete por paquete y método por método
10. Relaciones JPA (resumen)
11. Cómo se conectan los módulos entre sí
12. Tests
13. Postman
14. Códigos HTTP: cuál se usa dónde
15. Preguntas probables del profesor
16. Limitaciones conocidas y bugs que encontramos
17. Diferencia entre la rama `main` y la rama `entrega/etapa1`
18. Glosario

---

## 1. Qué es el proyecto

**mgw-prod** es el backend de una plataforma de música para productores y artistas. Es la
**Etapa 1** del Trabajo Integrador de Aplicaciones Interactivas (UADE): un backend en Java con
Spring Boot que guarda los datos en MySQL y se prueba con Postman. No tiene pantalla todavía
(eso es la Etapa 2).

### Qué se puede hacer

- **Registrarse y loguearse** con tres roles posibles:
  - `ARTIST`: productor o artista. Publica beats, graba toplines y participa en desafíos.
  - `DISCOGRAFICA`: un sello. Puede crear desafíos.
  - `ADMIN`: administrador. Verifica artistas, crea y cierra desafíos, y puede editar o borrar
    cualquier cosa.
- **Publicar beats**: instrumentales con título, género, BPM, tonalidad y link al audio.
- **Grabar un topline** sobre un beat de otro (la voz o melodía). Esto crea automáticamente una
  **propuesta de colaboración** que el dueño del beat acepta o rechaza.
- **Comentar** beats y toplines.
- **Desafíos (challenges)**: un admin o una discográfica crea un concurso con género, BPM,
  deadline, premios y un **artista invitado** que hace de jurado. Los artistas mandan su
  **entrega (submission)**, la gente **vota** del 1 al 10, y al **cerrar** el desafío se calcula
  un puntaje ponderado y se arma el podio (top 3). El ganador queda **verificado**
  automáticamente.
- **Ranking general**: suma los puntos de todos los desafíos de cada productor.
- **Suscripción**: plan FREE (máximo 50 producciones entre beats y toplines) o PREMIUM (sin
  límite). El pago es **simulado**.

### Quién hizo qué

| Paquete | Integrante | Tema |
|---|---|---|
| `users` | Santiago | Registro, login, sesiones, roles, perfil de artista, verificación |
| `catalog` | Santiago y Mateo | Beats (CRUD) y comentarios de beats |
| `collab` | Dani | Toplines, comentarios de toplines, colaboraciones |
| `billing` | Dani | Suscripciones y pago simulado |
| `challenges` | Paolo | Desafíos, entregas, votos, cierre, resultados y ranking |

### Stack (tecnologías)

- **Java 21**: el lenguaje.
- **Spring Boot 4.1**: el framework que arma el servidor web y conecta todo.
- **Spring Data JPA + Hibernate**: para guardar y leer objetos Java en la base sin escribir SQL.
- **MySQL**: la base de datos.
- **Lombok**: genera getters, setters y constructores automáticamente.
- **Maven**: descarga las librerías y compila el proyecto.
- **JUnit 5 + Mockito + H2**: para los tests.
- **Postman**: para probar los endpoints a mano.

---

## 2. Cómo viaja un request (de Postman a la base y de vuelta)

Esto es lo más importante para entender el proyecto. Tomemos "crear un beat":

```
Postman
  │  POST http://localhost:8080/api/beats
  │  Header: Authorization: Bearer 3f2a...   Body: {"title":"Trap Beat", ...}
  ▼
Tomcat (servidor web que viene adentro de Spring Boot, escucha el puerto 8080)
  ▼
SessionAuthInterceptor.preHandle            (paquete users.security)
  │  lee el token, lo busca en la tabla sessions,
  │  y guarda userId = 1 como "atributo" del request
  ▼
BeatController.createBeat                   (capa CONTROLLER)
  │  Jackson convierte el JSON del body en un objeto Beat
  │  @RequestAttribute("userId") recibe el 1
  │  valida: ¿hay userId? ¿title vacío? ¿bpm válido? ¿es artista? ¿llegó al límite?
  ▼
BeatService.create                          (capa SERVICE: la lógica de negocio)
  │  suma 1 producción en la suscripción, pone producerId = 1
  ▼
BeatRepository.save                         (capa REPOSITORY: habla con la base)
  │  Hibernate genera: INSERT INTO beats (...) VALUES (...)
  ▼
MySQL guarda la fila y devuelve el id nuevo
  ▲
  │  el Beat guardado vuelve hacia arriba: Repository → Service → Controller
  │  el controller lo envuelve en ResponseEntity con estado 201 CREATED
  │  Jackson convierte el objeto Beat en JSON
  ▼
Postman recibe: 201 Created  {"id": 7, "producerId": 1, "title": "Trap Beat", ...}
```

**Las 4 capas y qué hace cada una:**

| Capa | Carpeta | Responsabilidad | Qué NO hace |
|---|---|---|---|
| Controller | `controller/` | Recibir el HTTP, validar lo que llega, decidir el código de respuesta | No habla con la base directamente |
| Service | `service/` | Reglas de negocio: permisos, límites, cálculos | No sabe nada de HTTP |
| Repository | `repository/` | Leer y escribir en la base | No tiene lógica |
| Model | `model/` | Las clases que representan las tablas (entidades) | No tiene lógica (salvo `@PrePersist`) |

**¿Por qué separar en capas?** Porque cada una cambia por motivos distintos. Si mañana cambiamos
MySQL por otra base, solo se toca la config. Si cambia una regla de negocio (el límite del plan
free), solo se toca el service. Y es el patrón que se vio en clase con el proyecto `webcampus`.

---

## 3. Las carpetas y archivos de la raíz

```
mgw-prod/
├── .gitignore
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── docs/
│   ├── Trabajo Integrador de Aplicaciones Interactivas.pdf
│   ├── db/schema.sql
│   ├── api/  (colecciones de Postman y documentación de endpoints)
│   └── GUIA-*.md (guías del equipo, solo en main)
└── src/
    ├── main/
    │   ├── java/com/mgwprod/   ← el código de la aplicación
    │   └── resources/application.properties
    └── test/
        ├── java/com/mgwprod/   ← los tests
        └── resources/application.properties
```

| Archivo o carpeta | Para qué está |
|---|---|
| `.gitignore` | Le dice a git qué archivos no subir: `target/` (lo compilado), `*.class`, carpetas de los editores (`.idea/`, `.vscode/`), `.DS_Store` de Mac. Lo compilado no se sube porque cada uno lo genera en su máquina. |
| `mvnw` / `mvnw.cmd` | **Maven Wrapper**. Scripts que descargan Maven solos la primera vez. Así nadie tiene que instalar Maven: `./mvnw` en Mac/Linux, `mvnw.cmd` en Windows. |
| `.mvn/wrapper/maven-wrapper.properties` | Dice qué versión de Maven descarga el wrapper (3.9.16). Así todo el grupo usa la misma. |
| `pom.xml` | El archivo de Maven: nombre del proyecto, versión de Java, librerías y plugins. Ver sección 4. |
| `README.md` | Cómo levantar y probar el proyecto. |
| `docs/Trabajo Integrador...pdf` | La consigna de la cátedra. |
| `docs/db/schema.sql` | El `CREATE TABLE` de todas las tablas. Se corre a mano una vez. Ver sección 6. |
| `docs/api/` | Colecciones y environment de Postman (y en main, documentación de endpoints por módulo). |
| `src/main/java` | El código Java de la app. |
| `src/main/resources` | Archivos que no son código: la configuración (`application.properties`). |
| `src/test/java` | Los tests automáticos. |
| `src/test/resources` | Configuración que usan solo los tests (base H2 en memoria). |
| `target/` | La crea Maven al compilar. No está en git. Se puede borrar sin problema. |

**¿Por qué `src/main/java/com/mgwprod`?** Es la convención de Maven: el código va en
`src/main/java` y adentro las carpetas siguen el nombre del paquete. Por convención el paquete
es el dominio al revés (`com.mgwprod`), para que no choque con paquetes de otras librerías.

**¿Por qué los paquetes están organizados por módulo (`users`, `catalog`...) y no por capa como
en `webcampus`?** En el ejemplo de la clase todo está junto en `controller/`, `service/`, etc.
Nosotros primero separamos por módulo y adentro por capa (`catalog/controller/`,
`catalog/service/`...). Lo hicimos porque somos 4 y la evaluación es individual: cada uno tiene
su carpeta y puede explicarla completa sin mezclarse con el código de los demás. Las capas son
exactamente las mismas que en clase.

---

## 4. `pom.xml`: dependencias y plugins

El `pom.xml` (Project Object Model) es la "receta" de Maven.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
</parent>
```

**parent**: heredamos la configuración de Spring Boot 4.1.0. Gracias a esto **no ponemos versión
en las dependencias**: el parent ya sabe qué versión de cada librería es compatible con las otras.

```xml
<groupId>com.mgwprod</groupId>
<artifactId>mgw-prod</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

**groupId / artifactId / version**: el "nombre completo" del proyecto. `SNAPSHOT` significa
"versión en desarrollo".

```xml
<properties><java.version>21</java.version></properties>
```

Compilamos con Java 21. Si tenés un JDK más viejo, falla con "release 21 not supported".

### Dependencias (librerías)

| Dependencia | Para qué la usamos |
|---|---|
| `spring-boot-starter-webmvc` | Todo lo web: el servidor Tomcat embebido, `@RestController`, `@GetMapping`, `ResponseEntity`, y Jackson para convertir JSON ↔ objetos. (En Spring Boot 4 se llama `webmvc`; en versiones viejas era `starter-web`.) |
| `spring-boot-starter-data-jpa` | JPA + Hibernate + Spring Data: `@Entity`, `JpaRepository`, `@Transactional`. Es lo que nos evita escribir SQL. |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Valid`). **Hoy no la usamos**: validamos con `if`. Quedó de una versión anterior; se podría sacar (ver sección 16). |
| `mysql-connector-j` (scope `runtime`) | El driver JDBC que sabe hablar con MySQL. `runtime` = no se usa al compilar, solo cuando corre la app. |
| `lombok` (`optional`) | Genera getters/setters/constructores en tiempo de compilación. `optional` = no se "contagia" a otros proyectos que usen el nuestro. |
| `spring-boot-starter-data-jpa-test` (scope `test`) | Herramientas para testear repositorios (`@DataJpaTest`). |
| `spring-boot-starter-webmvc-test` (scope `test`) | Herramientas para testear controllers (`@WebMvcTest`, `MockMvc`) + JUnit 5, Mockito y AssertJ. |
| `h2` (scope `test`) | Una base de datos que vive en memoria. Los tests la usan para no necesitar MySQL. |

**scope `test`** = esa librería solo existe al correr los tests; no va en la app final.

### Plugins

- `spring-boot-maven-plugin`: permite `./mvnw spring-boot:run` (levantar la app) y armar un
  `.jar` ejecutable.
- `maven-compiler-plugin` con `annotationProcessorPaths` → `lombok`: le dice al compilador que
  corra Lombok **mientras compila** (tanto el código normal como los tests). Sin esto,
  `@Getter` no generaría nada y `beat.getTitle()` no compilaría.

---

## 5. `application.properties`: la configuración

### `src/main/resources/application.properties`

```properties
spring.application.name=mgw-prod
server.port=8080
```
Nombre de la app y puerto donde escucha el servidor.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod
spring.datasource.username=root
spring.datasource.password=admin
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```
Cómo conectarse a la base: MySQL en nuestra máquina (`localhost`), puerto 3306, base
`mgw_prod`, usuario `root`, clave `admin`. El driver es el de `mysql-connector-j`.

```properties
spring.jpa.hibernate.ddl-auto=none
```
**Hibernate NO crea ni modifica tablas.** Las tablas las creamos nosotros con `schema.sql`. Es
lo que se enseñó en clase: el esquema de la base se controla a mano. (Otras opciones serían
`update` o `create`, que hacen que Hibernate cree las tablas a partir de las clases.)

```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```
No imprimir en consola cada SQL que genera Hibernate (si lo ponés en `true` sirve para
aprender qué SQL se ejecuta; `format_sql` lo muestra lindo).

```properties
spring.jpa.properties.jakarta.persistence.validation.mode=none
```
Como la librería de validación está en el `pom.xml`, Hibernate intentaría validar las entidades
cada vez que guarda. Nosotros validamos con `if` en los controllers, así que lo apagamos.

### `src/test/resources/application.properties`

Cuando corren los tests, Spring usa **este** archivo en vez del de `main`:

- `spring.datasource.url=jdbc:h2:mem:mgw_prod_test;MODE=MySQL;...`: una base **H2 en memoria**
  que imita a MySQL. Se crea al arrancar los tests y desaparece al terminar. Así los tests
  corren en cualquier máquina sin MySQL.
- `spring.jpa.hibernate.ddl-auto=create-drop`: acá **sí** dejamos que Hibernate cree las tablas
  a partir de las entidades (y las borre al terminar), porque la base es temporal.

---

## 6. La base de datos (`schema.sql`), tabla por tabla

Se carga una sola vez:

```bash
mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS mgw_prod;"
mysql -u root -padmin mgw_prod < docs/db/schema.sql
```

**Conceptos que aparecen en todas las tablas:**

- `id BIGINT AUTO_INCREMENT PRIMARY KEY`: clave primaria numérica que MySQL numera sola.
  En Java es `Long id` con `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
- `NOT NULL`: la columna es obligatoria.
- `UNIQUE`: no puede haber dos filas con el mismo valor.
- `FOREIGN KEY (x) REFERENCES tabla(id)`: **clave foránea**. La columna tiene que apuntar a una
  fila que exista en la otra tabla. MySQL rechaza lo que rompa esa regla.
- `ON DELETE CASCADE`: si se borra la fila "padre", MySQL borra también las "hijas".
  Sin `CASCADE`, MySQL **no deja** borrar al padre mientras tenga hijos.

| Tabla | Qué guarda | Claves foráneas y reglas |
|---|---|---|
| `users` | Todas las cuentas | `email` UNIQUE. `role` guardado como texto (`ARTIST`...). |
| `artist_profiles` | Datos extra de los artistas (géneros, bio, BPM, experiencia, `verified`) | `user_id` UNIQUE (un perfil por usuario) → `users` con CASCADE |
| `sessions` | Tokens de login | `token` UNIQUE, `user_id` → `users` CASCADE, `expires_at` |
| `beats` | Los beats | `producer_id` → `users` **sin CASCADE** (por eso no se puede borrar un usuario que tiene beats) |
| `beat_comments` | Comentarios de beats | `beat_id` → `beats` CASCADE, `author_id` → `users` |
| `toplines` | Toplines | `beat_id` → `beats` CASCADE, `artist_id` → `users` |
| `collaborations` | Propuestas de colaboración | `topline_id` UNIQUE (una por topline) → `toplines` CASCADE |
| `comments` | Comentarios de toplines | `topline_id` → `toplines` CASCADE |
| `challenges` | Desafíos | `created_by` y `guest_artist_id` → `users` |
| `submissions` | Entregas a un desafío | `challenge_id` → `challenges` CASCADE |
| `votes` | Votos | `UNIQUE (submission_id, voter_id)`: una persona no puede votar dos veces la misma entrega |
| `challenge_results` | Podio de cada desafío cerrado | `submission_id` UNIQUE. La columna se llama `rank_position` porque `rank` es palabra reservada en MySQL |
| `subscriptions` | Plan de cada usuario | `user_id` UNIQUE, `plan`, `productions_count` |

**Ejemplo de CASCADE en acción:** borrar un beat borra solo sus comentarios, sus toplines, y
(en cadena) las colaboraciones y comentarios de esos toplines. Por eso en la demo de Postman
el DELETE de un beat funciona aunque tenga cosas colgando.

**Nombres:** en SQL usamos `snake_case` (`producer_id`) y en Java `camelCase` (`producerId`).
La traducción la hace `@Column(name = "producer_id")`.

---

## 7. Conceptos que se repiten en todo el código

### 7.1 Inyección de dependencias por constructor

```java
@Service
public class BeatService {
    private final BeatRepository beatRepository;

    public BeatService(BeatRepository beatRepository) {
        this.beatRepository = beatRepository;
    }
}
```

Nunca hacemos `new BeatRepository()`. Spring, al arrancar, crea **una sola instancia** de cada
clase marcada con `@Service`, `@RestController`, `@Component` o de cada `Repository`
(se llaman **beans**), y cuando ve que un constructor pide un `BeatRepository`, se lo pasa.
Eso es la **inyección de dependencias**.

- **¿Por qué por constructor?** Porque los campos pueden ser `final` (no se pueden cambiar
  después) y en los tests se puede pasar un objeto falso (mock) fácilmente.
- `final` = una vez asignado en el constructor, no cambia más.

### 7.2 Cómo decidimos cada código HTTP (sin excepciones)

Todos los controllers siguen el mismo patrón, con `if` y `ResponseEntity`:

```java
if (userId == null) {                         // no está logueado
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);   // 401
}
if (beat.getTitle() == null || beat.getTitle().isBlank()) {   // dato inválido
    return ResponseEntity.badRequest().body(null);                     // 400
}
Beat existing = beatService.getById(id);
if (existing == null) {                       // no existe
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);      // 404
}
if (!beatService.canModify(existing, userId)) {   // no tiene permiso
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);      // 403
}
return ResponseEntity.ok(beatService.update(id, beat));                 // 200
```

Las reglas de la casa:

- **400** → `if` en el controller revisando los campos.
- **404** → el service devuelve `null` cuando algo no existe (`findById(id).orElse(null)`), y el
  controller pregunta `== null`.
- **403 / 409** → el service tiene un método que devuelve `boolean` (`canModify`, `isArtist`,
  `isOwner`, `alreadyVoted`, `emailExists`...) y el controller lo consulta **antes** de cambiar
  nada.
- **¿Por qué no usamos excepciones propias ni `@Valid`?** Porque no se vieron en clase hasta
  ahora, y así todo el proyecto se lee con un solo patrón simple que sabemos explicar.
- Única excepción que sí atrapamos: `DataIntegrityViolationException` al borrar un usuario,
  porque la tira Spring (no nosotros) cuando MySQL rechaza el borrado.

### 7.3 `ResponseEntity`

Es la "respuesta HTTP completa": código de estado + body (+ headers). Formas que usamos:

| Código | Cómo se escribe |
|---|---|
| 200 OK | `ResponseEntity.ok(objeto)` |
| 201 Created | `ResponseEntity.status(HttpStatus.CREATED).body(objeto)` |
| 204 No Content | `ResponseEntity.noContent().build()` (en los DELETE: salió bien y no hay nada que devolver) |
| 400 | `ResponseEntity.badRequest().body(null)` |
| 401/403/404/409 | `ResponseEntity.status(HttpStatus.XXX).body(null)` o `.build()` |

`ResponseEntity<Beat>`: el `<Beat>` dice qué tipo de objeto va en el body.
`ResponseEntity<Void>`: no hay body (se usa en los DELETE).
Si un método de controller devuelve directamente un `List<Beat>` (sin `ResponseEntity`), Spring
responde siempre 200 con esa lista.

### 7.4 El usuario logueado: `@RequestAttribute(name = "userId", required = false) Long userId`

El `SessionAuthInterceptor` (sección 9.2) guarda el id del usuario logueado en el request. El
controller lo recibe así. `required = false` significa: si no hay usuario, llega `null` en vez
de dar error. Por eso cada endpoint privado empieza con `if (userId == null) → 401`.

**Regla importante:** los campos que dicen "quién hizo algo" (`producerId`, `authorId`,
`artistId`, `createdBy`, `voterId`) **nunca se toman del JSON**. Siempre salen del `userId` de
la sesión. Si no fuera así, cualquiera podría mandar `"producerId": 5` y publicar a nombre de
otro.

### 7.5 Entidades JPA sin DTOs

Los controllers reciben y devuelven directamente las clases del `model/` (`Beat`, `User`...).
Jackson las convierte a JSON y desde JSON. Es el patrón de la clase 4.

Para controlar qué se muestra usamos anotaciones de Jackson:

- `@JsonIgnore`: el campo **nunca** aparece en el JSON (ej. `passwordHash`).
- `@JsonProperty(access = WRITE_ONLY)`: se puede **mandar** pero nunca se **devuelve**
  (ej. `password` al registrarse).
- `@JsonProperty(access = READ_ONLY)`: se **devuelve** pero si el cliente lo manda se ignora
  (ej. `artistId` en Topline).

### 7.6 `Optional` y `.orElse(null)`

`repository.findById(id)` no devuelve un `Beat`: devuelve un `Optional<Beat>`, una "cajita" que
puede tener un beat o estar vacía. `.orElse(null)` significa "dame lo que hay adentro o
`null` si está vacía". Lo usamos para seguir el patrón de "null = no existe = 404".

En dos lugares usamos `.orElseThrow()` (tira error si está vacía): cuando es **imposible** que
esté vacía porque el controller ya lo verificó antes (ver `UserService.updateArtistProfile`).

### 7.7 `@Transactional`

Marca un método del service como **transacción**: todo lo que hace contra la base se confirma
junto al final, y si algo falla en el medio, se deshace todo. Ejemplo: `ToplineService.create`
guarda el topline **y** la colaboración; si la segunda falla, no queda un topline sin
colaboración.

`@Transactional(readOnly = true)` = transacción solo de lectura. Es más liviana, **pero MySQL
no deja hacer INSERT/UPDATE dentro**. Esto causó un bug real (sección 16).

### 7.8 Update parcial

En todos los `update` solo se cambian los campos que vinieron en el JSON:

```java
if (request.getTitle() != null) beat.setTitle(request.getTitle());
if (request.getBpm() != null) beat.setBpm(request.getBpm());
```

Si mandás solo `{"bpm": 145}`, el título no se borra. Por eso los números usan `Integer` (que
puede ser `null`) y no `int` (que vale 0 si no viene).

### 7.9 `@PrePersist`

```java
@PrePersist
protected void onCreate() { this.createdAt = Instant.now(); }
```

Un método que JPA llama solo justo antes del `INSERT`. Así la fecha de creación la pone el
servidor y el cliente no la tiene que mandar.

### 7.10 Métodos de repositorio que "se escriben solos"

```java
List<Beat> findByGenreAndBpm(String genre, Integer bpm);
```

No hay implementación. Spring Data lee el **nombre** del método y arma el SQL:
`SELECT * FROM beats WHERE genre = ? AND bpm = ?`. Las palabras clave son `findBy`, `existsBy`,
`And`, `In`, `True`... Y si el campo es una relación (`beat` es un objeto `Beat`),
`findByBeatId` entiende "el id del beat relacionado" → `WHERE beat_id = ?`.

`JpaRepository<Beat, Long>` ya trae gratis: `save`, `findById`, `findAll`, `deleteById`,
`existsById`, `count`, `flush`... El `<Beat, Long>` significa "entidad Beat, id de tipo Long".

---

## 8. Diccionario de imports y anotaciones

Un `import` le dice a Java de qué paquete viene una clase que usamos. Acá están **todos** los
que aparecen en el código, agrupados.

### 8.1 Spring Web (controllers) — `org.springframework.web.bind.annotation.*`

| Import | Qué es |
|---|---|
| `RestController` | Marca la clase como controller REST: cada método responde JSON. |
| `RequestMapping` | Prefijo de URL para toda la clase (ej. `/api/beats`). |
| `GetMapping` / `PostMapping` / `PutMapping` / `DeleteMapping` | Qué método HTTP y qué ruta atiende cada método Java. |
| `PathVariable` | Toma un valor de la URL: `/api/beats/{id}` → `@PathVariable Long id`. |
| `RequestParam` | Toma un parámetro de query: `/api/beats?genre=Trap` → `@RequestParam String genre`. `required = false` lo hace opcional. |
| `RequestBody` | Convierte el JSON del body en un objeto Java. |
| `RequestAttribute` | Lee un atributo que alguien (el interceptor) guardó en el request. |

### 8.2 Spring HTTP — `org.springframework.http.*`

| Import | Qué es |
|---|---|
| `ResponseEntity` | La respuesta completa (código + body). |
| `HttpStatus` | Enum con los códigos: `CREATED`, `NOT_FOUND`, `FORBIDDEN`, etc. |

### 8.3 Spring core

| Import | Qué es |
|---|---|
| `org.springframework.stereotype.Service` | `@Service`: esta clase es un bean de lógica de negocio. |
| `org.springframework.stereotype.Component` | `@Component`: bean genérico (lo usamos en `PasswordHasher` y `SimulatedPaymentGateway`). |
| `org.springframework.context.annotation.Configuration` | `@Configuration`: clase de configuración (`WebConfig`). |
| `org.springframework.transaction.annotation.Transactional` | Ver 7.7. |
| `org.springframework.dao.DataIntegrityViolationException` | Error que tira Spring cuando la base rechaza algo por una regla (FK, UNIQUE). |
| `org.springframework.boot.SpringApplication` / `autoconfigure.SpringBootApplication` | Arrancan la app (sección 9.1). |
| `org.springframework.web.servlet.HandlerInterceptor` | Interfaz para "interceptar" requests antes del controller. |
| `org.springframework.web.servlet.config.annotation.WebMvcConfigurer` / `InterceptorRegistry` | Para registrar el interceptor. |
| `org.springframework.data.jpa.repository.JpaRepository` | La interfaz base de los repositorios. |

### 8.4 JPA — `jakarta.persistence.*`

| Import | Qué es |
|---|---|
| `Entity` | Esta clase es una tabla. |
| `Table(name = "beats")` | Nombre exacto de la tabla. |
| `Id` | Este campo es la clave primaria. |
| `GeneratedValue(strategy = GenerationType.IDENTITY)` | El id lo genera MySQL con `AUTO_INCREMENT`. |
| `Column(name = ..., nullable = ..., unique = ..., length = ..., updatable = ..., columnDefinition = ...)` | Configura la columna. `updatable = false` en `created_at` para que nunca se modifique. `columnDefinition = "TEXT"` para textos largos (bio). |
| `Enumerated(EnumType.STRING)` | Guarda un enum como texto (`"ARTIST"`) en vez de número (0). Si mañana se reordena el enum, no se rompen los datos. |
| `Transient` | Este campo **no** es una columna (no se guarda). |
| `PrePersist` | Ver 7.9. |
| `OneToOne` / `ManyToOne` / `OneToMany` | Relaciones entre entidades (sección 10). |
| `JoinColumn(name = "beat_id")` | Qué columna es la clave foránea de la relación. |

**¿Por qué `jakarta` y no `javax`?** Es el mismo JPA; en 2019 cambió de nombre de paquete.
Spring Boot 3 y 4 usan `jakarta`.

### 8.5 Jackson (JSON)

| Import | Qué es |
|---|---|
| `com.fasterxml.jackson.annotation.JsonIgnore` | Ver 7.5. |
| `com.fasterxml.jackson.annotation.JsonProperty` | Ver 7.5 (`WRITE_ONLY` / `READ_ONLY`). |
| `tools.jackson.databind.ObjectMapper` (solo tests) | El objeto que convierte JSON ↔ Java. |

**¿Por qué dos paquetes distintos (`com.fasterxml` y `tools.jackson`)?** Spring Boot 4 usa
Jackson 3. En Jackson 3 el motor se mudó a `tools.jackson`, pero las anotaciones siguieron en
`com.fasterxml.jackson.annotation` para ser compatibles con código viejo.

### 8.6 Lombok — `lombok.*`

| Import | Qué genera |
|---|---|
| `Getter` | Un `getX()` por cada campo. |
| `Setter` | Un `setX(...)` por cada campo. |
| `NoArgsConstructor` | Un constructor vacío `new Beat()`. JPA y Jackson lo necesitan. |
| `AllArgsConstructor` | Un constructor con todos los campos (solo en `Session`). |

### 8.7 Java estándar

| Import | Qué es |
|---|---|
| `java.util.List` / `ArrayList` | Lista ordenada. `List` es la interfaz, `ArrayList` la implementación. |
| `java.util.Map` / `HashMap` | Diccionario clave → valor (ej. id de entrega → puntaje). |
| `java.util.Set` / `HashSet` | Conjunto sin repetidos; `contains` es muy rápido. |
| `java.util.Optional` | Ver 7.6. |
| `java.util.UUID` | Genera identificadores aleatorios únicos (los tokens). |
| `java.util.Base64` | Convierte bytes en texto (para guardar el hash). |
| `java.time.Instant` | Un momento exacto en el tiempo (fechas de creación, deadline, vencimiento). |
| `java.time.temporal.ChronoUnit` | Unidades de tiempo (`HOURS`) para sumar 24 horas. |
| `java.math.BigDecimal` | Número decimal exacto, para plata (`15.00`). `double` tiene errores de redondeo. |
| `java.security.MessageDigest` | Calcula hashes (SHA-256). |
| `java.security.SecureRandom` | Números aleatorios seguros (para el salt). |
| `java.security.NoSuchAlgorithmException` | Error si el algoritmo no existe. |
| `jakarta.servlet.http.HttpServletRequest` / `HttpServletResponse` | El request y la response "crudos" que maneja el interceptor. |

---

## 9. El código, paquete por paquete y método por método

### 9.1 `com.mgwprod` — `MgwProdApplication.java`

```java
@SpringBootApplication
public class MgwProdApplication {
    public static void main(String[] args) {
        SpringApplication.run(MgwProdApplication.class, args);
    }
}
```

- Es el **punto de entrada**: lo que corre `./mvnw spring-boot:run`.
- `@SpringBootApplication` hace tres cosas: activa la configuración automática de Spring Boot
  (ve MySQL en el classpath y configura la conexión, ve Spring Web y levanta Tomcat...), marca la
  clase como configuración, y **escanea** el paquete `com.mgwprod` y todos sus subpaquetes
  buscando clases con `@Service`, `@RestController`, `@Component`, repositorios, etc.
- **Por eso esta clase tiene que estar en la raíz** (`com.mgwprod`): si estuviera adentro de
  `users`, Spring no encontraría las clases de `catalog`.
- `SpringApplication.run(...)` crea todos los beans, conecta la base y levanta el servidor en el
  puerto 8080.

---

### 9.2 Paquete `users` (Santiago)

Se encarga de las cuentas, el login y la seguridad. Todos los demás módulos dependen de él.

```
users/
├── config/WebConfig.java
├── controller/AuthController.java, UserController.java, ArtistVerificationController.java
├── model/User.java, Role.java, Session.java, ArtistProfile.java
├── repository/UserRepository.java, SessionRepository.java, ArtistProfileRepository.java
├── security/PasswordHasher.java, SessionAuthInterceptor.java
└── service/AuthService.java, UserService.java
```

Tiene dos carpetas extra (`config` y `security`) porque el login necesita piezas que no son
ni controller ni service: el hasheo de contraseñas y el interceptor.

#### `model/Role.java` (enum)

```java
public enum Role { ARTIST, DISCOGRAFICA, ADMIN }
```

Un `enum` es un tipo con un conjunto **cerrado** de valores. Usamos enum en vez de un `String`
para que sea imposible tener un rol mal escrito como `"artista"`. En el JSON se manda como
texto: `"role": "ARTIST"`.

#### `model/User.java` (entidad → tabla `users`)

| Campo | Tipo | Detalle |
|---|---|---|
| `id` | `Long` | Clave primaria autogenerada. |
| `email` | `String` | `unique = true`. |
| `passwordHash` | `String` | `@JsonIgnore`: nunca sale en una respuesta. Es lo que se guarda (salt + hash). |
| `password` | `String` | `@Transient` (no es columna) + `@JsonProperty(WRITE_ONLY)`: solo sirve para **recibir** la contraseña en el registro. Nunca se guarda ni se devuelve. |
| `displayName` | `String` | Nombre visible. |
| `role` | `Role` | `@Enumerated(EnumType.STRING)`. |
| `city` | `String` | Opcional. |
| `createdAt` | `Instant` | La pone `onCreate()` (`@PrePersist`). |

- Anotaciones de clase: `@Entity`, `@Table(name = "users")`, `@Getter`, `@Setter`,
  `@NoArgsConstructor`.
- **¿Por qué no tiene `@AllArgsConstructor`?** Con Jackson 3, si hay un constructor con todos
  los campos, Jackson intenta usarlo para armar el objeto desde el JSON y falla cuando falta
  algún campo. Con solo el constructor vacío, Jackson crea `new User()` y llama a los setters de
  los campos que vinieron. Hay un test (`UserJsonTest`) que controla esto.

#### `model/Session.java` (→ tabla `sessions`)

- `user`: `@ManyToOne` + `@JoinColumn(name = "user_id")`. **Muchas** sesiones pueden ser de
  **un** usuario (ej. logueado en la compu y en el celu).
- `token`: texto aleatorio (UUID), `unique`. Es lo que el cliente manda en cada request.
- `expiresAt`: vence a las 24 horas.
- Tiene `@AllArgsConstructor` además de `@NoArgsConstructor` (se usa en tests).

#### `model/ArtistProfile.java` (→ tabla `artist_profiles`)

- Datos que solo tienen sentido para artistas: `genres`, `bio` (`TEXT`), `bpmMin`, `bpmMax`,
  `experienceLevel`, `verified` (arranca en `false`).
- **¿Por qué una tabla aparte y no columnas en `users`?** Porque una discográfica o un admin
  tendrían todas esas columnas vacías.
- `user`: `@OneToOne` + `@JoinColumn(name = "user_id", unique = true)`. Un perfil ↔ un usuario.
  Tiene `@JsonIgnore` para que al devolver el perfil no se incluya el `User` entero.
- `verified` pasa a `true` de dos formas: un admin lo verifica, o el artista gana un desafío.

#### `repository/UserRepository.java`

- `extends JpaRepository<User, Long>` → hereda `save`, `findById`, `findAll`, `deleteById`...
- `Optional<User> findByEmail(String email)` → `SELECT ... WHERE email = ?`. Se usa en el login.
- `boolean existsByEmail(String email)` → devuelve true/false. Se usa para el 409 de email
  repetido. Es más liviano que traer el usuario entero.

#### `repository/SessionRepository.java`

- `Optional<Session> findByToken(String token)` → la consulta que corre en **cada** request
  autenticado.

#### `repository/ArtistProfileRepository.java`

- `Optional<ArtistProfile> findByUserId(Long userId)` → el campo se llama `user` (objeto), pero
  Spring entiende `UserId` como "el id del user relacionado": `WHERE user_id = ?`.
- `List<ArtistProfile> findByVerifiedTrue()` → `WHERE verified = true`. Lo usa el cierre de
  desafíos para saber qué votantes están verificados.

#### `security/PasswordHasher.java` (`@Component`)

Nunca guardamos la contraseña en texto plano. Guardamos un **hash**: una "huella" que se calcula
a partir de la contraseña y que **no se puede revertir**.

- `SALT_LENGTH_BYTES = 16`: tamaño del salt.
- `hash(String rawPassword)`: genera un **salt** aleatorio, calcula `SHA-256(salt + password)` y
  devuelve `"saltEnBase64:hashEnBase64"`. Se llama una vez, al registrarse.
- `matches(String rawPassword, String storedHash)`: separa el texto guardado por `":"`,
  recupera el salt, vuelve a calcular el hash de la contraseña que llegó con **ese mismo salt**
  y compara. Si coinciden, la contraseña es correcta. Nunca se "desencripta" nada.
  - Compara con `MessageDigest.isEqual` y no con `equals`, porque `isEqual` tarda siempre lo
    mismo. Así nadie puede adivinar el hash midiendo cuánto tarda la comparación.
- `generateSalt()` (privado): 16 bytes con `SecureRandom`.
- `digest(...)` (privado): el cálculo SHA-256. Tiene un `try/catch` obligatorio porque
  `getInstance("SHA-256")` declara que puede tirar `NoSuchAlgorithmException` (en la práctica
  nunca pasa: toda JVM trae SHA-256).
- **¿Para qué sirve el salt?** Dos usuarios con la misma contraseña terminan con hashes
  distintos, y no sirven las tablas de hashes precalculados ("rainbow tables").

#### `security/SessionAuthInterceptor.java`

Un **interceptor** es código que se ejecuta **antes** de llegar al controller, en todos los
requests. Es nuestra "portería".

- `implements HandlerInterceptor` y sobreescribe `preHandle(request, response, handler)`.
  Si devuelve `true`, el request sigue al controller. Si devuelve `false`, se corta ahí.
- Pasos:
  1. Lee el header `Authorization`.
  2. Si no hay header o no empieza con `"Bearer "` → devuelve `true` (deja pasar **sin**
     usuario). El controller después decide si ese endpoint necesitaba login.
  3. Si hay token: lo busca con `sessionRepository.findByToken`. Si no existe o venció
     (`expiresAt.isBefore(Instant.now())`) → pone 401 y devuelve `false`.
  4. Si es válido: `request.setAttribute("userId", ...)` y `("userRole", ...)`, y devuelve
     `true`. De ahí lo leen los controllers con `@RequestAttribute`.
- `USER_ID_ATTRIBUTE` y `USER_ROLE_ATTRIBUTE` son constantes (`public static final`) para no
  escribir el texto `"userId"` a mano en varios lados.
- **Diferencia clave:** no mandar token = pasás como anónimo. Mandar un token **inválido** = 401
  directo.
- No lleva `@Component`: lo crea a mano `WebConfig` con `new`.

#### `config/WebConfig.java` (`@Configuration`, `implements WebMvcConfigurer`)

- `addInterceptors(InterceptorRegistry registry)`: Spring lo llama al arrancar. Registra el
  interceptor para `/api/**` **excepto** `/api/auth/**` (registro y login, porque para
  loguearte todavía no tenés token).
- Recibe `SessionRepository` por constructor y se lo pasa al interceptor.
- Sin esta clase, el interceptor existiría pero nunca se ejecutaría.

#### `service/AuthService.java`

- `SESSION_DURATION_HOURS = 24`.
- Dependencias: `UserRepository`, `ArtistProfileRepository`, `SessionRepository`,
  `PasswordHasher`.
- `emailExists(String email)` (`readOnly`): true si el email ya está registrado → el controller
  responde 409.
- `register(User incoming)`:
  1. Crea un `User` nuevo y copia email, nombre, rol y ciudad (no se guarda el objeto que vino
     del JSON tal cual, se copian solo los campos permitidos).
  2. `passwordHash = passwordHasher.hash(password)`.
  3. `userRepository.save(user)`.
  4. Si el rol es `ARTIST`, crea su `ArtistProfile` vacío.
  5. Devuelve el usuario (sin hash en el JSON por el `@JsonIgnore`).
- `login(String email, String password)`:
  1. Si falta email o password → `null`.
  2. Busca el usuario por email; si no existe o la contraseña no coincide → `null`.
  3. Crea una `Session` con token `UUID.randomUUID()` y vencimiento `ahora + 24 horas`, la
     guarda y la devuelve.
  - **Todos los errores devuelven el mismo `null` (→ 401) a propósito**: si dijéramos "ese email
    no existe" vs "contraseña incorrecta", alguien podría averiguar qué emails están
    registrados.
  - Cada login crea una sesión nueva (no reutiliza), así podés estar logueado en dos lugares.

#### `service/UserService.java`

- `getById(Long)`: `findById(...).orElse(null)`.
- `isArtist(User)` / `isAdmin(User)`: comparan el rol.
- `getProfile(Long userId)`: busca el perfil de artista.
- `isOwner(Long target, Long requesting)`: true si el que pide es el mismo usuario de la URL.
- `updateUser(Long, User request)`: update parcial de `displayName` y `city`.
- `updateArtistProfile(Long, ArtistProfile request)`: update parcial de géneros, bio, BPM y
  experiencia. Usa `orElseThrow()` porque el controller ya confirmó que es artista (y todo
  artista tiene perfil desde el registro).
- `verifyArtist(Long)`: pone `verified = true`.
- `delete(Long)`:
  ```java
  try {
      userRepository.deleteById(id);
      userRepository.flush();
      return true;
  } catch (DataIntegrityViolationException ex) {
      return false;
  }
  ```
  Si el usuario tiene beats (FK sin CASCADE), MySQL rechaza el borrado. **`flush()`** obliga a
  Hibernate a mandar el `DELETE` a la base **en ese momento**. Sin el `flush()`, el `DELETE` se
  mandaría recién al terminar la transacción, fuera del `try`, y no lo podríamos atrapar. El
  controller convierte el `false` en **409 Conflict**.

#### `controller/AuthController.java` — `/api/auth`

| Endpoint | Método Java | Qué hace |
|---|---|---|
| `POST /api/auth/register` | `register(@RequestBody User user)` | 400 si falta email, si la contraseña tiene menos de 8 caracteres, si falta nombre o rol. 409 si el email existe. 201 con el usuario creado. |
| `POST /api/auth/login` | `login(@RequestBody User credentials)` | Recibe `{email, password}` (reusa la clase `User` como "molde"). 401 si no coincide, 200 con la sesión (incluye el `token`). |

Son los únicos endpoints que no pasan por el interceptor.

#### `controller/UserController.java` — `/api/users`

| Endpoint | Método | Login | Reglas |
|---|---|---|---|
| `GET /api/users/{id}` | `getUser` | No | 404 si no existe |
| `GET /api/users/{id}/profile` | `getProfile` | No | 404 si no existe, **403** si existe pero no es artista |
| `PUT /api/users/{id}` | `updateUser` | Sí | 401 → 403 si no es el dueño → 404 |
| `PUT /api/users/{id}/artist-profile` | `updateArtistProfile` | Sí | 401 → 403 → 404 → 403 si no es artista |
| `DELETE /api/users/{id}` | `deleteUser` | Sí | 401 → 404 → si no es el dueño tiene que ser admin (si no, 403) → 409 si tiene contenido → 204 |

**Detalle del orden en `updateUser`:** se chequea "¿sos el dueño?" (403) **antes** que
"¿existe?" (404). Así alguien que prueba ids al azar recibe siempre 403 y no puede averiguar
qué ids existen.

#### `controller/ArtistVerificationController.java`

- `PUT /api/artists/{id}/verify` → `verify(...)`. Pasos numerados en el código: 401 si no hay
  login, 404 si el que pide no existe, 403 si no es admin, 404 si el artista no existe, 403 si el
  destino no es artista, 200 con el perfil verificado.
- **¿Por qué un controller aparte?** Porque verificar es una acción de administración, distinta
  a que un usuario maneje su cuenta. Por eso además tiene otra URL (`/api/artists`).

---

### 9.3 Paquete `catalog` (Santiago y Mateo)

Los beats y sus comentarios. Es el CRUD más "de manual" del proyecto.

#### `model/Beat.java` (→ `beats`)

| Campo | Detalle |
|---|---|
| `id` | Autogenerado |
| `producerId` | `Long`. Lo pone el service con el `userId` de la sesión. |
| `title`, `genre` | Obligatorios |
| `bpm` | `Integer` obligatorio (`Integer` y no `int` para poder saber si vino o no en un update) |
| `key` | Tonalidad. En la base se llama `music_key` porque `key` es palabra reservada en MySQL |
| `audioUrl` | Link al audio (no subimos archivos, guardamos un link) |
| `createdAt` | `@PrePersist` |
| `comments` | `@OneToMany(mappedBy = "beat")` + `@JsonIgnore` |

**`comments` explicado:** es el "otro lado" de la relación que está en `BeatComment`.
`mappedBy = "beat"` significa "la clave foránea no está en mi tabla, está en el campo `beat` de
`BeatComment`". Esta lista **se lee** de la base, nunca se escribe desde acá. El `@JsonIgnore`
evita un bucle infinito al convertir a JSON: Beat → comments → BeatComment → beat → comments...

#### `model/BeatComment.java` (→ `beat_comments`)

- `beat`: `@ManyToOne` + `@JoinColumn(name = "beat_id")`. **Muchos** comentarios pertenecen a
  **un** beat. Es un objeto `Beat` completo, no solo un número: Hibernate puede traer el beat
  entero cuando se necesita. Al devolver un comentario, el JSON incluye el beat.
- `authorId`: lo pone el service desde la sesión.
- `text`: hasta 1000 caracteres.
- `createdAt`: `@PrePersist`.

#### `repository/BeatRepository.java`

| Método | SQL que genera |
|---|---|
| `findByProducerId(Long)` | `WHERE producer_id = ?` |
| `findByGenre(String)` | `WHERE genre = ?` |
| `findByBpm(Integer)` | `WHERE bpm = ?` |
| `findByGenreAndBpm(String, Integer)` | `WHERE genre = ? AND bpm = ?` |

#### `repository/BeatCommentRepository.java`

- `findByBeatId(Long)` → `WHERE beat_id = ?` (navega la relación `beat`).

#### `service/BeatService.java`

Dependencias: `BeatRepository`, `UserRepository` (para chequear roles) y `SubscriptionService`
(del paquete `billing`, para el límite del plan).

| Método | Qué hace |
|---|---|
| `isArtist(Long userId)` | Busca el usuario y devuelve true si es `ARTIST`. Solo los artistas publican beats. |
| `isAtProductionLimit(Long)` | Pregunta a `SubscriptionService` si llegó al límite del plan FREE. **No es `readOnly`** porque puede terminar creando la suscripción (ver bug en sección 16). |
| `create(Long producerId, Beat)` | Suma una producción a la suscripción, pone el `producerId` y guarda. |
| `list(genre, bpm, producerId)` | Filtros en cascada: si viene `producerId`, se usa solo ese. Si vienen `genre` y `bpm`, los combina. Si viene uno solo, filtra por ese. Si no viene nada, `findAll()`. |
| `getById(Long)` | `orElse(null)`. |
| `update(Long, Beat)` | Update parcial de título, género, BPM, tonalidad y audio. |
| `delete(Long)` | `deleteById`. Por el CASCADE se borran sus comentarios y toplines. |
| `canModify(Beat, Long)` | true si el que pide es el dueño del beat **o** un admin. |

#### `service/BeatCommentService.java`

- `create(beatId, authorId, comment)`: busca el beat (si no existe → `null` → 404), le asigna el
  beat y el autor, guarda.
- `listByBeat(beatId)`: `null` si el beat no existe (para poder dar 404 en vez de una lista
  vacía, que sería engañosa).

#### `controller/BeatController.java` — `/api/beats`

| Endpoint | Método | Login | Reglas |
|---|---|---|---|
| `POST /api/beats` | `createBeat` | Sí (ARTIST) | 401 → 400 (title, genre, bpm ≥ 1, audioUrl) → 403 si no es artista → 403 si llegó al límite → 201 |
| `GET /api/beats` | `listBeats` | No | Query params opcionales `genre`, `bpm`, `producerId`. Siempre 200. |
| `GET /api/beats/{id}` | `getBeat` | No | 404 / 200 |
| `PUT /api/beats/{id}` | `updateBeat` | Sí | 401 → 404 → 403 (no es dueño ni admin) → 200 |
| `DELETE /api/beats/{id}` | `deleteBeat` | Sí | 401 → 404 → 403 → 204 |

El orden en `createBeat` es 401 → 400 → 403: primero ver que esté logueado, después que el beat
esté bien armado, y recién ahí consultar la base por el rol y el límite.

#### `controller/BeatCommentController.java` — `/api/beats/{beatId}/comments`

La URL está **anidada** porque un comentario siempre pertenece a un beat.

- `POST` → `createComment`: 401 → 400 si `text` vacío → 404 si el beat no existe → 201.
  Cualquier usuario logueado puede comentar (no hace falta ser artista).
- `GET` → `listComments`: público. 404 si el beat no existe.

---

### 9.4 Paquete `collab` (Dani)

Toplines sobre beats, sus comentarios, y la colaboración que nace de cada topline.

**El flujo del negocio:** el artista B escucha un beat del productor A y graba una voz encima
(un **topline**). Al subirlo, el sistema crea sola una **colaboración** en estado `PENDING`. El
productor A la acepta (`ACCEPTED`) o la rechaza (`REJECTED`).

#### `model/Topline.java` (→ `toplines`)

- `artistId`: `@JsonProperty(READ_ONLY)` (si el cliente lo manda, se ignora) — lo pone el
  service desde la sesión.
- `beat`: `@ManyToOne` + `@JoinColumn(name = "beat_id")`. **Muchos** toplines sobre **un** beat.
  Es la relación JPA del módulo de Dani.
  - Por eso en el JSON de creación se manda `"beat": {"id": 5}`: Jackson arma un objeto `Beat`
    que solo tiene el id, y el service después busca el beat real en la base.
- `audioUrl`, `createdAt` (`@PrePersist`).

#### `model/CollaborationStatus.java` (enum)

`PENDING`, `ACCEPTED`, `REJECTED`. Se guarda como texto (`@Enumerated(STRING)`).

#### `model/Collaboration.java` (→ `collaborations`)

- `toplineId`: `Long` con `unique = true` → una colaboración por topline. Acá **no** usamos una
  relación JPA, solo el id (decidimos una relación "real" por módulo, y la de este módulo es
  `Topline.beat`).
- `status`: el estado.
- `decidedAt`: `null` mientras está `PENDING`; se completa cuando el productor decide.

#### `model/Comment.java` (→ `comments`)

Comentario sobre un topline (distinto de `BeatComment`, que es sobre un beat). `toplineId` y
`authorId` son `READ_ONLY`: los pone el service.

#### Repositorios

| Repositorio | Métodos propios |
|---|---|
| `ToplineRepository` | `findByBeatId(Long)` (navega la relación `beat`), `findByArtistId(Long)` |
| `CollaborationRepository` | `findByToplineId(Long)` (devuelve `Optional` porque hay como máximo una), `findByStatus(CollaborationStatus)` |
| `CommentRepository` | `findByToplineId(Long)` |

#### `service/ToplineService.java`

Dependencias: `ToplineRepository`, `CollaborationRepository`, `UserRepository`,
`BeatRepository` (del paquete `catalog`) y `SubscriptionService` (de `billing`).

| Método | Qué hace |
|---|---|
| `isArtist(Long)` | Solo los artistas suben toplines. |
| `isAtProductionLimit(Long)` | Igual que en `BeatService`: los toplines también cuentan para el límite del plan. No es `readOnly`. |
| `create(Long artistId, Topline)` | 1) Busca el beat real por `topline.getBeat().getId()`; si no existe → `null` (404). 2) Suma una producción. 3) Pone el beat real y el `artistId`, guarda. 4) **Crea la `Collaboration` en `PENDING`** con el id del topline. Todo en una transacción. |
| `list(beatId, artistId)` | Filtra por beat, o por artista, o devuelve todos. |
| `getById(Long)` | `orElse(null)`. |
| `canModify(Topline, Long)` | true si es el artista que lo subió o un admin. |
| `update(Long, Topline)` | Solo se puede cambiar `audioUrl`. |
| `delete(Long)` | Por el CASCADE, se borran su colaboración y sus comentarios. |

#### `service/CollaborationService.java`

| Método | Qué hace |
|---|---|
| `getById(Long)` | `orElse(null)`. |
| `canDecide(Collaboration, Long)` | Va de la colaboración al topline, del topline al beat, y compara el `producerId` del beat con el que pide. **Solo el dueño del beat decide.** |
| `decide(Long, CollaborationStatus)` | Guarda el estado nuevo y `decidedAt = ahora`. |
| `listByStatus(status)` | Filtra por estado, o devuelve todas. |
| `canDelete(Collaboration, Long)` | Pueden borrar las dos partes (el artista del topline o el productor del beat) o un admin. |
| `delete(Long)` | `deleteById`. |

#### `service/CommentService.java`

`create` y `listByTopline`, igual que `BeatCommentService` pero sobre toplines.

#### Controllers

**`ToplineController`** — `/api/toplines`

| Endpoint | Reglas |
|---|---|
| `POST` → `createTopline` | 401 → 400 si falta `beat.id` o `audioUrl` → 403 si no es artista → 403 si llegó al límite → 404 si el beat no existe → 201 |
| `GET` → `listToplines` | Público. `?beatId=` o `?artistId=` opcionales |
| `GET /{id}` → `getTopline` | 404 / 200 |
| `PUT /{id}` → `updateTopline` | 401 → 404 → 403 → 200 |
| `DELETE /{id}` → `deleteTopline` | 401 → 404 → 403 → 204 |

**`CollaborationController`** — `/api/collaborations`

- **No tiene POST**: una colaboración nace sola con el topline.
- `PUT /{id}?status=ACCEPTED` → `decide`: el estado viene como **query param** y Spring lo
  convierte al enum solo. 401 → 404 → 403 si no es el dueño del beat → 200.
- `GET ?status=PENDING` → `list`: público, filtro opcional.
- `DELETE /{id}` → `deleteCollaboration`: 401 → 404 → 403 → 204.

**`CommentController`** — `/api/toplines/{toplineId}/comments`: `POST` (401 → 400 → 404 →
201) y `GET` (404 / 200).

---

### 9.5 Paquete `billing` (Dani)

Suscripciones con plan FREE o PREMIUM y un pago **simulado**.

#### `model/SubscriptionPlan.java` (enum)

`FREE` (máximo 50 producciones) y `PREMIUM` (sin límite).

#### `model/Subscription.java` (→ `subscriptions`)

- `userId`: `unique` (una suscripción por usuario).
- `plan`: arranca en `FREE`.
- `productionsCount`: cuántos beats + toplines creó el usuario. Es `int` (no `Integer`) porque
  siempre tiene valor (arranca en 0).
- `createdAt`: `@PrePersist`.

#### `repository/SubscriptionRepository.java`

- `findByUserId(Long)`.

#### `gateway/PaymentGateway.java` (interfaz)

```java
public interface PaymentGateway {
    PaymentResult charge(Long userId, BigDecimal amount);
}
```

Una **interfaz** es un "contrato": dice **qué** se puede hacer (cobrar) pero no **cómo**.
`SubscriptionService` depende de la interfaz, no de una clase concreta.

#### `gateway/SimulatedPaymentGateway.java` (`@Component`)

- `implements PaymentGateway`. `charge(...)` **siempre aprueba** y devuelve una referencia
  falsa `"SIMULATED-<uuid>"`.
- **¿Cómo sabe Spring cuál usar?** `SubscriptionService` pide un `PaymentGateway` en su
  constructor, y la única clase que lo implementa y es bean es `SimulatedPaymentGateway`. Si
  mañana hacemos `MercadoPagoGateway`, se cambia esa pieza y `SubscriptionService` no se toca.
  Eso es **polimorfismo**.

#### `gateway/PaymentResult.java` (record)

```java
public record PaymentResult(boolean approved, String reference) {}
```

Un **record** es una clase para guardar datos que no cambian. Java genera solo el constructor,
los "getters" (`approved()`, `reference()`), `equals` y `toString`. Se usa acá porque el
resultado de un pago es solo un dato.

#### `service/SubscriptionService.java`

- Constantes: `PREMIUM_PRICE_USD = new BigDecimal("15.00")` y `FREE_PLAN_LIMIT = 50`.
  `static final` = una sola copia compartida y que no cambia.

| Método | Qué hace |
|---|---|
| `userExists(Long)` / `isArtist(Long)` | Para los 404/403 del controller. Solo los artistas tienen plan. |
| `getOrCreate(Long)` | Si el usuario ya tiene suscripción la devuelve; si no, **le crea una FREE**. Así el resto del código nunca tiene que preguntar "¿y si no tiene?". |
| `upgrade(Long)` | Si ya es PREMIUM, no hace nada (no cobra dos veces). Si no, cobra 15 USD con el gateway; si se aprueba, pasa a PREMIUM. Si se rechazara, queda FREE. |
| `downgrade(Long)` | Vuelve a FREE. |
| `isAtProductionLimit(Long)` | true si es FREE **y** tiene 50 o más producciones. Lo llaman `BeatService` y `ToplineService`. **No es `readOnly`** porque llama a `getOrCreate`, que puede hacer un INSERT. |
| `recordProduction(Long)` | Suma 1 al contador. Lo llaman al crear un beat o un topline. |

#### `controller/SubscriptionController.java` — `/api/subscriptions`

| Endpoint | Qué hace |
|---|---|
| `GET /me` | Ver mi plan |
| `POST /upgrade` | Pasar a PREMIUM |
| `PUT /downgrade` | Volver a FREE |

Los tres usan el mismo método privado `checkAccess(userId)`: 401 si no hay login, 404 si el
usuario no existe, 403 si no es artista. Devuelve `null` si está todo bien, o la respuesta de
error lista para devolver. **Se escribió una vez para no repetir los mismos 3 `if` en los tres
endpoints.**

---

### 9.6 Paquete `challenges` (Paolo)

El módulo con más lógica: concursos con jurado ponderado.

**El flujo del negocio:**

1. Un ADMIN o una DISCOGRAFICA crea un desafío (género, BPM, tema, deadline, premios) y elige un
   **artista invitado** que hace de jurado.
2. Los artistas mandan su **entrega** (submission) antes del deadline.
3. Los usuarios **votan** cada entrega del 1 al 10 (una vez por entrega).
4. El artista invitado puede elegir su favorita (**opportunity pick**).
5. Un ADMIN **cierra** el desafío: se calcula el puntaje de cada entrega, se arma el top 3, se
   guardan los resultados con puntos y premios, y **el ganador queda verificado**.
6. El **ranking** general suma los puntos de todos los desafíos.

#### `model/Challenge.java` (→ `challenges`)

| Campo | Detalle |
|---|---|
| `createdBy` | `READ_ONLY`, lo pone el service desde la sesión |
| `title`, `genre`, `bpm`, `key` (`music_key`), `theme` | Datos del concurso |
| `deadline` | `Instant`. Después de esta fecha no se aceptan entregas |
| `guestArtistId` | El artista invitado (jurado) |
| `prizeFirst/Second/Third` | Texto del premio de cada puesto |
| `opportunityPickSubmissionId` | La entrega elegida por el invitado (`null` hasta que elija) |
| `createdAt` | `@PrePersist` |
| `submissions` | `@OneToMany(mappedBy = "challenge")` + `@JsonIgnore` (lado inverso de `Submission.challenge`) |

**No hay un campo "cerrado".** Un desafío se considera cerrado si ya tiene resultados en
`challenge_results` (`ChallengeService.isClosed`).

#### `model/Submission.java` (→ `submissions`)

- `challenge`: `@ManyToOne` + `@JoinColumn(name = "challenge_id")`. **La relación JPA del
  módulo de Paolo.** Permite ir de la entrega al desafío completo.
- `producerId`: desde la sesión. `audioUrl`. `submittedAt` (`@PrePersist`).

#### `model/Vote.java` (→ `votes`)

- `submissionId`, `voterId` (los pone el service), `score` (1 a 10), `comment` opcional.

#### `model/ChallengeResult.java` (→ `challenge_results`)

Una fila por cada entrega del podio: `challengeId`, `submissionId` (`unique`), `rank` (columna
`rank_position`), `pointsAwarded`, `badge` ("Ganador del desafío" para el 1º) y `prizeText`.

#### `model/RankingEntry.java` (record)

`RankingEntry(Long producerId, int totalPoints)`. **No es una tabla**: se arma en memoria al
calcular el ranking y se devuelve como JSON.

#### Repositorios

| Repositorio | Métodos propios | Para qué |
|---|---|---|
| `ChallengeRepository` | ninguno | Con los de `JpaRepository` alcanza |
| `SubmissionRepository` | `findByChallengeId`, `findByProducerId` | Entregas de un desafío / de un productor |
| `VoteRepository` | `findBySubmissionId`, `existsBySubmissionIdAndVoterId` | Votos de una entrega / ¿ya votó? |
| `ChallengeResultRepository` | `findByChallengeId`, `findBySubmissionIdIn(List<Long>)`, `existsByChallengeId` | Podio de un desafío / resultados de varias entregas en **una** consulta (`WHERE submission_id IN (...)`) / ¿está cerrado? |

#### `service/ChallengeService.java`

| Método | Qué hace |
|---|---|
| `canCreateChallenge(Long)` | true si es ADMIN o DISCOGRAFICA. Un artista participa pero no organiza. |
| `userExists(Long)` / `isArtist(Long)` | Validan que el `guestArtistId` exista y sea artista. |
| `create(Long, Challenge)` | Pone `createdBy` y guarda. |
| `list()` / `getById(Long)` | Lecturas. |
| `isGuestArtist(Challenge, Long)` | true si el que pide es el invitado. |
| `setOpportunityPick(Challenge, Long submissionId)` | Verifica que la entrega exista **y sea de este desafío** (si no → `null` → 400) y la guarda como favorita. |
| `canModify(Challenge, Long)` | El creador o un admin. |
| `isClosed(Long)` | `existsByChallengeId` en resultados. |
| `update(Long, Challenge)` | Update parcial de título, tema y deadline. |
| `delete(Long)` | `deleteById` (CASCADE borra entregas, votos y resultados). |

#### `service/SubmissionService.java`

- Usa `ChallengeRepository` y **no** `ChallengeService`. **¿Por qué?** Porque
  `ChallengeService` ya depende de `SubmissionService`. Si cada uno necesitara al otro en el
  constructor, Spring no podría crear ninguno primero (dependencia circular).
- `isArtist`, `getChallenge`, `isPastDeadline(Challenge)` (`Instant.now().isAfter(deadline)`),
  `create` (pone el desafío y el productor), `listByChallenge`, `getById`.

#### `service/VoteService.java`

- `alreadyVoted(submissionId, voterId)`: el controller responde 403 si ya votó.
- `create(...)`: `null` si la entrega no existe (404). Pone `submissionId` y `voterId`.

#### `service/ChallengeScoringService.java` — el cálculo del puntaje

Pesos (suman 1.0 = 100%):

```java
COMMUNITY_WEIGHT = 0.30;   // cualquier usuario
VERIFIED_WEIGHT  = 0.30;   // artistas verificados
GUEST_WEIGHT     = 0.40;   // el artista invitado
```

`computeScore(guestArtistId, verifiedProducerIds, votes)`:

1. Recorre los votos y separa cada uno en un grupo: si el votante es el invitado → su puntaje;
   si está en el `Set` de verificados → lista de verificados; si no → lista de comunidad.
2. Promedia cada lista con `average(...)` (si una lista está vacía devuelve 0, para no dividir
   por cero).
3. Si el invitado no votó, su parte vale 0.
4. `puntaje = 0.30 × promedio comunidad + 0.30 × promedio verificados + 0.40 × voto invitado`.

**Ejemplo:** comunidad votó 8 y 6 (promedio 7), no votó ningún verificado (0), el invitado votó 9.
`0.30 × 7 + 0.30 × 0 + 0.40 × 9 = 2.1 + 0 + 3.6 = 5.7`.

**¿Por qué ponderado?** Para que no gane el que tiene más amigos votando: la opinión del
invitado y de los verificados pesa más que la de cualquiera.

#### `service/ChallengeResultService.java` — el cierre

- `POINTS_BY_RANK = {500, 300, 150}` y `TOP_N = 3`.
- `isAdmin(Long)`: solo un admin cierra.
- **`close(Challenge)`**, paso a paso:
  1. Trae todas las entregas del desafío.
  2. Arma un `Set<Long>` con los ids de los artistas verificados (**una sola** consulta, en vez de
     preguntar por cada voto).
  3. Calcula el puntaje de cada entrega **una vez** y lo guarda en un `Map<idEntrega, puntaje>`.
  4. **Ordena con "selección"** (selection sort) solo los primeros 3 puestos:
     ```java
     for (int i = 0; i < podiumSize; i++) {
         int bestIndex = i;
         for (int j = i + 1; j < ranked.size(); j++) {
             if (puntaje(j) > puntaje(bestIndex)) bestIndex = j;
         }
         // intercambiar las posiciones i y bestIndex
     }
     ```
     En la vuelta `i = 0` busca la mejor de todas y la pone primera; en `i = 1` la mejor de las
     que quedan y la pone segunda; etc. Usamos dos `for` a mano en vez de `sort` con lambdas
     porque es lo que sabemos explicar.
  5. Por cada puesto crea un `ChallengeResult` con puesto, puntos (500/300/150), premio y, para
     el 1º, el badge "Ganador del desafío" + **verifica al ganador** (`verifyWinner`).
  6. Devuelve la lista de resultados (eso es lo que se ve en Postman).
- `listResults(producerId)`: si viene productor, junta los ids de sus entregas y trae sus
  resultados con `findBySubmissionIdIn`; si no, todos.
- `ranking()`: recorre todos los resultados, suma los puntos por productor en un
  `Map<productorId, puntos>` (con `containsKey` para saber si ya estaba), lo convierte en
  `RankingEntry` y lo ordena de mayor a menor con el mismo selection sort.
- `scoreFor(...)` (privado): trae los votos de una entrega y llama a
  `ChallengeScoringService.computeScore`.
- `prizeFor(challenge, rank)` (privado): devuelve `prizeFirst`, `prizeSecond` o `prizeThird`.
- `verifyWinner(producerId)` (privado): si el ganador tiene perfil de artista, `verified = true`.
  Usa `Optional.isPresent()` / `get()`.

#### Controllers

**`ChallengeController`** — `/api/challenges`

| Endpoint | Reglas |
|---|---|
| `POST` → `createChallenge` | 401 → 400 (title, genre, bpm, deadline, guestArtistId) → 403 si no es ADMIN/DISCOGRAFICA → 404 si el invitado no existe → 403 si el invitado no es artista → 201 |
| `GET` → `listChallenges` | Público |
| `GET /{id}` → `getChallenge` | 404 / 200 |
| `PUT /{id}` → `updateChallenge` | 401 → 404 → 403 (no es creador ni admin) → 403 si ya está cerrado → 400 si manda título vacío → 200 |
| `DELETE /{id}` → `deleteChallenge` | 401 → 404 → 403 → 403 si está cerrado → 204 |
| `PUT /{id}/opportunity-pick?submissionId=X` → `opportunityPick` | 401 → 404 → 403 si no es el invitado → 400 si la entrega no es de este desafío → 200 |

**`ChallengeCloseController`**: `PUT /api/challenges/{id}/close` → 401 → 403 si no es admin →
404 → 200 con el podio. Está separado porque cerrar dispara todo el cálculo, no es un CRUD.

**`SubmissionController`** — `/api/challenges/{challengeId}/submissions`: `POST` (401 → 400 sin
audioUrl → 403 si no es artista → 404 si no existe el desafío → 403 si pasó el deadline → 201) y
`GET` (lista).

**`VoteController`** — `/api/submissions/{submissionId}/votes`: `POST` (401 → 400 si el score no
está entre 1 y 10 → 403 si ya votó → 404 si la entrega no existe → 201).

**`RankingController`** (públicos): `GET /api/ranking` y
`GET /api/challenges/results?producerId=X`.

---

## 10. Relaciones JPA (resumen)

Se eligió **una relación real por módulo**, para que cada integrante tenga su ejemplo:

| Módulo | Relación | Tipo | Dueño de la FK | Lado inverso |
|---|---|---|---|---|
| `users` | `ArtistProfile.user` → `User` | `@OneToOne` | `artist_profiles.user_id` | — |
| `users` | `Session.user` → `User` | `@ManyToOne` | `sessions.user_id` | — |
| `catalog` | `BeatComment.beat` → `Beat` | `@ManyToOne` | `beat_comments.beat_id` | `Beat.comments` (`@OneToMany(mappedBy)`) |
| `collab` | `Topline.beat` → `Beat` | `@ManyToOne` | `toplines.beat_id` | — |
| `challenges` | `Submission.challenge` → `Challenge` | `@ManyToOne` | `submissions.challenge_id` | `Challenge.submissions` (`@OneToMany(mappedBy)`) |

**Conceptos para defender:**

- **Lado dueño**: el que tiene `@JoinColumn`. Es el que escribe la clave foránea en la base.
- **Lado inverso**: el que tiene `mappedBy`. Solo lee; no agrega columnas.
- **`@JsonIgnore` en el lado inverso**: si no, Jackson entra en un bucle infinito al armar el
  JSON (beat → comentarios → beat → comentarios...).
- **El resto de las referencias son ids sueltos (`Long producerId`)**: funcionan igual (la FK
  existe en la base), pero en Java solo tenemos el número. Es más simple y fue una decisión
  consciente.

---

## 11. Cómo se conectan los módulos entre sí

```
users  ◄──────── todos (UserRepository para roles, ArtistProfileRepository)
   ▲
billing ◄──────── catalog (BeatService)  y  collab (ToplineService)   [límite del plan]
   ▲
catalog ◄──────── collab (Topline.beat, BeatRepository)
challenges ──────► users (roles, verificar ganador)
```

- `users` no depende de nadie (es la base).
- `catalog` usa `users` (roles) y `billing` (límite).
- `collab` usa `users`, `catalog` (el beat del topline) y `billing`.
- `challenges` usa `users` (roles, perfiles verificados).
- `billing` usa `users` (¿es artista?).
- Siempre se usa el **repositorio o el service** del otro módulo, nunca su controller.

---

## 12. Tests

Hay **188 tests** en 43 archivos, en `src/test/java/com/mgwprod/`, con la misma estructura de
carpetas que el código. Se corren con:

```bash
./mvnw test
```

Tiene que terminar con `Tests run: 188, Failures: 0, Errors: 0` y `BUILD SUCCESS`. **No hace
falta tener MySQL prendido**: los tests usan H2 en memoria (sección 5).

### Los 4 tipos de test que hay

**1. Tests de service (unitarios, con Mockito)** — ej. `BeatServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class BeatServiceTest {
    @Mock private BeatRepository beatRepository;          // repositorio FALSO
    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private BeatService beatService;         // el service REAL, con los falsos adentro

    @Test
    void createSavesBeat() {
        when(beatRepository.save(any(Beat.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));   // "cuando guarden, devolvé lo mismo"
        Beat saved = beatService.create(1L, beat);
        assertThat(saved.getProducerId()).isEqualTo(1L);            // verificamos el resultado
    }
}
```

- **Mock** = un objeto de mentira que imita a otro. Le decimos qué devolver con
  `when(...).thenReturn(...)`. Así probamos **solo** la lógica del service, sin base de datos.
- `@InjectMocks` crea el service real y le pasa los mocks por el constructor.
- `verify(repo).deleteById(1L)` comprueba que el service **llamó** a ese método.
- `assertThat(...)` (AssertJ) comprueba que un valor es el esperado.

**2. Tests de controller (con MockMvc)** — ej. `BeatControllerTest`

```java
@WebMvcTest(BeatController.class)
class BeatControllerTest {
    @Autowired private MockMvc mockMvc;               // simula requests HTTP
    @MockitoBean private BeatService beatService;     // service falso
    @MockitoBean private SessionRepository sessionRepository;  // lo pide el interceptor

    @Test
    void createBeatReturns201WhenAuthenticatedAsArtist() throws Exception {
        when(beatService.isArtist(1L)).thenReturn(true);
        mockMvc.perform(post("/api/beats")
                    .requestAttr("userId", 1L)                 // como si el interceptor lo hubiera puesto
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())               // esperamos 201
               .andExpect(jsonPath("$.title").value("Trap Beat"));
    }
}
```

- `@WebMvcTest` levanta **solo** la capa web de ese controller (sin base).
- Prueban que cada camino devuelva el **código HTTP correcto** (201, 400, 401, 403, 404...).
- `jsonPath("$.title")` lee un campo del JSON de respuesta.

**3. Tests de repositorio** — ej. `BeatRepositoryTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class BeatRepositoryTest {
    @Autowired private BeatRepository beatRepository;
    @Test
    void findByProducerIdReturnsOnlyThatProducersBeats() {
        beatRepository.save(beat);
        assertThat(beatRepository.findByProducerId(1L)).hasSize(1);
    }
}
```

- `@DataJpaTest` levanta JPA con la base H2 **de verdad** (en memoria). Prueba que los métodos
  "que se escriben solos" generen la consulta correcta.

**4. Otros**

- `MgwProdApplicationTests.contextLoads()`: `@SpringBootTest` levanta **toda** la app. Si algún
  bean está mal configurado, falla. Es el test más básico: "¿arranca?".
- `UserJsonTest` / `ProfileJsonTest`: prueban que el JSON **nunca** muestre `passwordHash` ni
  `password`, y que el perfil no incluya el usuario.
- `PasswordHasherTest`: que el hash de la misma contraseña no sea igual dos veces (salt) y que
  `matches` funcione.
- `SessionAuthInterceptorTest`: sin token pasa, token vencido da 401, token válido pone el
  `userId`.

**Nombres de los tests:** describen el caso, ej. `createBeatReturns401WhenNotAuthenticated`
= "crear un beat devuelve 401 cuando no está autenticado".

### Tests por archivo

| Módulo | Archivo | Tests |
|---|---|---|
| — | `MgwProdApplicationTests` | 1 |
| users | `AuthControllerTest` / `UserControllerTest` / `ArtistVerificationControllerTest` | 5 / 10 / 3 |
| users | `AuthServiceTest` / `UserServiceTest` | 7 / 14 |
| users | `PasswordHasherTest` / `SessionAuthInterceptorTest` | 3 / 3 |
| users | `UserJsonTest` / `ProfileJsonTest` | 3 / 2 |
| catalog | `BeatControllerTest` / `BeatCommentControllerTest` | 11 / 4 |
| catalog | `BeatServiceTest` / `BeatCommentServiceTest` | 11 / 2 |
| catalog | `BeatRepositoryTest` / `BeatCommentRepositoryTest` | 2 / 2 |
| collab | `ToplineControllerTest` / `CollaborationControllerTest` / `CommentControllerTest` | 7 / 6 / 3 |
| collab | `ToplineServiceTest` / `CollaborationServiceTest` / `CommentServiceTest` | 11 / 7 / 1 |
| collab | `ToplineRepositoryTest` / `CollaborationRepositoryTest` / `CommentRepositoryTest` | 1 / 1 / 1 |
| billing | `SubscriptionControllerTest` / `SubscriptionServiceTest` | 5 / 10 |
| billing | `SubscriptionRepositoryTest` / `SimulatedPaymentGatewayTest` | 2 / 1 |
| challenges | `ChallengeControllerTest` / `ChallengeCloseControllerTest` / `SubmissionControllerTest` / `VoteControllerTest` / `RankingControllerTest` | 10 / 3 / 4 / 2 / 2 |
| challenges | `ChallengeServiceTest` / `ChallengeResultServiceTest` / `ChallengeScoringServiceTest` / `SubmissionServiceTest` / `VoteServiceTest` | 12 / 4 / 3 / 4 / 3 |
| challenges | `ChallengeRepositoryTest` / `ChallengeResultRepositoryTest` / `SubmissionRepositoryTest` / `VoteRepositoryTest` | 1 / 1 / 1 / 2 |

---

## 13. Postman

### 13.1 Qué es y por qué lo usamos

Postman es un programa para mandar requests HTTP a mano y ver la respuesta. En la Etapa 1 no
hay pantalla, así que **Postman hace de cliente**. La cátedra pide que en la defensa se muestre
el CRUD funcionando **con requests reales en vivo** (no corriendo un `main` en el IDE), y que la
colección se exporte en formato **Collection v2.1**.

### 13.2 Archivos

| Archivo | Rama | Qué es |
|---|---|---|
| `docs/api/mgw-prod-demo.postman_collection.json` | main | **La demo corta para la defensa** (16 requests) |
| `docs/api/mgw-prod.postman_collection.json` | main | La colección completa (53 requests), para repasar |
| `docs/api/mgw-prod.postman_collection.json` | entrega | En la entrega, este nombre es **la demo de 16** |
| `docs/api/mgw-prod.postman_environment.json` | ambas | El environment "mgw-prod local" |
| `docs/GUIA-DEMO-POSTMAN.md` | main | El guion de qué decir en cada request |

### 13.3 Cómo importarla y correrla

1. Levantar MySQL y la app (`./mvnw spring-boot:run`).
2. Postman → **Import** → arrastrar la colección y el environment.
3. Arriba a la derecha, elegir el environment **mgw-prod local**.
4. Mandar los requests **en orden** (Send uno por uno), o click derecho en la colección →
   **Run collection** → Run para correrlos todos.
5. La demo tiene que dar **16/16 en verde**. Se puede correr las veces que quieras sin
   resetear la base.

### 13.4 Conceptos de Postman que hay que saber explicar

**Environment y variables.** Un environment es un conjunto de variables. `{{base_url}}` vale
`http://localhost:8080`; si el backend se mudara a otro servidor, se cambia solo ahí. Las demás
variables (`artistToken`, `beatId`...) empiezan vacías y **las van llenando los requests**.

**Header `Authorization: Bearer {{artistToken}}`.** Así se manda el token del login. Es lo que
lee `SessionAuthInterceptor`.

**Body raw JSON + header `Content-Type: application/json`.** Le dice al servidor que el body es
JSON, para que Jackson lo convierta al objeto.

**Pestaña "Tests"** (se ejecuta **después** de recibir la respuesta):

```javascript
pm.test("Status 201", function () {
    pm.response.to.have.status(201);        // chequea el código de respuesta
});
pm.environment.set("beatId", pm.response.json().id);   // guarda el id para los próximos requests
```

- `pm` es el objeto de Postman. `pm.response.json()` es el body de la respuesta.
- `pm.test` crea una verificación con nombre: si falla, se ve en rojo.
- `pm.environment.set` guarda un valor en el environment. **Así se encadenan los requests:** el
  login guarda el token, crear beat guarda `beatId`, y editar beat usa `{{beatId}}`.

**Pestaña "Pre-request Script"** (se ejecuta **antes** de mandar el request):

```javascript
pm.environment.set("artistEmail", "artista" + Date.now() + "@mgw.test");
```

`Date.now()` son los milisegundos actuales: cada corrida genera un email distinto. Si no,
la segunda vez el registro daría **409** (email repetido).

**El DELETE con Pre-request Script** (lo que se vio en la Clase 5):

```javascript
pm.sendRequest({
    url: pm.environment.get("base_url") + "/api/beats",
    method: "POST",
    header: { "Content-Type": "application/json",
              "Authorization": "Bearer " + pm.environment.get("artistToken") },
    body: { mode: "raw", raw: JSON.stringify({ title: "Beat para borrar", ... }) }
}, function (err, res) {
    pm.environment.set("beatToDeleteId", res.json().id);
});
```

Antes del DELETE, el script **crea** un beat con un POST "escondido" y guarda su id. Después el
DELETE borra ese. Así el DELETE nunca depende de lo que haya en la base (si lo corrés dos veces
no da 404). `JSON.stringify` convierte el objeto JavaScript en texto JSON.

### 13.5 La demo (16 requests), request por request

| # | Carpeta | Request | Token | Esperado | Guarda |
|---|---|---|---|---|---|
| 1 | Usuarios | Registrar artista (`POST /api/auth/register`) | — | 201 | `artistEmail` (pre), `artistUserId` |
| 2 | Usuarios | Login artista (`POST /api/auth/login`) | — | 200 | `artistToken` |
| 3 | Usuarios | Registrar admin | — | 201 | `adminEmail` (pre) |
| 4 | Usuarios | Login admin | — | 200 | `adminToken` |
| 5 | Beats | Crear beat (`POST /api/beats`) | artista | 201 | `beatId` |
| 6 | Beats | Listar beats (`GET /api/beats`) | — | 200 | |
| 7 | Beats | Editar beat (`PUT /api/beats/{{beatId}}`, cambia título y bpm) | artista | 200 | |
| 8 | Beats | Borrar beat (`DELETE`, con pre-request que lo crea) | artista | 204 | `beatToDeleteId` (pre) |
| 9 | Toplines | Crear topline (`POST /api/toplines`, `"beat": {"id": {{beatId}}}`) | artista | 201 | `toplineId` |
| 10 | Suscripción | Pasar a premium (`POST /api/subscriptions/upgrade`) | artista | 200 | |
| 11 | Desafíos | Crear desafío (`guestArtistId` = el artista) | admin | 201 | `challengeId` |
| 12 | Desafíos | Enviar entrega | artista | 201 | `submissionId` |
| 13 | Desafíos | Votar la entrega (score 9) | admin | 201 | |
| 14 | Desafíos | Cerrar desafío y ver podio | admin | 200 | |
| 15 | Errores | Crear beat sin token | — | 401 | |
| 16 | Errores | Beat que no existe (`GET /api/beats/999999`) | — | 404 | |

**Qué mostrar en el cierre (request 14):** la respuesta es una lista con un resultado:
`rank: 1`, `pointsAwarded: 500`, `badge: "Ganador del desafío"`, `prizeText: "Sesión de
estudio"`. El puntaje fue `0.30 × 9` (el voto del admin cuenta como "comunidad") = 2.7.

Errores que se pueden armar en vivo si el profe los pide:

- **400**: crear beat con `"title": ""`.
- **403**: crear desafío usando `{{artistToken}}` (un artista no puede crear desafíos).
- **409**: mandar dos veces el mismo registro sin el pre-request (mismo email).

El guion completo, con quién presenta cada parte y qué decir, está en `docs/GUIA-DEMO-POSTMAN.md`.

### 13.6 La colección completa (53 requests, solo en main)

Para repasar. Cubre GET-lista, GET-por-id, POST, PUT y DELETE de los 5 módulos, más una carpeta
de errores con los 5 códigos (400, 401, 403, 404, 409). Carpetas: Users (14), Catalog (8),
Collab (10), Challenges (13), Billing (3), Casos de error (5).

### 13.7 Si algo falla en la demo

| Síntoma | Causa |
|---|---|
| `Could not send request` / `ECONNREFUSED` | La app no está corriendo |
| 401 en todo | No se eligió el environment, o no se corrió el login |
| 404 en un request del medio | Se salteó un request anterior que guardaba ese id |
| 500 | Mirar la consola donde corre `./mvnw spring-boot:run`: ahí está el error |

---

## 14. Códigos HTTP: cuál se usa dónde

| Código | Significa | Dónde aparece |
|---|---|---|
| **200 OK** | Salió bien, devuelvo datos | GET, PUT, login, upgrade, close |
| **201 Created** | Se creó algo nuevo | Todos los POST que crean (register, beat, topline, desafío, entrega, voto, comentario) |
| **204 No Content** | Salió bien, no hay nada que devolver | Todos los DELETE |
| **400 Bad Request** | Mandaste datos inválidos | Campos vacíos, bpm < 1, score fuera de 1-10, contraseña < 8, opportunity pick de otro desafío |
| **401 Unauthorized** | No estás logueado (o el token es inválido/vencido) | Cualquier endpoint privado sin token; login incorrecto |
| **403 Forbidden** | Estás logueado pero no tenés permiso | No sos el dueño, rol equivocado, límite del plan, deadline vencido, ya votaste, desafío cerrado |
| **404 Not Found** | No existe | Cualquier id que no está en la base |
| **409 Conflict** | Choca con algo existente | Email ya registrado; borrar un usuario con beats |
| **500** | Error del servidor (bug) | No debería pasar; ver sección 16 |

**401 vs 403:** 401 = "no sé quién sos". 403 = "sé quién sos, pero no podés".

---

## 15. Preguntas probables del profesor (y cómo responder)

**¿Por qué no usaron DTOs?**
Porque seguimos el patrón de la Clase 4: el controller recibe y devuelve la entidad. Para no
exponer datos sensibles usamos anotaciones de Jackson (`@JsonIgnore` en `passwordHash`,
`WRITE_ONLY` en `password`).

**¿Por qué no usaron excepciones ni `@Valid`?**
Porque no lo vimos en clase. Validamos con `if` en el controller, el service devuelve `null`
cuando algo no existe, y los permisos son métodos `boolean` (`canModify`, `isArtist`...).
Todo el proyecto sigue el mismo patrón.

**¿Cómo funciona el login?**
Registro → la contraseña se guarda hasheada con SHA-256 + salt. Login → si coincide, se crea
una `Session` con un token UUID que vence en 24 hs. El cliente manda el token en el header
`Authorization: Bearer ...`; el `SessionAuthInterceptor` lo busca en la tabla `sessions` y deja
el `userId` en el request para los controllers.

**¿Por qué no usaron Spring Security?**
No se vio en clase. Hicimos una versión simple a mano que cumple lo mismo para esta etapa.

**¿Qué es `@Transactional`?**
Que todas las operaciones del método contra la base se confirman juntas o se deshacen juntas.
Ejemplo: crear un topline y su colaboración.

**¿Qué diferencia hay entre `@ManyToOne` y `@OneToMany`?**
`@ManyToOne` va en el lado que tiene la clave foránea (muchos comentarios → un beat).
`@OneToMany(mappedBy = ...)` es el otro lado, solo para leer (un beat → sus comentarios).

**¿Por qué `ddl-auto=none`?**
Porque las tablas las controlamos nosotros con `schema.sql`, como se vio en clase. Hibernate no
toca la estructura de la base.

**¿Cómo se calcula el ganador de un desafío?**
Puntaje ponderado: 30% promedio de la comunidad, 30% promedio de verificados, 40% voto del
invitado. Se ordena con selection sort, se guarda el top 3 con 500/300/150 puntos y el ganador
queda verificado.

**¿Qué pasa si alguien intenta editar algo que no es suyo?**
`canModify` compara el dueño con el `userId` de la sesión; si no es el dueño ni admin → 403.

**¿Qué pasa si mando `"producerId": 99` en el JSON?**
Se ignora: el service lo pisa con el `userId` de la sesión.

**¿Por qué el pago es simulado?**
La consigna ya no pide e-commerce real. Hicimos la interfaz `PaymentGateway` para que conectar
un pago real (por ejemplo Mercado Pago) sea agregar una clase nueva sin tocar el service.

**¿Para qué sirve el interceptor?**
Para no repetir en cada controller el código de "leer el token y buscar la sesión". Se escribe
una vez y se aplica a todo `/api/**` menos login y registro.

---

## 16. Limitaciones conocidas y bugs que encontramos

### Bugs reales que encontramos y arreglamos

1. **Transacción de solo lectura.** Un artista nuevo intentaba publicar su **primer** beat y
   daba **500**. Causa: `isAtProductionLimit` estaba marcado `@Transactional(readOnly = true)`,
   pero por dentro llamaba a `getOrCreate`, que crea la suscripción la primera vez (un INSERT).
   MySQL no permite escribir en una transacción de solo lectura. Arreglo: sacar el `readOnly` en
   `SubscriptionService`, `BeatService` y `ToplineService`. Solo apareció probando contra MySQL
   real (en los tests con mocks no se veía).
2. **Postman desactualizado.** Cuando `Topline` pasó a tener la relación `beat`, la colección
   seguía mandando `"beatId": 5` en vez de `"beat": {"id": 5}` → 400. Se corrigió la colección.
3. **Jackson y `@AllArgsConstructor` en `User`.** Con el constructor con todos los campos,
   Jackson 3 fallaba al registrarse si faltaba algún campo. Se dejó solo `@NoArgsConstructor`.

### Limitaciones que siguen (saberlas por si preguntan)

| Limitación | Por qué pasa | Cómo se arreglaría |
|---|---|---|
| **Cualquiera puede registrarse como ADMIN** | `/api/auth/register` acepta cualquier rol | Que el registro solo permita ARTIST/DISCOGRAFICA y el admin se cree a mano en la base |
| **Cerrar un desafío dos veces da 500** | `close` no chequea `isClosed`, y el segundo cierre intenta guardar resultados repetidos (`submission_id` es UNIQUE) | En `ChallengeCloseController`, si `isClosed(id)` → 403 antes de cerrar |
| **Una colaboración se puede volver a decidir** (incluso volver a `PENDING`) | `decide` no mira el estado actual | Solo permitir decidir si está `PENDING` y solo con ACCEPTED/REJECTED |
| **Se puede votar la propia entrega** | `VoteController` no compara votante con productor | Chequear `submission.getProducerId()` distinto del votante |
| **No hay logout** | Las sesiones solo vencen a las 24 hs | Un `DELETE /api/auth/logout` que borre la sesión |
| **Condiciones de carrera en el contador del plan** | Dos publicaciones al mismo tiempo pueden pasar el límite de 50 | Bloqueo en la base (no visto en clase) |
| **SHA-256 es rápido** | Para contraseñas conviene un algoritmo lento (bcrypt) | Usar bcrypt (viene con Spring Security) |
| **`spring-boot-starter-validation` sin usar** | Quedó de la versión con `@Valid` | Sacarla del `pom.xml` junto con `validation.mode=none` |

---

## 17. Diferencia entre la rama `main` y la rama `entrega/etapa1`

| | `main` | `entrega/etapa1` |
|---|---|---|
| Para qué | Estudiar | Lo que se entrega |
| Código | El mismo | El mismo |
| Comentarios | Uno por método + explicaciones largas | ~33 comentarios cortos, solo donde algo no es obvio |
| Postman | Demo (16) + completa (53) | Solo la demo (16) |
| Docs | Guías del equipo, esta guía, docs de endpoints | README, consigna, `schema.sql` |
| Historial | Completo | Un solo commit |

**Regla:** si se cambia código, se cambia en `main` y después se replica en la entrega (nunca
mergear `main` en `entrega/etapa1`, porque traería todos los comentarios y documentos).

---

## 18. Glosario

| Término | Significado |
|---|---|
| **API REST** | Forma de exponer funciones por HTTP: recursos (`/api/beats`) + verbos (GET, POST, PUT, DELETE) + JSON. |
| **Endpoint** | Una URL + un verbo que hace algo (`POST /api/beats`). |
| **Bean** | Objeto que crea y administra Spring (services, controllers, repositorios). |
| **Inyección de dependencias** | Spring le pasa a cada clase los objetos que pide en el constructor. |
| **Entidad** | Clase Java con `@Entity` que representa una tabla. |
| **JPA** | El estándar de Java para mapear objetos ↔ tablas. |
| **Hibernate** | La implementación de JPA que usa Spring; genera el SQL. |
| **Spring Data JPA** | Lo que hace que los repositorios funcionen sin implementación. |
| **ORM** | Object-Relational Mapping: trabajar con objetos en vez de SQL. |
| **Jackson** | Librería que convierte JSON ↔ objetos Java. |
| **Lombok** | Librería que genera getters/setters/constructores al compilar. |
| **Maven** | Herramienta que descarga librerías, compila, testea y corre el proyecto. |
| **Clave primaria (PK)** | Columna que identifica cada fila (`id`). |
| **Clave foránea (FK)** | Columna que apunta a la PK de otra tabla. |
| **CASCADE** | Borrar el padre borra los hijos. |
| **Hash** | Huella de un texto que no se puede revertir. |
| **Salt** | Valor aleatorio que se mezcla con la contraseña antes de hashear. |
| **Token** | Texto aleatorio que identifica una sesión de login. |
| **Interceptor** | Código que corre antes de llegar al controller. |
| **Transacción** | Grupo de operaciones que se confirman o se deshacen todas juntas. |
| **Mock** | Objeto falso usado en los tests. |
| **H2** | Base de datos en memoria para los tests. |
| **Enum** | Tipo con un conjunto cerrado de valores. |
| **Record** | Clase corta e inmutable solo para datos. |
| **Interfaz** | Contrato que dice qué métodos tiene que tener una clase. |
| **Environment (Postman)** | Conjunto de variables (`base_url`, tokens, ids). |
| **Pre-request Script** | JavaScript que Postman corre antes de mandar un request. |
| **Collection v2.1** | Formato de exportación de colecciones de Postman que pide la cátedra. |
