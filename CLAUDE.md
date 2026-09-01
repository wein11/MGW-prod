# mgw-prod — Music Discovery & Challenge Platform (TPO Aplicaciones Interactivas)

Hereda el contexto global de `../../CLAUDE.md` y `~/.claude/CLAUDE.md`.

## Qué es este proyecto

Trabajo Práctico Obligatorio de la materia **Aplicaciones Interactivas (UADE)**. Grupo de 4:
**Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi**. La evaluación es
individual aunque el trabajo sea grupal — cada integrante debe poder explicar y defender su
módulo.

Split de módulos (4, uno por integrante) — ver "Arquitectura" abajo. Historia del split: nació
como `marketplace`, se partió en `catalog` + `orders` el 2026-08-27 para darle a Dani un módulo
propio; con el pivot del 2026-09-01 `orders` (carrito/checkout) se eliminó y se reemplazó por
`collab` (toplines, comentarios, colaboraciones) como módulo de Dani.

Es la primera versión (académica) de una idea de producto más grande: una plataforma de
descubrimiento y colaboración musical para productores/artistas emergentes, con challenges
semanales, rankings y reputación ("Music Score"). Descripción completa del producto en
`docs/descripcion.md`.

**Pivot 2026-09-01:** el profesor confirmó que el TPO ya no tiene que ser e-commerce
transaccional. El proyecto volvió al pitch original (perfil-portfolio, publicaciones tipo red
social, colaboración, desafíos con jurado ponderado) en vez de mantener el recorte de
"marketplace de beats con carrito y checkout". Diseño vigente en
`docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md`. Fuera de alcance de esta entrega:
Talent Discovery para sellos (rol `LABEL`, búsqueda paga) — queda como visión de producto.

## Regla dura: solo lo visto en clase

Este proyecto es para una materia de la facultad. **No se inventa contenido que no se haya
visto en clase.** Antes de usar un patrón/librería/anotación de Spring que no aparezca en
`../../facultad/aplicaciones-interactivas/clases/`, confirmar que ya se cubrió o preguntar a
Santiago. Ver ese directorio para el estado real de avance de la cursada.

## Consigna

El TPO completo está en `docs/Trabajo Integrador de Aplicaciones Interactivas.pdf` (copiar ahí
si no está). Dos etapas obligatorias:
- **Etapa 1** — backend + persistencia, probado con Postman/curl, corre en localhost.
- **Etapa 2** — frontend HTML5/CSS3/JS plano (sin frameworks) integrado con el backend.

## Stack

- **Backend:** Java + Spring Boot + Spring Data JPA + MySQL (localhost:3306, mismo motor que
  ya usan en Clase 2 con JDBC).
- **Frontend:** HTML5 + CSS3 + JavaScript plano (sin React/Vue/etc. — la consigna lo prohíbe
  para Etapa 2), servido como estático desde `src/main/resources/static` del mismo proyecto
  Spring Boot (sin CORS, un solo puerto).
- **Auth:** casera simple (password hasheada + token de sesión en tabla propia). Nada de
  Spring Security todavía — se suma solo si la cátedra lo enseña más adelante.
- **Persistencia vía Spring Data JPA/`Repository`, nunca DAO manual** — confirmado por Clase 4
  (2026-08-27, "Arquitectura Spring"): la cátedra dice explícitamente que ya no se usa el
  patrón DAO. Nuestra elección de JPA desde el diseño inicial (25/08) resultó ser la correcta.
- **Esquema manual, no auto-DDL** — `spring.jpa.hibernate.ddl-auto=none` (cambiado el 27/08
  desde `update`, para coincidir con lo enseñado en Clase 4). Las tablas se crean a mano vía
  `docs/db/schema.sql`: cada módulo que agrega una entidad nueva suma ahí su `CREATE TABLE`.
- **Sin DTOs — los controllers reciben/devuelven la entidad JPA directo**, para calcar el
  patrón de Clase 4 (ver `docs/superpowers/plans/2026-08-29-remove-users-dtos.md`; aplicado
  primero en `users`, mergeado a `main` el 2026-09-01 — PR #1). Convención obligatoria también
  para `catalog`/`collab`/`challenges`. Consecuencia importante para cualquier entidad nueva:
  `spring.jpa.properties.jakarta.persistence.validation.mode=none` está seteado a nivel app
  entero, así que las anotaciones
  de Bean Validation (`@NotBlank`, `@Size`, etc.) puestas en una entidad **no se validan solas
  al guardar** — hay que validar siempre en el controller con `@Valid @RequestBody`, nunca
  confiar en que Hibernate lo haga en el `save()`.
- Este proyecto **no sigue el stack default del hub** (`FastAPI/React/Postgres`) porque el
  stack viene impuesto por la cátedra.

## Arquitectura

Monolito Spring Boot, un solo `pom.xml`, cuatro paquetes verticales — cada uno dueño de un
integrante, cada uno con su propio Controller/Service/Repository/Entity:

- `com.mgwprod.users` — perfiles, roles (PRODUCER/ARTIST), auth, verificación de productores.
  Dueño: Santiago (ya en curso, incluye el refactor sin DTOs mergeado el 2026-09-01).
- `com.mgwprod.catalog` — publicar/listar beats, comentarios sobre beats. Dueños: Santiago +
  Mateo (Mateo no tiene Claude Code, trabaja en pareo con Santiago). El más liviano de los tres
  módulos nuevos (5 tareas, CRUD directo) — asignado así a propósito por el ritmo del pareo.
- `com.mgwprod.collab` — toplines de artistas sobre beats, comentarios, colaboraciones
  (depende de `catalog` para el FK a `Beat`). Dueño: Dani (reemplaza a su módulo anterior,
  `orders`). Complejidad media (5 tareas, con una máquina de estados y dependencia cruzada).
- `com.mgwprod.challenges` — challenges con jurado ponderado, submissions, votos, resultados,
  ranking/Music Score. Dueño: Paolo. El más pesado de los tres (9 tareas: cálculo ponderado +
  orquestación del cierre).

Detalle completo en `docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md` (spec vigente;
`2026-08-25-mgw-prod-tpo-design.md` queda como referencia histórica del diseño e-commerce).

## Convenciones

- Español para conversación y documentación; código, commits y nombres de clase/paquete en
  inglés (regla heredada del hub).
- Nunca commitear a `main` directo — todo por branch.
