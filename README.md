# mgw-prod

Music Discovery & Challenge Platform — TPO de Aplicaciones Interactivas (UADE).
Grupo: Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi.

> **¿Por qué "Music Discovery" y no e-commerce?** La consigna original de la cátedra
> (`docs/Trabajo Integrador de Aplicaciones Interactivas.pdf`) pide una app con
> "funcionalidades de e-commerce", pero el profesor confirmó en clase que **para esta entrega
> no hace falta** — no hay carrito, checkout ni pagos en ningún lado del alcance actual. El
> dominio elegido es descubrimiento y colaboración musical (perfiles, catálogo de beats,
> colaboración, challenges con jurado). El e-commerce/marketplace transaccional es una idea de
> producto para mucho más adelante, fuera del alcance de esta materia — no hay que construirlo
> ahora. Detalle completo en `CLAUDE.md`.

## Requisitos

- JDK 21 o superior (si tenés varias versiones instaladas, verificá que `JAVA_HOME` apunte a una ≥21 — un JDK más viejo falla con "release 21 not supported").
- MySQL corriendo en `localhost:3306`, con un usuario `root` y password `admin` (misma convención que `GestorDeInventario` de Clase 2).
- No hace falta tener Maven instalado — el proyecto trae el wrapper (`./mvnw`).

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

## Convención: el esquema es manual, no lo genera Hibernate

`spring.jpa.hibernate.ddl-auto=none` (en `application.properties`, decisión de Clase 4) — Hibernate **no** crea ni modifica tablas. Todas las tablas viven a mano en `docs/db/schema.sql`. Esto tiene dos consecuencias directas para cualquiera que agregue una entidad nueva:

- Si agregás un `@Entity`, tenés que sumar su `CREATE TABLE` correspondiente en `docs/db/schema.sql` — Spring no lo va a hacer solo.
- Si cambiás la forma de una entidad existente, actualizá el `.sql` y recreá la base local:

  ```bash
  mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
  mysql -u root -padmin mgw_prod < docs/db/schema.sql
  ```

## Estado del proyecto y qué falta por módulo

Arquitectura: un solo Spring Boot, cuatro paquetes verticales (uno por integrante) — `com.mgwprod.<modulo>`, cada uno con su propio `controller/model/repository/service`. Por qué está organizado así (y no plano como el ejemplo de Clase 4) está explicado en `CLAUDE.md` → "Por qué la estructura de paquetes no es igual al ejemplo de Clase 4".

| Módulo | Dueño | Estado | Plan |
|---|---|---|---|
| `users` | Santiago | ✅ Implementado en `main` (auth, roles, perfiles) | — |
| `catalog` | Santiago + Mateo | 🔲 Pendiente — plan listo (5 tareas) | `docs/superpowers/plans/2026-09-01-mgw-prod-catalog-module.md` |
| `collab` | Dani | 🔲 Pendiente — plan listo (5 tareas, depende de `catalog`) | `docs/superpowers/plans/2026-09-01-mgw-prod-collab-module.md` |
| `challenges` | Paolo | 🔲 Pendiente — plan listo (9 tareas) | `docs/superpowers/plans/2026-09-01-mgw-prod-challenges-module.md` |

Cada plan tiene el desglose de tareas en formato TDD (test primero, después implementación) y los detalles de las entidades de ese módulo. Arrancá desde una branch nueva (`feature/<modulo>-module`), nunca directo sobre `main`.

## Troubleshooting

Si en Windows o con una versión distinta de MySQL aparece un error de zona horaria o de "public key retrieval", agregá parámetros a la URL en `application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
```

## Stack y diseño

- `CLAUDE.md` — contexto completo del proyecto, stack, convenciones y decisiones de arquitectura.
- `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` — diseño vigente (post-pivot, dominio actual).
- `docs/descripcion.md` — descripción completa del producto (visión, incluida la parte fuera de alcance de esta entrega).
- `docs/superpowers/specs/2026-08-25-mgw-prod-tpo-design.md` — diseño histórico (e-commerce), solo como referencia, ya no vigente.
