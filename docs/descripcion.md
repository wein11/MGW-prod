# mgw-prod — Descripción del proyecto

## Qué es

**mgw-prod** (Music Discovery & Challenge Platform) es una plataforma de descubrimiento y
colaboración musical para productores y artistas emergentes. Es una mezcla de **SoundCloud**
(publicar producciones, escuchar y comentar lo que suben otros) y **LinkedIn** (un perfil que
funciona como portfolio profesional de todo lo que hiciste), con un diferencial propio:
**desafíos periódicos con premios reales y un sistema de jurado ponderado** que puede abrirle a
un productor desconocido una oportunidad de trabajar con un artista reconocido.

Es el Trabajo Práctico Obligatorio de la materia **Aplicaciones Interactivas (UADE)**, hecho por
un grupo de 4 (Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi), pero está
diseñado como la primera versión real de una idea de producto que el grupo quiere seguir
desarrollando más allá de la materia.

## El problema

Un productor de beats emergente en Argentina (o cualquier país de habla hispana) no tiene un
lugar claro donde:
- Mostrar su trabajo a artistas que buscan una base para grabar.
- Demostrar su nivel frente a sellos o artistas más grandes sin ya tener contactos en la
  industria.
- Competir de forma medible contra otros productores y ganar visibilidad por mérito, no solo
  por seguidores.

Un artista emergente, del otro lado, no tiene un lugar simple para encontrar beats de calidad,
mostrar cómo sonaría su voz sobre una producción ajena, y conectar con el productor si el
resultado convence a ambos.

## Cómo lo resuelve mgw-prod

### 1. Perfil como portfolio
Cada usuario (productor o artista) tiene un perfil público. Todo lo que publica —beats,
interpretaciones sobre beats ajenos, resultados de desafíos— queda asociado a ese perfil, de
forma que cualquiera (un artista, un sello, otro productor) puede ver de un vistazo el
recorrido y el nivel de esa persona, igual que un perfil de LinkedIn muestra experiencia.

### 2. Publicaciones tipo red social
- Los **productores** suben sus beats (título, género, BPM, tonalidad, link de audio).
- Los **artistas** suben **"toplines"**: su propia interpretación o voz sobre un beat que ya
  publicó un productor — literalmente "así cantaría yo esto".
- La comunidad puede **comentar** tanto los beats como los toplines, generando la dinámica de
  feed social que tiene SoundCloud.

### 3. Colaboración
Cuando un artista sube un topline sobre un beat, automáticamente se genera una propuesta de
colaboración. El productor dueño del beat escucha el resultado y decide si quiere colaborar
formalmente con ese artista o no. Es la forma concreta en la que dos desconocidos terminan
trabajando juntos sin necesidad de conocerse antes.

### 4. Desafíos con jurado ponderado (el diferencial)

Esta es la pieza que distingue a mgw-prod de cualquier red social musical genérica. Funciona
así:

> **🏆 Desafío semanal: "Creamos el próximo hit de RKT"**
> Un artista o productor reconocido propone el desafío: BPM 100, tonalidad F#m, tema libre,
> deadline el domingo a las 23:59. Los productores tienen 7 días para crear y enviar su
> producción.

La votación no es un simple "me gusta": hay **tres categorías de jurado con peso distinto**
sobre el puntaje final de cada producción:

| Jurado | Peso |
|---|---|
| 👥 Comunidad (cualquier usuario) | 30% |
| 🎹 Productores verificados | 30% |
| 🎤 Artista invitado del desafío | 40% |

El resultado es un ranking (🥇🥈🥉) con premios por puesto — dinero simbólico, sesión de estudio,
verificación de cuenta, puntos para el ranking global (Music Score), un badge exclusivo y
aparición en el ranking nacional.

**Pero el premio más valioso no siempre es el primer puesto.** El artista invitado a cada
desafío puede escuchar, por ejemplo, los 20 mejores envíos y decidir: *"quiero trabajar con el
#7"* — una oportunidad real de colaboración con alguien reconocido, sin importar si ganó o no
el ranking. Eso puede valer muchísimo más para un productor desconocido que el premio en dinero.

### 5. Ranking y reputación (Music Score)
Cada vez que un producer entra al podio de un desafío, suma puntos a su Music Score. La suma
acumulada de todos sus resultados históricos arma un ranking global — la reputación medible que
un productor puede mostrar en su perfil-portfolio.

## Roles de usuario

- **PRODUCER**: publica beats, participa en desafíos, recibe/gestiona propuestas de
  colaboración sobre sus beats, puede quedar marcado como "verificado" (mayor peso de voto).
- **ARTIST**: publica toplines sobre beats ajenos, propone colaboraciones implícitamente al
  subir un topline, puede ser designado "artista invitado" de un desafío puntual.
- **Admin** (flag, no un rol de usuario nuevo): crea desafíos, cierra desafíos y reparte
  resultados, verifica productores.

## Qué NO incluye esta entrega (y por qué)

El pitch de producto completo incluye una pieza con mucho más potencial comercial: un
**Talent Discovery Platform** para sellos discográficos — un sello paga para buscar, por
ejemplo, "productores de trap de Argentina entre 18 y 30 años con ranking superior a 500" y la
plataforma le devuelve una lista de candidatos recomendados. Es, en esencia, un buscador de
talento pago sobre los datos de reputación que genera el resto de la plataforma.

Esta pieza queda **fuera del alcance del TPO** a propósito: introduce un rol de usuario nuevo
(`LABEL`), un modelo de monetización, y un motor de búsqueda/recomendación — nada de eso suma a
lo que la cátedra evalúa (arquitectura por capas, CRUD, persistencia SQL, manejo de errores) y
hubiera obligado a que alguien del grupo cargue con un quinto módulo en un cuatrimestre. Queda
documentado acá como el siguiente paso natural del producto una vez terminada la materia.

## De dónde viene este documento

Esta descripción refleja el pivot de alcance del 2026-09-01: el proyecto arrancó recortado a
"marketplace de beats" (catálogo + carrito + checkout simulado) porque la consigna original de
la cátedra exigía una aplicación transaccional de e-commerce. Cuando el profesor confirmó que
ese requisito ya no aplica, el grupo decidió volver a la idea de producto original en vez de
mantener el recorte de e-commerce. El diseño técnico completo de este pivot —modelo de datos,
endpoints, y qué código se borra/agrega— está en
`docs/superpowers/specs/2026-09-01-mgw-prod-pivot-design.md`.
