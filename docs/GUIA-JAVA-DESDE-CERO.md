# Java y Spring Boot desde cero — para entender `mgw-prod` sin haber programado en Java

> Este documento no explica la lógica de negocio del proyecto (para eso está
> `docs/GUIA-TECNICA-EQUIPO.md`). Este explica **el lenguaje y las herramientas**: qué es
> cada cosa que aparece en el código, aunque nunca hayas escrito una línea de Java. La idea
> es que puedas leer cualquier archivo de `src/main/java` y entender qué hace cada símbolo,
> cada `@algo`, y por qué está ahí.
>
> No hace falta leerlo de punta a punta de una sentada. Podés usarlo como diccionario:
> buscá la anotación o la palabra que no entendés y andá directo a esa sección.

## Índice

1. [Qué es Java y cómo corre](#1-qué-es-java-y-cómo-corre)
2. [Maven y el `pom.xml`](#2-maven-y-el-pomxml)
3. [La estructura de carpetas del proyecto](#3-la-estructura-de-carpetas-del-proyecto)
4. [Conceptos base del lenguaje](#4-conceptos-base-del-lenguaje)
5. [Qué es una anotación (`@algo`)](#5-qué-es-una-anotación-algo)
6. [Las librerías que usamos, una por una](#6-las-librerías-que-usamos-una-por-una)
7. [Cómo arranca la aplicación](#7-cómo-arranca-la-aplicación)
8. [Inyección de dependencias (por qué todo recibe cosas en el constructor)](#8-inyección-de-dependencias)
9. [Cada anotación del proyecto, explicada una por una](#9-cada-anotación-del-proyecto-explicada-una-por-una)
10. [HTTP y REST desde cero](#10-http-y-rest-desde-cero)
11. [Bases de datos relacionales desde cero](#11-bases-de-datos-relacionales-desde-cero)
12. [Una request completa, de punta a punta](#12-una-request-completa-de-punta-a-punta)
13. [Glosario rápido](#13-glosario-rápido)

---

## 1. Qué es Java y cómo corre

Java es un lenguaje de programación. Un archivo `.java` es texto plano con código, como
`BeatController.java`. Pero la computadora no entiende ese texto directamente: hace falta
un paso intermedio.

1. **Compilar**: un programa (el compilador de Java, `javac`) lee el `.java` y lo convierte
   en un archivo `.class`, que contiene "bytecode" — instrucciones en un formato intermedio,
   no el texto original ni código de máquina directo.
2. **Ejecutar**: la **JVM** (Java Virtual Machine, "máquina virtual de Java") lee ese
   bytecode y lo ejecuta. La JVM es lo que realmente corre en tu computadora o en el
   servidor; es la que traduce el bytecode a instrucciones que el procesador entiende.

¿Por qué este paso extra? Porque el mismo `.class` corre igual en Windows, Mac o Linux —
alcanza con tener instalada una JVM para ese sistema operativo. Es la promesa histórica de
Java: "escribís una vez, corre en cualquier lado".

En este proyecto usamos **Java 21** (definido en `pom.xml`, `<java.version>21</java.version>`).
No necesitás compilar nada a mano: Maven (siguiente sección) lo hace por vos.

## 2. Maven y el `pom.xml`

Ningún proyecto real escribe todo desde cero. Spring Boot, la conexión a MySQL, las
herramientas de test — todo eso ya lo escribió otra gente y lo publicó como **librerías**
(también llamadas "dependencias": el proyecto *depende* de código externo para funcionar).

**Maven** es la herramienta que:
- Descarga esas librerías de internet (de un repositorio central).
- Compila tu código.
- Corre los tests.
- Empaqueta todo en un `.jar` ejecutable.

Todo eso se configura en un solo archivo: `pom.xml`, en la raíz del proyecto. Ahí están
declaradas las dependencias que usamos:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Esto le dice a Maven: "descargá el paquete `spring-boot-starter-webmvc` y ponelo disponible
para que el código lo pueda usar". No hace falta bajar ningún `.zip` a mano — Maven se
encarga solo.

En vez de tener Maven instalado en tu máquina, el proyecto trae **`mvnw`** (Maven Wrapper,
"envoltorio de Maven") — un script que ya sabe qué versión exacta de Maven usar y la baja
sola la primera vez. Por eso los comandos del proyecto son `./mvnw compile`, `./mvnw test`,
etc., y no simplemente `mvn`.

## 3. La estructura de carpetas del proyecto

```
mgw-prod/
├── pom.xml                          ← la config de Maven (sección 2)
├── src/
│   ├── main/
│   │   ├── java/com/mgwprod/...     ← todo el código de la aplicación
│   │   └── resources/
│   │       └── application.properties  ← configuración (puerto, base de datos, etc.)
│   └── test/
│       └── java/com/mgwprod/...     ← los tests, misma estructura que main
└── docs/                            ← documentación (este archivo incluido)
```

Dentro de `src/main/java`, cada carpeta es un **paquete** (`package`). Un paquete es como
una carpeta con nombre: agrupa clases relacionadas y evita que dos clases con el mismo
nombre choquen entre sí (podés tener un `Session` en `users` y, en teoría, otro `Session`
en otro paquete sin que se pisen).

La primera línea de casi todo archivo `.java` declara a qué paquete pertenece:

```java
package com.mgwprod.catalog.model;
```

Esto dice "esta clase vive en `com.mgwprod.catalog.model`", que en disco es literalmente
la carpeta `src/main/java/com/mgwprod/catalog/model/`. La convención (no es obligatorio,
pero todo el mundo la sigue) es usar el dominio de la empresa al revés como prefijo — por
eso `com.mgwprod`, no algo como `mgwprod.com`.

Cuando una clase necesita usar otra que vive en un paquete distinto, hay que **importarla**
al principio del archivo:

```java
import com.mgwprod.catalog.model.Beat;
```

Esto es literalmente "traé la clase `Beat` del paquete `catalog.model` para poder usarla
acá abajo sin escribir el nombre completo cada vez".

## 4. Conceptos base del lenguaje

Antes de meternos con Spring, hace falta tener claros algunos conceptos de Java puro que
aparecen en cada archivo del proyecto.

### Clase, objeto, atributo, método

Una **clase** es un molde. Define qué datos tiene algo y qué puede hacer. Por ejemplo,
`User` es el molde de "un usuario": dice que todo usuario tiene un `email`, una
`passwordHash`, un `role`, etc. (esos son los **atributos**, también llamados "campos" o
"fields"), y opcionalmente qué acciones puede ejecutar (los **métodos** — funciones
asociadas a la clase).

```java
public class User {
    private String email;
    private Role role;
    // ... más atributos
}
```

Un **objeto** es una instancia concreta de ese molde — un usuario real, con un email real.
Se crea con la palabra clave `new`:

```java
User user = new User();
user.setEmail("santi@ejemplo.com");
```

Acá `setEmail` es un método (en este caso generado automáticamente por Lombok, ver sección
6) que asigna un valor al atributo `email` de ese objeto puntual.

### Constructor

El **constructor** es un método especial que se ejecuta al crear el objeto con `new`. Sirve
para dejarlo en un estado inicial válido. Por ejemplo, en `SubscriptionService`:

```java
public SubscriptionService(SubscriptionRepository subscriptionRepository,
                            PaymentGateway paymentGateway,
                            UserRepository userRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.paymentGateway = paymentGateway;
    this.userRepository = userRepository;
}
```

El `this.subscriptionRepository = subscriptionRepository` dice "el atributo
`subscriptionRepository` de ESTE objeto (`this`) se llena con lo que me pasaron por
parámetro". Es el patrón que vas a ver en absolutamente todos los `Service` y `Controller`
del proyecto — es la base de la inyección de dependencias (sección 8).

### `public` / `private` / `protected`

Son modificadores de visibilidad — quién puede ver/usar algo:
- **`public`**: cualquiera, desde cualquier paquete.
- **`private`**: solo código dentro de la misma clase.
- **`protected`**: la misma clase, sus subclases, y clases del mismo paquete.

Por qué importa: en casi todas las entidades (`User`, `Beat`, etc.) los atributos son
`private` — nadie de afuera puede tocarlos directo — y se accede a ellos solo a través de
métodos públicos (`getEmail()`, `setEmail(...)`), generados por Lombok. Es una práctica
estándar llamada **encapsulamiento**: la clase controla cómo se lee/modifica su propio
estado, en vez de dejar que cualquiera meta la mano directo.

### `static`

Un miembro `static` (atributo o método) pertenece a la clase en sí, no a cada objeto
individual. Por ejemplo, en `AuthService`:

```java
private static final long SESSION_DURATION_HOURS = 24;
```

`SESSION_DURATION_HOURS` no cambia de un `AuthService` a otro — es una sola constante
compartida por todos. `final` además dice "una vez asignado, este valor no se puede volver
a cambiar" (es una constante). El `main` de la aplicación (sección 7) también es `static`,
porque se ejecuta antes de que exista ningún objeto todavía.

### Interfaces — el concepto más importante para entender la "magia" de Spring

Una **interfaz** (`interface`) es un contrato: declara qué métodos tiene que existir, pero
no dice cómo funcionan. Ejemplo real del proyecto, `PaymentGateway`:

```java
public interface PaymentGateway {
    PaymentResult charge(Long userId, BigDecimal amount);
}
```

Esto dice: "cualquier clase que implemente `PaymentGateway` tiene que tener un método
`charge` con esta firma". La implementación real la da otra clase:

```java
public class SimulatedPaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Long userId, BigDecimal amount) {
        return new PaymentResult(true, "SIMULATED-" + UUID.randomUUID());
    }
}
```

`implements` dice "esta clase cumple ese contrato". `@Override` (una anotación, ver sección
5 y 9) avisa "este método está pisando/completando uno que la interfaz exigía" — el
compilador chequea que realmente coincida, y si te equivocás en la firma te avisa al toque.

**Por qué esto importa tanto en este proyecto**: `SubscriptionService` depende de
`PaymentGateway` (la interfaz), nunca de `SimulatedPaymentGateway` (la implementación
concreta) directamente. El día de mañana se puede escribir una `MercadoPagoGateway` que
también implemente `PaymentGateway`, y `SubscriptionService` no se entera del cambio — sigue
llamando a `.charge(...)` igual.

Y ahí está la parte que más confunde al principio: **`UserRepository`, `BeatRepository`,
etc. también son interfaces, y jamás escribimos ninguna clase que las implemente**:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

¿Quién implementa esto entonces? **Spring, en el momento de arrancar la aplicación**, mira
la interfaz y genera automáticamente (en memoria, en tiempo de ejecución) una clase que la
cumple, traduciendo cada método a una consulta SQL. Nunca vas a ver esa clase generada en
ningún archivo — por eso parece magia la primera vez. Es la base de Spring Data JPA
(sección 6).

### `enum`

Un `enum` (enumeration, "enumeración") es un tipo con un conjunto fijo y cerrado de valores
posibles. En el proyecto:

```java
public enum Role {
    ARTIST,
    DISCOGRAFICA,
    ADMIN
}
```

Un `User.role` solo puede ser una de esas tres cosas — nunca un string libre como
`"artista"` o `"Artist"` mal tipeado. El compilador te lo garantiza. Se compara con `==`:
`user.getRole() == Role.ADMIN`.

### `record`

Un `record` es una forma corta de declarar una clase que **solo** guarda datos, sin lógica
propia ni forma de modificarlos después de creados (inmutable). En el proyecto:

```java
public record PaymentResult(boolean approved, String reference) {
}
```

Esa única línea le da automáticamente a `PaymentResult` un constructor, y métodos
`approved()` y `reference()` para leer esos dos valores (no se llaman `getApproved()`
como en el resto del proyecto porque los records tienen su propia convención de nombres).
Comparado con una clase común, ahorra escribir a mano el constructor y los getters cuando
lo único que necesitás es transportar un par de datos juntos.

### Generics (`<T>`)

Los símbolos `< >` que aparecen todo el tiempo (`List<Beat>`, `Optional<User>`,
`JpaRepository<Beat, Long>`) son **generics**: le dicen al compilador **de qué tipo** son
los datos que contiene algo, sin tener que escribir una clase distinta para cada tipo
posible.

- `List<Beat>` = "una lista, y cada elemento adentro es un `Beat`" (no cualquier cosa).
- `JpaRepository<Beat, Long>` = "un repositorio que trabaja con la entidad `Beat`, cuyo id
  (`@Id`) es de tipo `Long`".

Sin generics, tendrías que hacer *casting* manual (convertir un tipo genérico "Object" al
tipo real) en cada lugar donde usás la lista, con riesgo de romperse en tiempo de ejecución
si te equivocás. Con generics, el compilador lo chequea antes de que el programa corra.

### `Optional<T>`

Vas a ver `Optional<User>`, `Optional<Beat>`, etc. en casi todos los repositorios. Es una
cajita que puede **contener** un valor o estar **vacía**, en vez de usar directamente
`null` (la ausencia de valor "cruda" de Java, que si no se chequea explota con un
`NullPointerException`). Ejemplo real:

```java
Optional<User> maybeUser = userRepository.findByEmail(email);
User user = maybeUser.orElse(null);
```

`findByEmail` puede no encontrar a nadie con ese email — en vez de devolver `null` a lo
loco (fácil de olvidarse de chequear), devuelve un `Optional` que **obliga** a decidir qué
hacer si está vacío. En este proyecto, la convención elegida es "si está vacío, seguí
usando `null` de ahí en más" (`.orElse(null)`), porque es lo que después permite a los
`Service` devolver `null` y a los `Controller` decidir el 404 a mano (ver
`GUIA-TECNICA-EQUIPO.md`). La alternativa que se usa en un par de lugares puntuales,
`.orElseThrow()`, dice "si está vacío, explotá acá mismo" — se usa solo cuando la ausencia
sería un error interno, no un caso esperado (por ejemplo, un usuario `ARTIST` sin su
`ArtistProfile`, que nunca debería pasar).

### Lambdas y streams (lo justo para entender `ChallengeResultService`)

En un par de archivos (sobre todo `ChallengeResultService` y `ChallengeScoringService`)
aparece una forma de programar distinta a los `if`/`for` de siempre: **streams** y
**lambdas**. Ejemplo real:

```java
Set<Long> verifiedProducerIds = artistProfileRepository.findByVerifiedTrue().stream()
        .map(ArtistProfile::getUser)
        .map(User::getId)
        .collect(Collectors.toSet());
```

Se lee de arriba hacia abajo como una cadena de transformaciones sobre una lista:
1. `findByVerifiedTrue()` trae una `List<ArtistProfile>`.
2. `.stream()` la convierte en un "flujo" sobre el que se pueden encadenar operaciones.
3. `.map(ArtistProfile::getUser)` transforma cada `ArtistProfile` en su `User` (llamando a
   `getUser()` sobre cada uno). `ArtistProfile::getUser` es una **method reference**
   ("referencia a método") — una forma corta de escribir
   `perfil -> perfil.getUser()`.
4. `.map(User::getId)` transforma cada `User` en su `id`.
5. `.collect(Collectors.toSet())` junta todos los resultados en un `Set<Long>` (una
   colección sin duplicados).

Es equivalente a escribir un `for` con una lista vacía e ir agregando adentro, pero más
compacto una vez que le agarrás la vuelta. Una **lambda** es una función corta, sin nombre,
que se pasa como si fuera un dato — por ejemplo en `Comparator.comparingDouble(submission ->
scoreFor(challenge, verifiedProducerIds, submission))`, la parte
`submission -> scoreFor(...)` es una lambda: "dado un `submission`, calculá esto".

## 5. Qué es una anotación (`@algo`)

Una **anotación** es una etiqueta que se le pega a una clase, un método, un atributo o un
parámetro. Empieza siempre con `@`. La idea clave, que suele confundir al principio:

**una anotación no es código que se ejecuta en el lugar donde está escrita.** Es
*metadata* — información extra sobre ese pedazo de código, que después **otra cosa** (el
compilador, o Spring en tiempo de ejecución) lee y usa para decidir cómo comportarse.

Pensalo como un post-it pegado a una caja: el post-it no mueve nada por sí solo, pero
alguien (el compilador, o Spring) lo lee antes de decidir qué hacer con esa caja.

Ejemplo concreto:

```java
@GetMapping("/{id}")
public ResponseEntity<Beat> getBeat(@PathVariable Long id) {
```

- `@GetMapping("/{id}")` le dice a Spring: "cuando llegue una request HTTP `GET` a una URL
  que matchee este patrón, ejecutá este método". Spring lee esa anotación al arrancar la
  aplicación y arma internamente una tabla de "qué método atiende qué URL".
- `@PathVariable` le dice a Spring: "el valor que venga en la parte `{id}` de la URL,
  metelo en este parámetro".

El método `getBeat` en sí mismo es código Java normal — el `@` de arriba es lo que hace que
Spring sepa que existe y cuándo llamarlo. Sin la anotación, ese método sería un método
cualquiera que nadie invoca nunca.

## 6. Las librerías que usamos, una por una

Todo lo que sigue son dependencias declaradas en `pom.xml` (o traídas automáticamente por
ellas).

### Spring Framework / Spring Boot

**Spring** es un framework (un esqueleto sobre el cual se construye la aplicación, con
reglas y piezas ya armadas) para aplicaciones Java. Resuelve, entre otras cosas, la
inyección de dependencias (sección 8) y la configuración de toda la app.

**Spring Boot** es una capa sobre Spring que elimina casi toda la configuración manual:
trae un servidor web embebido (no hace falta instalar Tomcat aparte), detecta solo qué
piezas necesitás según las dependencias del `pom.xml`, y las configura con valores
razonables por defecto. Es literalmente lo que hace que `./mvnw spring-boot:run` levante un
servidor HTTP funcionando en el puerto 8080 sin que hayamos escrito ni una línea de
configuración de servidor.

### Spring Web (`spring-boot-starter-webmvc`)

La parte de Spring que expone endpoints HTTP — recibe requests, las rutea al método
correcto según la URL y el verbo (`GET`/`POST`/etc.), y arma la respuesta. Todas las clases
`@RestController` de este proyecto usan esta pieza.

### Spring Data JPA (`spring-boot-starter-data-jpa`)

La parte de Spring que conecta el mundo de los objetos Java con una base de datos
relacional, sin escribir SQL a mano. Trae adentro **Hibernate** (ver más abajo) y agrega la
"magia" de las interfaces `Repository` que se explicó en la sección 4 (Spring genera la
implementación sola).

### Hibernate

Es el **ORM** (Object-Relational Mapper, "mapeador objeto-relacional") que hace el trabajo
sucio detrás de Spring Data JPA: toma una entidad Java (`@Entity`, ver sección 9) y genera
el `INSERT`/`UPDATE`/`SELECT` de SQL correspondiente, y viceversa — toma una fila de la base
y arma un objeto Java con esos datos. Nunca escribimos SQL manual en el código Java (con la
excepción del `docs/db/schema.sql`, que crea las tablas a mano — Hibernate no las crea
porque `spring.jpa.hibernate.ddl-auto=none`).

### Jakarta Persistence (JPA)

`jakarta.persistence.*` (de donde vienen `@Entity`, `@Id`, `@Column`, etc.) es la
**especificación** — el estándar que dice qué anotaciones tienen que existir y qué
significan. Hibernate es una **implementación** concreta de esa especificación (hay otras,
como EclipseLink, pero acá usamos Hibernate). Es la misma relación que interfaz/implementación
de la sección 4, a otra escala: JPA define el contrato, Hibernate lo cumple.

### Jackson

Es la librería que convierte objetos Java a JSON y viceversa. Cuando un `@RestController`
devuelve un `Beat`, Jackson es quien lo convierte a texto JSON antes de mandarlo por HTTP;
cuando llega un `@RequestBody`, Jackson es quien lee el JSON de la request y arma el objeto
Java. Viene incluida dentro de `spring-boot-starter-webmvc`, no aparece como dependencia
propia en el `pom.xml`. Las anotaciones `@JsonIgnore`/`@JsonProperty` (sección 9) son de
Jackson, no de Spring ni de JPA.

### Lombok

Una librería que **genera código automáticamente** durante la compilación, para no tener
que escribirlo a mano. Por ejemplo, en `Beat.java`:

```java
@Getter
@Setter
@NoArgsConstructor
public class Beat {
    private String title;
    // ...
}
```

Sin Lombok, tendrías que escribir vos mismo:

```java
public String getTitle() { return title; }
public void setTitle(String title) { this.title = title; }
public Beat() { }
```

...y así para cada atributo. Lombok lee las anotaciones y agrega ese código
automáticamente en el `.class` compilado (nunca lo vas a ver en el `.java`, porque no
modifica el archivo — lo genera "al vuelo" durante la compilación). Por eso en el `pom.xml`
tiene `<optional>true</optional>` y aparece configurado como
`annotationProcessorPath` — es una herramienta que participa **durante la compilación**,
no una librería que se use en tiempo de ejecución.

### MySQL Connector / H2

- **`mysql-connector-j`**: el *driver* — la pieza que sabe hablar el protocolo específico
  de MySQL para que Java se pueda conectar a una base MySQL real. Es lo que usa la
  aplicación cuando corre normalmente (`spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod`
  en `application.properties`).
- **`h2`**: una base de datos súper liviana que corre **en memoria** (no hace falta
  instalar nada, vive y muere con el proceso). Se usa **solo en los tests**
  (`@DataJpaTest`), para no depender de tener MySQL corriendo para poder testear.

### JUnit 5 + Mockito (para los tests)

- **JUnit 5** es el framework que define qué es un test en Java: la anotación `@Test`
  marca un método como "esto es un test, corré esto y fijate si explota o si los `assert`
  fallan".
- **Mockito** permite crear objetos "de mentira" (*mocks*) que imitan a una clase real sin
  ejecutar su código de verdad — por ejemplo, un `UserRepository` de mentira que "responde"
  lo que el test le dice que responda, sin tocar ninguna base de datos real. Así los tests
  de `Service` prueban la lógica propia sin depender de que la base esté funcionando.

## 7. Cómo arranca la aplicación

Todo programa Java necesita un punto de entrada: un método `main`. En este proyecto está
en `MgwProdApplication.java`:

```java
@SpringBootApplication
public class MgwProdApplication {
    public static void main(String[] args) {
        SpringApplication.run(MgwProdApplication.class, args);
    }
}
```

- `public static void main(String[] args)` es la firma exacta que la JVM busca para saber
  por dónde arrancar. `static` (sección 4) porque se ejecuta antes de que exista ningún
  objeto.
- `SpringApplication.run(...)` le dice a Spring "arrancá todo desde acá": levanta el
  servidor HTTP embebido, escanea el código en busca de clases anotadas
  (`@RestController`, `@Service`, `@Entity`, etc.) y arma todas las conexiones entre ellas
  (sección 8).
- `@SpringBootApplication` es en realidad un combo de tres anotaciones en una:
  - Habilita el auto-configurado de Spring Boot (decide solo, según las dependencias del
    `pom.xml`, qué piezas activar).
  - Habilita el escaneo de componentes (busca clases anotadas dentro del mismo paquete,
    `com.mgwprod`, y todos sus subpaquetes — por eso `users`, `catalog`, `collab`,
    `challenges` y `billing` se detectan solos sin configurarlos a mano en ningún lado).
  - Permite agregar configuración adicional (como `WebConfig`, ver sección 9).

## 8. Inyección de dependencias

Este es el patrón que se repite en **cada** `Service` y `Controller` del proyecto:

```java
@Service
public class BeatService {

    private final BeatRepository beatRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public BeatService(BeatRepository beatRepository, UserRepository userRepository,
                        SubscriptionService subscriptionService) {
        this.beatRepository = beatRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }
    // ...
}
```

`BeatService` necesita usar `BeatRepository`, `UserRepository` y `SubscriptionService` para
hacer su trabajo. En vez de que `BeatService` cree esos objetos él mismo (con `new
BeatRepository()`, etc.), los **recibe ya armados** por parámetro en su constructor. A esto
se le llama **inyección de dependencias**: algo externo (Spring) es quien decide cómo armar
cada objeto y se los "inyecta" (se los pasa) a quien los necesita.

¿Quién arma esos objetos entonces? Spring, al arrancar la aplicación. Cuando ve
`@Service` en `BeatService`, sabe que tiene que crear una instancia de esa clase. Mira su
constructor, ve que pide un `BeatRepository`, un `UserRepository` y un
`SubscriptionService`, y — como esas clases también están anotadas (`@Repository`
implícito en las interfaces `JpaRepository`, `@Service` en `SubscriptionService`) — Spring
ya tiene (o crea) una instancia de cada una, y se las pasa automáticamente. Todo este
mecanismo se conoce como **IoC** (*Inversion of Control*, "inversión de control"): en vez
de que tu código controle cuándo y cómo se crean sus dependencias, es el framework el que
lo controla y te las entrega ya resueltas.

**Por qué esto es útil, no solo una complicación extra**: gracias a esto, en los tests se
puede reemplazar cualquiera de esas dependencias por un mock (sección 6) sin tocar el
código de `BeatService` — el constructor solo pide "algo que cumpla la interfaz/tipo
`BeatRepository`", no le importa si es el real o uno de mentira armado por Mockito.

## 9. Cada anotación del proyecto, explicada una por una

### Anotaciones de Spring Web (controllers y endpoints)

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@RestController` | En la clase, ej. `BeatController` | Le dice a Spring "esta clase expone endpoints HTTP; todo lo que devuelvan sus métodos se convierte automáticamente a JSON con Jackson" (en vez de devolver una página HTML, como sería un `@Controller` a secas). |
| `@RequestMapping("/api/beats")` | En la clase | Define el prefijo de URL común a todos los métodos de esa clase — todos los endpoints de `BeatController` empiezan con `/api/beats`. |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | En cada método | Asocian ese método a un verbo HTTP específico (ver sección 10) y, opcionalmente, a una sub-URL. `@GetMapping("/{id}")` sobre `/api/beats` resuelve `GET /api/beats/42`. |
| `@PathVariable` | En un parámetro, ej. `Long id` | Toma el valor de esa parte de la URL (la que está entre `{}` en el mapping) y lo pasa como parámetro Java ya convertido al tipo que pediste (acá, de texto a `Long`). |
| `@RequestParam` | En un parámetro | Toma un valor de la **query string** de la URL (la parte después del `?`, ej. `?genre=trap&bpm=140`). `required = false` permite que ese parámetro no venga — si no viene, el valor queda en `null`. |
| `@RequestBody` | En un parámetro | Toma el **cuerpo** de la request HTTP (el JSON que mandó el cliente) y lo convierte, vía Jackson, en un objeto Java del tipo que pediste. |
| `@RequestAttribute` | En un parámetro, ej. `Long userId` | Lee un valor que quedó guardado en el objeto del request por algo que corrió antes del controller — en este proyecto, `SessionAuthInterceptor` (ver `GUIA-TECNICA-EQUIPO.md`) guarda ahí el `userId` de la sesión autenticada. `required = false` porque un request sin login no tiene ese atributo seteado. |

### Anotaciones de Spring "core" (componentes e inyección)

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@Service` | En la clase, ej. `BeatService` | Marca la clase como un "componente de lógica de negocio" para que Spring la detecte, cree una instancia, y la pueda inyectar donde se necesite (sección 8). Funcionalmente casi idéntica a `@Component`; se usa este nombre por convención cuando la clase contiene lógica de negocio. |
| `@Component` | En la clase, ej. `PasswordHasher`, `SimulatedPaymentGateway` | La anotación genérica: "Spring, gestioná vos esta clase". `@Service`, `@RestController` y `@Repository` son en el fondo variantes de `@Component` con un nombre más específico según el rol. |
| `@Configuration` | En la clase, ej. `WebConfig` | Marca una clase como fuente de configuración adicional para Spring — acá es donde se registra manualmente el `SessionAuthInterceptor` en el pipeline de requests. |
| `@Transactional` | En un método de `Service` | Envuelve todo el método en una **transacción** de base de datos: o se guardan todos los cambios que hace ese método, o ninguno (si algo falla a mitad de camino, se revierte todo — *rollback*). `readOnly = true` es una pista de optimización para cuando el método solo lee datos y no modifica nada (Hibernate puede saltearse chequeos que solo hacen falta al escribir). |

### Anotaciones de JPA / persistencia (`jakarta.persistence.*`)

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@Entity` | En la clase, ej. `Beat` | Marca la clase como mapeada a una tabla de la base de datos — cada objeto `Beat` corresponde (potencialmente) a una fila de la tabla `beats`. |
| `@Table(name = "beats")` | En la clase | Dice explícitamente el nombre de la tabla en la base. Si se omite, Hibernate usaría el nombre de la clase por defecto — se pone siempre a mano acá para que coincida exacto con `docs/db/schema.sql`. |
| `@Id` | En un atributo | Marca cuál atributo es la clave primaria (el identificador único de cada fila). |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Junto a `@Id` | Dice que el valor del id no lo elige la aplicación — lo genera la base de datos sola al insertar (un `AUTO_INCREMENT` en MySQL). |
| `@Column(...)` | En un atributo | Configura detalles de la columna correspondiente: `name` (si el nombre en la base es distinto al del atributo Java, ej. `producerId` → columna `producer_id`), `nullable = false` (la base rechaza un `INSERT`/`UPDATE` sin ese valor), `unique = true` (no puede haber dos filas con el mismo valor), `length` (tamaño máximo de un texto). |
| `@Enumerated(EnumType.STRING)` | En un atributo `enum` | Le dice a Hibernate que guarde el `enum` como texto (el nombre, ej. `"ARTIST"`) en vez de como un número. Sin esto, por defecto Hibernate guardaría la posición del valor dentro del enum (0, 1, 2...), lo cual es ilegible en la base y peligroso si alguna vez se reordenan los valores del enum. |
| `@Transient` | En un atributo | Le dice a Hibernate "este campo NO es una columna, no lo guardes ni lo leas de la base". Se usa en `User.password`: existe solo para transportar la contraseña en texto plano desde el JSON del registro hasta que `AuthService` la hashea — nunca debe llegar a la base tal cual. |
| `@PrePersist` | En un método propio de la entidad | Marca ese método para que Hibernate lo ejecute automáticamente justo antes de insertar la fila por primera vez. Se usa en todas las entidades para sellar `createdAt = Instant.now()` sin que el cliente tenga que mandarlo. |
| `@OneToOne`, `@OneToMany`, `@ManyToOne` | En un atributo que es otro objeto (o una lista de objetos) | Definen una **relación** entre dos entidades — ver la sección 11 para el concepto de relación en bases de datos. `@ManyToOne` (ej. `BeatComment.beat`) es "muchos comentarios pueden apuntar al mismo beat". `@OneToMany` es el lado inverso, la lista completa (ej. `Beat.comments`). `@OneToOne` es una relación de uno a uno (ej. `ArtistProfile.user`). |
| `@JoinColumn(name = "beat_id")` | Junto a `@ManyToOne`/`@OneToOne` | Dice el nombre exacto de la columna que guarda la clave foránea (ver sección 11) en la base. |
| `mappedBy = "beat"` | Dentro de `@OneToMany` | Le dice a Hibernate "este lado de la relación NO tiene su propia columna en la base — la columna real vive del otro lado, en el atributo llamado `beat` de la otra entidad". Evita que Hibernate intente crear una columna redundante. |

### Anotaciones de Jackson (conversión a/desde JSON)

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@JsonIgnore` | En un atributo | Le dice a Jackson "nunca incluyas este campo cuando conviertas el objeto a JSON, ni lo esperes cuando conviertas JSON al objeto". Se usa para no filtrar datos sensibles (ej. la `passwordHash` de un `User` dentro del `User` de una `ArtistProfile`) o para cortar un ciclo infinito de referencias entre dos entidades que se apuntan mutuamente. |
| `@JsonProperty(access = JsonProperty.Access.READ_ONLY)` | En un atributo | El campo SÍ aparece en las respuestas JSON, pero Jackson lo ignora si viene en el JSON de una request entrante. Se usa en campos que el servidor calcula solo (ej. `Beat.producerId`, que sale del usuario autenticado, no de lo que mande el cliente) — evita que alguien intente hacerse pasar por otro productor mandando ese campo a mano en el body. |
| `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` | En un atributo | El caso inverso: el campo SÍ se acepta si viene en una request, pero nunca se incluye en una respuesta. Se usa en `User.password` — se acepta al registrarse, pero jamás se devuelve en ningún JSON de salida. |

### Anotaciones de Lombok (generación de código)

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@Getter` / `@Setter` | En la clase | Genera automáticamente un método `getX()`/`setX(valor)` por cada atributo, sin que haya que escribirlos a mano. |
| `@NoArgsConstructor` | En la clase | Genera un constructor sin parámetros (`new Beat()`), dejando todos los atributos en su valor por defecto. Hibernate y Jackson lo necesitan para poder crear el objeto vacío y después llenarlo campo por campo con los setters. |
| `@AllArgsConstructor` | En la clase | Genera un constructor que recibe todos los atributos de una — se usa puntualmente donde hace falta crear el objeto completo en una sola línea (ej. en algunos tests). |

### Otras

| Anotación | Dónde se usa | Qué hace |
|---|---|---|
| `@Override` | En un método | No es de Spring ni de ninguna librería — es del lenguaje Java en sí. Avisa "este método está reemplazando/completando uno definido en una interfaz o clase padre" (ver la sección de interfaces). El compilador chequea que la firma coincida exactamente; si te tipeaste mal el nombre o los parámetros, te avisa en vez de dejarte crear sin querer un método nuevo que nadie llama. |

## 10. HTTP y REST desde cero

**HTTP** es el protocolo (el "idioma") que usan un cliente (Postman, un navegador, una app)
y un servidor para comunicarse por internet. Un **endpoint** es una combinación de
**verbo** (qué se quiere hacer) + **URL** (sobre qué recurso) que el servidor sabe atender.

Los verbos que usamos en este proyecto:

| Verbo | Para qué se usa acá |
|---|---|
| `GET` | Leer datos, sin modificar nada (ej. `GET /api/beats/42`). |
| `POST` | Crear algo nuevo (ej. `POST /api/beats` crea un beat). |
| `PUT` | Actualizar algo que ya existe (ej. `PUT /api/beats/42`). |
| `DELETE` | Borrar algo (ej. `DELETE /api/beats/42`). |

**REST** es simplemente un estilo/convención para diseñar APIs HTTP: las URLs representan
**recursos** (sustantivos: `/api/beats`, `/api/challenges/{id}/submissions`) y el verbo
HTTP dice la acción — en vez de, por ejemplo, tener una URL `/crearBeat` y otra
`/borrarBeat`.

Cada respuesta HTTP incluye un **status code** (código de estado): un número que resume
qué pasó. Los que usa este proyecto (y por qué, ver la lógica exacta en
`GUIA-TECNICA-EQUIPO.md`):

| Código | Nombre | Significa, en este proyecto |
|---|---|---|
| `200 OK` | Éxito | La operación (normalmente `GET`/`PUT`) salió bien. |
| `201 Created` | Creado | Un `POST` creó un recurso nuevo con éxito. |
| `204 No Content` | Sin contenido | La operación (normalmente `DELETE`) salió bien y no hay nada que devolver en el body. |
| `400 Bad Request` | Pedido inválido | Faltó un campo obligatorio o vino con un valor inválido — error del que llama. |
| `401 Unauthorized` | Sin autenticar | No mandaste un token de sesión válido (`Authorization: Bearer ...`). |
| `403 Forbidden` | Prohibido | Estás autenticado, pero no tenés permiso para esa acción puntual (ej. no sos el dueño, o tu rol no alcanza). |
| `404 Not Found` | No encontrado | El recurso pedido (por id) no existe. |
| `409 Conflict` | Conflicto | La acción choca con el estado actual de los datos (ej. borrar un usuario que todavía tiene contenido asociado, o votar dos veces lo mismo). |
| `500 Internal Server Error` | Error del servidor | Algo se rompió del lado del servidor que no debería haber pasado — un bug, no un error del cliente. |

En el código, `ResponseEntity<T>` es la clase de Spring que envuelve "el status code +
el body" de una respuesta HTTP. `ResponseEntity.ok(beat)` arma un 200 con `beat` como body;
`ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)` arma un 404 sin body.

## 11. Bases de datos relacionales desde cero

Una base de datos **relacional** (como MySQL, que usa este proyecto) guarda los datos en
**tablas**: una grilla de filas y columnas, muy parecida a una hoja de cálculo. Cada
**tabla** representa un tipo de cosa (`users`, `beats`, `challenges`...); cada **fila** es
un registro concreto; cada **columna** es un dato de ese registro.

Cada fila tiene una **clave primaria** (*primary key*, `@Id` en la entidad Java): un valor
que identifica a esa fila de manera única dentro de su tabla — en este proyecto, siempre un
número autoincremental (`id`).

Una **clave foránea** (*foreign key*, `@JoinColumn` en la entidad Java) es una columna que
guarda el id de una fila de **otra** tabla, para "apuntar" a ella. Por ejemplo, la columna
`beat_id` de la tabla `beat_comments` guarda el `id` de una fila de la tabla `beats` — así
la base sabe que ese comentario pertenece a ese beat puntual.

Cuando querés traer datos combinando información de dos tablas relacionadas (ej. "traeme el
comentario junto con el título del beat al que pertenece"), en SQL puro se hace con un
**JOIN**. En este proyecto nunca escribimos ese SQL a mano — es Hibernate quien lo genera
automáticamente cuando accedés a una relación desde Java (ej. `beatComment.getBeat().getTitle()`
dispara, detrás de escena, el `SELECT ... JOIN ...` necesario).

Los tres tipos de relación que aparecen en el proyecto:

- **Uno a uno** (`@OneToOne`): cada `User` artista tiene, como mucho, un `ArtistProfile`, y
  viceversa. Ejemplo: `ArtistProfile.user`.
- **Muchos a uno** (`@ManyToOne`): muchas filas de una tabla apuntan a la misma fila de
  otra. Ejemplo: `BeatComment.beat` — muchos comentarios pueden pertenecer al mismo beat.
- **Uno a muchos** (`@OneToMany`): el lado inverso del anterior, visto desde "el uno": un
  `Beat` tiene una lista de `comments`. En la base de datos **no hay ninguna columna nueva**
  para esto — es la misma columna `beat_id` de la tabla `beat_comments`, solo que mirada
  "desde el otro lado". Por eso lleva `mappedBy`.

## 12. Una request completa, de punta a punta

Juntando todo lo anterior, así es el recorrido completo de, por ejemplo,
`GET /api/beats/42`:

1. El cliente (Postman) manda una request HTTP `GET` a `http://localhost:8080/api/beats/42`.
2. La JVM ya tiene corriendo el servidor embebido de Spring Boot (arrancado por
   `MgwProdApplication.main`, sección 7), escuchando en el puerto 8080
   (`server.port=8080` en `application.properties`).
3. Como la URL empieza con `/api/` y no es `/api/auth/**`, pasa primero por
   `SessionAuthInterceptor` (registrado en `WebConfig`, un `@Configuration`) — como es un
   `GET` sin necesidad de login, si no hay token simplemente deja pasar la request.
4. Spring Web mira sus anotaciones (`@RestController` + `@RequestMapping("/api/beats")` +
   `@GetMapping("/{id}")`) y encuentra que `BeatController.getBeat` es el método que
   atiende esta URL. Convierte el `42` de la URL en un `Long id` gracias a `@PathVariable`.
5. `BeatController.getBeat(id)` llama a `beatService.getById(id)`. `beatService` es la
   instancia de `BeatService` que Spring ya armó e inyectó en el constructor de
   `BeatController` (sección 8).
6. `BeatService.getById` llama a `beatRepository.findById(id)`. `beatRepository` es la
   implementación que Spring generó solo a partir de la interfaz `BeatRepository`
   (sección 4).
7. Esa llamada dispara, detrás de escena, un `SELECT * FROM beats WHERE id = 42` sobre
   MySQL, ejecutado por Hibernate a través del driver `mysql-connector-j`.
8. Si existe la fila, Hibernate arma un objeto `Beat` en memoria con esos datos y lo
   envuelve en un `Optional<Beat>`; el `.orElse(null)` lo deja en `null` si no existía.
9. De vuelta en `BeatController`, si `beat == null` arma un `ResponseEntity` con status
   `404`; si no, con status `200` y el `beat` como body.
10. Spring Web le pasa ese `Beat` a Jackson, que lo convierte a JSON (respetando
    `@JsonIgnore`/`@JsonProperty` si los hubiera) y arma la respuesta HTTP final que
    recibe Postman.

Todo esto — servidor HTTP, ruteo, inyección de dependencias, conexión a la base,
conversión a JSON — corre sin que hayamos escrito ni una línea de esa infraestructura:
es exactamente lo que compran las dependencias del `pom.xml` y lo que activan las
anotaciones de las secciones 9 y 10.

## 13. Glosario rápido

- **JVM**: el programa que ejecuta el bytecode de Java.
- **Maven**: la herramienta que descarga librerías, compila y empaqueta el proyecto.
- **Dependencia**: una librería externa que el proyecto usa.
- **Framework**: un esqueleto de código ya armado (acá, Spring) sobre el que se construye
  la aplicación siguiendo sus reglas.
- **Anotación (`@algo`)**: una etiqueta de metadata que el compilador o un framework lee
  para decidir cómo tratar la clase/método/campo al que está pegada.
- **Clase / objeto**: la clase es el molde, el objeto es una instancia concreta creada con `new`.
- **Interfaz**: un contrato de métodos, sin implementación propia.
- **Inyección de dependencias**: recibir ya armadas, por constructor, las cosas que una
  clase necesita, en vez de crearlas ella misma.
- **ORM**: la pieza (acá, Hibernate) que traduce objetos Java a filas de una tabla y viceversa.
- **Entidad**: una clase Java (`@Entity`) mapeada a una tabla de la base.
- **Endpoint**: una combinación verbo HTTP + URL que el servidor sabe atender.
- **Status code**: el número que resume qué pasó con una request HTTP (200, 404, etc.).
- **Clave primaria / foránea**: el id único de una fila / una columna que apunta al id de
  una fila de otra tabla.
- **Mock**: un objeto de mentira, usado en tests, que imita a uno real sin ejecutar su
  lógica de verdad.
