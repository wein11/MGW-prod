# mgw-prod

Plataforma de música para productores y artistas: publicar beats, grabar toplines sobre
beats de otros, colaborar y participar en desafíos con jurado.

TPO de Aplicaciones Interactivas (UADE) — Etapa 1 (backend) y Etapa 2 (frontend).
Grupo: Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi.

## Requisitos

- JDK 21 o superior.
- MySQL en `localhost:3306` con usuario `root` y password `admin`.
- Postman.

## Cómo levantarlo

1. Crear la base y las tablas:

   ```bash
   mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS mgw_prod;"
   mysql -u root -padmin mgw_prod < docs/db/schema.sql
   ```

2. Levantar el servidor (queda en `http://localhost:8080`):

   ```bash
   ./mvnw spring-boot:run
   ```

3. Tests:

   ```bash
   ./mvnw test
   ```

## Frontend (Etapa 2)

Con el servidor levantado, abrir `http://localhost:8080` en el navegador. El frontend está
hecho en HTML5, CSS3 y JavaScript y lo sirve el mismo Spring Boot desde
`src/main/resources/static` (un solo puerto, sin configuración extra).

| Página | Qué hace |
|---|---|
| `index.html` | Landing con barra de navegación y últimos beats |
| `beats.html` | Listado de beats con filtros y alta (usuarios ARTIST) |
| `beat.html` | Detalle, modificación y borrado (dueño del beat o ADMIN) |
| `premium.html` | Compra simulada del plan Premium en 3 pasos (usuarios ARTIST) |
| `about.html` | Descripción de la compañía e integrantes |
| `login.html` / `register.html` | Inicio de sesión y alta de cuenta |
| `challenges.html` / `challenge.html` | Desafíos: crear, entregar, votar y cerrar |
| `ranking.html` | Ranking de productores por puntos |

`js/api.js` concentra la comunicación con la API (fetch + token de sesión) y
`css/styles.css` tiene los estilos de todo el sitio.

## Probar con Postman

1. Importar `docs/api/mgw-prod.postman_collection.json` y
   `docs/api/mgw-prod.postman_environment.json`.
2. Elegir el environment **mgw-prod local**.
3. Mandar los requests en orden (o "Run collection"). Cada request guarda en el environment
   los datos que necesita el siguiente (token, id del beat, id del desafío).

El usuario administrador no se puede registrar por la API: viene cargado en `schema.sql`
(email `admin@mgw.com`, contraseña `admin1234`).

## Módulos

| Paquete | Integrante | Qué hace |
|---|---|---|
| `users` | Santiago | Registro, login con token, roles (ARTIST / DISCOGRAFICA / ADMIN), perfil de artista |
| `catalog` | Santiago y Mateo | Beats (CRUD) y comentarios sobre beats |
| `collab` | Dani | Toplines sobre beats, comentarios y colaboraciones |
| `billing` | Dani | Plan free / premium con pago simulado |
| `challenges` | Paolo | Desafíos, entregas, votos, resultados y ranking |

Cada paquete tiene sus capas `controller`, `model`, `repository` y `service`. Las tablas se
crean a mano con `docs/db/schema.sql` (`ddl-auto=none`).
