# Informe de entrega — Trabajo Práctico Obligatorio

**Materia:** Aplicaciones Interactivas — UADE
**Proyecto:** mgw-prod (Music Discovery & Challenge Platform)
**Grupo:** Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi
**Etapa:** 1 — Backend + Persistencia
**Fecha:** 15/09/2026

---

## 1. Resumen ejecutivo

`mgw-prod` es una plataforma de descubrimiento y colaboración musical para
productores/artistas emergentes: perfiles de artista y discográfica, catálogo de beats,
colaboración entre artistas (toplines sobre beats de otros), y challenges semanales con
jurado ponderado, ranking y verificación de artistas.

El backend está completo para la Etapa 1: cinco módulos funcionales (`users`, `catalog`,
`collab`, `challenges`, `billing`), persistencia real en MySQL, 164 tests automatizados sin
fallas, y una validación funcional end-to-end de los 48 endpoints vía Postman, documentada
en la sección 6 de este informe.

## 2. Objetivo y alcance

El enunciado original de la cátedra (`docs/Trabajo Integrador de Aplicaciones
Interactivas.pdf`) pide una aplicación con "funcionalidades de e-commerce". El profesor
autorizó verbalmente, el 01/09/2026, un pivot de dominio: en lugar de un marketplace
transaccional de beats (carrito, checkout, pagos), el proyecto se enfoca en descubrimiento
y colaboración musical — sin perder la exigencia de arquitectura cliente-servidor,
persistencia real y CRUD completo por módulo que pide la consigna. El diseño vigente de
este pivot está documentado en `docs/design/specs/2026-09-01-mgw-prod-pivot-design.md`.

La entrega se divide en dos etapas obligatorias:

- **Etapa 1** (esta entrega): backend + persistencia, probado con Postman/curl, corriendo
  en `localhost`.
- **Etapa 2** (pendiente): frontend HTML5/CSS3/JavaScript plano — sin frameworks —
  integrado con este backend.

## 3. Arquitectura

Monolito construido con **Spring Boot 4.1 + Spring Data JPA + MySQL**, organizado en cinco
paquetes verticales (uno por módulo funcional), cada uno con sus propias capas
Controller → Service → Repository → Entity:

| Módulo | Responsable | Responsabilidad |
|---|---|---|
| `users` | Santiago | Autenticación por sesión, roles, perfiles de artista, verificación |
| `catalog` | Santiago + Mateo | Publicación y listado de beats, comentarios |
| `collab` | Dani | Toplines, comentarios, colaboraciones (aceptar/rechazar) |
| `challenges` | Paolo | Challenges con jurado ponderado, submissions, votos, ranking |
| `billing` | Dani | Suscripción free/premium, límite de plan, pago simulado |

**Flujo de una request:** Cliente → (interceptor de autenticación por sesión) → Controller
(valida entrada) → Service (lógica de negocio y autorización) → Repository (Spring Data
JPA) → MySQL. La misma entidad JPA se devuelve como respuesta, sin una capa de DTOs
intermedia.

### Decisiones de diseño clave

- **Persistencia vía Spring Data JPA, sin DAO manual** — patrón confirmado por la cátedra
  en Clase 4; no hay SQL manual escrito en el código Java, las queries se derivan por
  nombre de método.
- **Esquema de base de datos manual** (`spring.jpa.hibernate.ddl-auto=none`) — las 13
  tablas están definidas a mano en `docs/db/schema.sql`, replicando la convención vista en
  clase de no delegar la creación del esquema a Hibernate.
- **Sin capa de DTOs** — los controllers reciben y devuelven la entidad JPA directamente,
  siguiendo el ejemplo de cátedra; la validación de entrada se hace explícitamente en cada
  controller (`@Valid`) en vez de depender de la validación automática de Hibernate.
- **Organización de paquetes por módulo, no por capa** — a diferencia del ejemplo de
  cátedra (organizado por capa en la raíz), cada integrante tiene su paquete
  autocontenido (`users/`, `catalog/`, etc.), necesario porque la evaluación es individual
  sobre un trabajo grupal.
- **Autenticación propia** (hash SHA-256 con salt + token de sesión con vencimiento de
  24hs), sin Spring Security, por no haberse visto todavía en la materia al momento de
  implementarla.
- **Dependencias entre módulos unidireccionales, sin ciclos** — verificado en el código:
  `collab` depende de `catalog`, ambos dependen de `billing`, y ni `billing` ni
  `challenges` dependen de ningún módulo hermano.

## 4. Modelo de dominio

