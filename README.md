# mgw-prod

Music Discovery & Challenge Platform — TPO de Aplicaciones Interactivas (UADE).
Grupo: Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi.

> **¿Por qué "Music Discovery" y no e-commerce?** La consigna original de la cátedra
> (`docs/Trabajo Integrador de Aplicaciones Interactivas.pdf`) pide una app con
> "funcionalidades de e-commerce", pero el profesor confirmó en clase que **para esta entrega
> no hace falta** — no hay carrito, checkout ni pagos reales en ningún lado del alcance actual.
> El dominio elegido es descubrimiento y colaboración musical: perfiles de artistas/discográficas,
> catálogo de beats, colaboración (toplines sobre beats), y challenges semanales con jurado
> ponderado. El e-commerce/marketplace transaccional es una idea de producto para mucho más
> adelante, fuera del alcance de esta materia. Diseño completo del pivot en
> `docs/design/specs/2026-09-01-mgw-prod-pivot-design.md`.

## Requisitos

- JDK 21 o superior (si tenés varias versiones instaladas, verificá que `JAVA_HOME` apunte a una ≥21 — un JDK más viejo falla con "release 21 not supported").
- MySQL corriendo en `localhost:3306`, con un usuario `root` y password `admin` (misma convención que `GestorDeInventario` de Clase 2).
- No hace falta tener Maven instalado — el proyecto trae el wrapper (`./mvnw`).
- Para probar los endpoints: [Postman](https://www.postman.com/downloads/) (o cualquier cliente HTTP — hay una colección lista para importar, ver más abajo).

## Setup inicial

1. Crear la base de datos local y cargar el esquema (el esquema es manual, no auto-generado — ver nota abajo):

   ```bash
   mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS mgw_prod;"
   mysql -u root -padmin mgw_prod < docs/db/schema.sql
   ```

2. Levantar la app:

   ```bash
   ./mvnw spring-boot:run
   ```

   Por defecto corre en `http://localhost:8080`.

3. Correr los tests:

   ```bash
   ./mvnw test
   ```

   Debería dar **164 tests, 0 failures**.

## Cómo probar el backend en vivo (Postman)

El requisito de la Etapa 1 es demostrar el CRUD completo con solicitudes HTTP reales, sin
correr ningún `main()` de prueba. Para eso está `docs/api/mgw-prod.postman_collection.json`
(Collection v2.1) + `docs/api/mgw-prod.postman_environment.json` (Environment):

1. Abrir Postman → **Import** → arrastrar ambos archivos (`.postman_collection.json` y
   `.postman_environment.json`).
2. Seleccionar el Environment **"mgw-prod local"** arriba a la derecha.
3. Con el servidor corriendo (`./mvnw spring-boot:run`), correr la Collection completa:
   click derecho sobre **mgw-prod** → **Run collection** → Run.
4. Debería terminar con **~47 requests, 0 errores** (status 200/201/204 en todos).

La Collection está armada para auto-sembrarse: registra y loguea un ARTIST, una DISCOGRAFICA
y un ADMIN de prueba al principio, y encadena los IDs/tokens que va generando (beat, topline,
challenge, submission, etc.) request por request — no hace falta pegar nada a mano. Cubre los
5 módulos con al menos un GET-lista, GET-por-id, POST, PUT y DELETE cada uno.

## Arquitectura

Un solo proyecto Spring Boot, organizado en **cuatro paquetes verticales** (uno por
integrante) más un quinto agregado después (billing) — cada uno con su propio
`controller/model/repository/service`:

| Módulo | Dueño | Qué hace |
|---|---|---|
| `com.mgwprod.users` | Santiago | Auth (registro/login por sesión), roles (ARTIST/DISCOGRAFICA/ADMIN), perfiles de artista, verificación |
| `com.mgwprod.catalog` | Santiago + Mateo | Publicar/listar beats, comentarios sobre beats |
| `com.mgwprod.collab` | Dani | Toplines de artistas sobre beats, comentarios, colaboraciones (aceptar/rechazar) |
| `com.mgwprod.challenges` | Paolo | Challenges con jurado ponderado, submissions, votos, resultados, ranking |
| `com.mgwprod.billing` | Dani | Suscripción free/premium, límite de producciones del plan free, pago simulado |

Documentación de endpoints por módulo en `docs/api/*-API.md`.

### Por qué la estructura de paquetes no es plana como el ejemplo de Clase 4

El ejemplo de la cátedra (`Backend con Spring.pdf`, proyecto `webcampus`) organiza el código
**por capa, plano en la raíz**: `controller/`, `model/`, `repository/`, `service/`, todo junto.

Acá organizamos **por módulo/dueño primero, y recién adentro por capa** (`users/controller/`,
`catalog/controller/`, etc.). Es deliberado: somos 4 integrantes y la evaluación es
**individual** aunque el trabajo sea grupal — cada uno necesita su paquete autocontenido para
poder explicarlo y defenderlo sin pisar el código de los demás en la misma carpeta
`controller/`. Todo lo demás calca el patrón de Clase 4: capas Controller/Model/Repository/
Service, JPA `Repository` sin DAO manual, sin DTOs (el controller recibe/devuelve la entidad
JPA directo), Lombok en las entidades, inyección por constructor en el Service.

### Convención: el esquema es manual, no lo genera Hibernate

`spring.jpa.hibernate.ddl-auto=none` (decisión de Clase 4) — Hibernate **no** crea ni modifica
tablas. Todas viven a mano en `docs/db/schema.sql`. Consecuencias para quien agregue una
entidad nueva:

- Si agregás un `@Entity`, sumá su `CREATE TABLE` en `docs/db/schema.sql` — Spring no lo hace solo.
- Si cambiás la forma de una entidad existente, actualizá el `.sql` y recreá la base local:

  ```bash
  mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
  mysql -u root -padmin mgw_prod < docs/db/schema.sql
  ```

### Sin DTOs

Los controllers reciben/devuelven la entidad JPA directo (patrón de Clase 4, ver
`docs/design/plans/2026-08-29-remove-users-dtos.md`). Consecuencia importante: como
`spring.jpa.properties.jakarta.persistence.validation.mode=none` está seteado a nivel app
entero, las anotaciones de Bean Validation (`@NotBlank`, `@Min`, etc.) puestas en una entidad
**no se validan solas al guardar** — hay que validar siempre en el controller con
`@Valid @RequestBody`, salvo en los métodos de actualización parcial (`PUT` que solo cambian
los campos enviados), donde se valida campo por campo en el Service para no rechazar los
campos omitidos (ver `InvalidFieldException`).

## Limitaciones conocidas

Dos condiciones de carrera identificadas y documentadas, no corregidas todavía porque la
solución correcta (locking pesimista o un `UPDATE` atómico en la base) es una técnica de
Spring Data JPA que no vimos en clase — se van a resolver una vez confirmado con la cátedra:

- **`SubscriptionService.recordProduction`**: si un mismo artista dispara dos publicaciones
  casi al mismo tiempo (doble click, dos pestañas) justo al llegar al límite de 50 del plan
  free, ambas pueden pasar la validación antes de que se guarde el contador — dejando pasar
  una publicación de más.
- **`SubscriptionService.getOrCreate`**: si un usuario nuevo dispara dos pedidos casi
  simultáneos que necesitan su suscripción por primera vez, el segundo puede fallar con un
  error 500 en vez de simplemente devolver la suscripción que el primero ya creó.

Ninguna de las dos corrompe datos ni afecta el flujo normal de uso/demo.

## Stack

- **Backend:** Java 21+, Spring Boot 4.1, Spring Data JPA, MySQL, Lombok, JUnit 5 + Mockito + MockMvc.
- **Auth:** casera (password hasheada + token de sesión en tabla propia), sin Spring Security.
- **Pagos:** interfaz `PaymentGateway` con una implementación simulada
  (`SimulatedPaymentGateway`, siempre aprueba) — pensada para poder enchufar más adelante un
  gateway real (ej. Mercado Pago) sin tocar `SubscriptionService`.

## Documentación

- `docs/GUIA-TECNICA-EQUIPO.md` — guía técnica por módulo (arquitectura, lógica de negocio,
  decisiones de diseño) para que cada integrante pueda explicar y defender el proyecto.
- `docs/INFORME-TPO.md` — informe formal de la entrega, con la validación funcional
  end-to-end vía Postman.
- `docs/design/specs/2026-09-01-mgw-prod-pivot-design.md` — diseño vigente (post-pivot, dominio actual).
- `docs/design/specs/2026-09-04-mgw-prod-professor-corrections-design.md` — diseño de las correcciones del profesor (roles, CRUD completo, billing).
- `docs/design/plans/*.md` — planes de implementación por módulo (desglose de tareas, decisiones puntuales).
- `docs/descripcion.md` — descripción completa del producto (visión, incluida la parte fuera de alcance de esta entrega).
- `docs/api/*-API.md` — referencia de endpoints por módulo.
- `docs/design/specs/2026-08-25-mgw-prod-tpo-design.md` — diseño histórico (e-commerce), solo como referencia, ya no vigente.

## Troubleshooting

Si en Windows o con una versión distinta de MySQL aparece un error de zona horaria o de
"public key retrieval", agregá parámetros a la URL en `application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
```
