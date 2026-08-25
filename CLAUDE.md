# mgw-prod — Music Discovery & Challenge Platform (TPO Aplicaciones Interactivas)

Hereda el contexto global de `../../CLAUDE.md` y `~/.claude/CLAUDE.md`.

## Qué es este proyecto

Trabajo Práctico Obligatorio de la materia **Aplicaciones Interactivas (UADE)**. Grupo de 3:
**Santiago Weinbinder, Mateo Galluzo, Paolo Maffei**. La evaluación es individual aunque el
trabajo sea grupal — cada integrante debe poder explicar y defender su módulo.

Es la primera versión (académica) de una idea de producto más grande: una plataforma de
descubrimiento y colaboración musical para productores/artistas emergentes, con challenges
semanales, rankings y reputación ("Music Score"). Para el TPO, el alcance se recortó a lo que
es defendible como **e-commerce transaccional** con el stack que exige la cátedra — el resto
del pitch original (matching avanzado, monetización, marketplace de servicios) queda fuera de
esta entrega.

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
- Este proyecto **no sigue el stack default del hub** (`FastAPI/React/Postgres`) porque el
  stack viene impuesto por la cátedra.

## Arquitectura

Monolito Spring Boot, un solo `pom.xml`, tres paquetes verticales — cada uno dueño de un
integrante, cada uno con su propio Controller/Service/Repository/Entity:

- `com.mgwprod.users` — perfiles, roles (PRODUCER/ARTIST), auth.
- `com.mgwprod.marketplace` — catálogo de beats, carrito, checkout (e-commerce core).
- `com.mgwprod.challenges` — challenges, submissions, votos, ranking/Music Score.

Detalle completo en `docs/superpowers/specs/2026-08-25-mgw-prod-tpo-design.md`.

## Convenciones

- Español para conversación y documentación; código, commits y nombres de clase/paquete en
  inglés (regla heredada del hub).
- Nunca commitear a `main` directo — todo por branch.