13 entidades persistidas en MySQL, entre las que se destacan las relaciones cruzadas entre
módulos: un `Topline` (módulo `collab`) referencia un `Beat` (módulo `catalog`); cada
publicación de contenido (`Beat` o `Topline`) consume cupo de la `Subscription` del
artista (módulo `billing`); y el cierre de un `Challenge` (módulo `challenges`) puede
marcar automáticamente verificado el `ArtistProfile` (módulo `users`) del ganador.

El mecanismo de mayor complejidad de negocio es el cálculo de resultados de un challenge:
el puntaje de cada submission combina tres fuentes de voto con pesos distintos (30% voto de
la comunidad, 30% voto de artistas/productores verificados, 40% voto del artista invitado
del challenge), y el cierre reparte premios y puntos de ranking a los tres primeros
puestos.

## 5. Testing automatizado

```
./mvnw test
Tests run: 164, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Ejecutado y verificado el 15/09/2026. La suite cubre las cuatro capas de cada módulo:
tests unitarios de Service (Mockito), tests de Controller (`@WebMvcTest` + `MockMvc`,
verificando status codes y forma de la respuesta), tests de Repository (`@DataJpaTest`
contra una base H2 en memoria), y tests puntuales de seguridad (hasheo de contraseñas,
interceptor de sesión) y de serialización JSON.

## 6. Validación funcional end-to-end (Postman)

El requisito de la Etapa 1 es demostrar el CRUD completo mediante solicitudes HTTP reales
contra el servidor corriendo — no una prueba interna vía `main()`. Para esto se armó una
Postman Collection (`docs/api/mgw-prod.postman_collection.json`) que cubre los 5 módulos
con al menos un GET-lista, GET-por-id, POST, PUT y DELETE cada uno, con auto-seed de datos
de prueba (registra y loguea un ARTIST y una DISCOGRAFICA, loguea el ADMIN precargado, encadenando IDs y
tokens automáticamente entre requests).

**Corrida del 15/09/2026**, con el servidor levantado en `localhost:8080` contra una base
MySQL local con el esquema cargado. Se ejecutó la Collection completa por dos vías
independientes, para verificar consistencia de resultados:

| Método | Herramienta | Resultado |
|---|---|---|
| CLI (Newman) | `npx newman run` | 48 requests, 0 fallos |
| CLI (Postman CLI) | `postman collection run` | 48 requests, 0 fallos, corrida subida a Postman Cloud |

Ambas corridas dieron el mismo resultado: **48/48 requests exitosos**, con los status
codes esperados (200 en lecturas, 201 en creaciones, 204 en borrados) en los cinco módulos:
autenticación y perfiles (`users`), catálogo de beats (`catalog`), toplines y
colaboraciones (`collab`), challenges completos incluyendo cierre y ranking
(`challenges`), y suscripciones (`billing`). Tiempo total de ejecución: ~0.5-1 segundo,
tiempo de respuesta promedio por request de 3-13 ms.

Esta validación confirma que el backend responde correctamente a un flujo real de uso de
punta a punta — no solo que los tests unitarios/de integración pasan en aislamiento, sino
que el sistema completo (HTTP + autenticación + lógica de negocio + persistencia en MySQL)
funciona correctamente quien lo consume como lo haría un cliente real.

## 7. Limitaciones conocidas

Se identificaron y documentaron (sin corregir aún, por requerir una técnica de JPA no
vista en clase — locking pesimista o `UPDATE` atómico) dos condiciones de carrera acotadas
al módulo `billing`, en el conteo de producciones del plan free y en la creación de la
primera suscripción de un usuario ante requests simultáneos. Ninguna de las dos corrompe
datos ni afecta el flujo normal de uso o de demostración; están detalladas en
`README.md`, sección "Limitaciones conocidas".

## 8. Estado actual y próximos pasos

La Etapa 1 (backend + persistencia) está **completa y validada**: los cinco módulos
funcionan de punta a punta, con test automatizado y validación manual/funcional contra un
servidor real. Queda pendiente la Etapa 2: construcción del frontend en HTML5/CSS3/JS
plano, integrado contra los endpoints ya validados en este informe.

## 9. Referencias

- `README.md` — instrucciones de setup y arquitectura general.
- `docs/GUIA-TECNICA-EQUIPO.md` — guía técnica detallada por módulo, para preparación de
  la defensa individual.
- `docs/api/*-API.md` — referencia de endpoints por módulo.
- `docs/design/specs/2026-09-01-mgw-prod-pivot-design.md` — diseño vigente del dominio.
- `docs/design/specs/2026-09-04-mgw-prod-professor-corrections-design.md` — diseño de las
  correcciones incorporadas tras la devolución del profesor.
