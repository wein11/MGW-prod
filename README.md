# mgw-prod

Plataforma de música para productores y artistas: publicar beats, grabar toplines sobre
beats de otros, colaborar y participar en desafíos con jurado.

TPO de Aplicaciones Interactivas (UADE). Etapa 1: backend (Spring Boot). Etapa 2: frontend
(React + Vite, en la carpeta `frontend/`).
Grupo: Santiago Weinbinder, Mateo Galluzo, Paolo Maffei, Dani Gariboldi.

## Frontend (Etapa 2)

El frontend está en `frontend/` y funciona solo, con datos simulados: no hace falta levantar
el backend ni MySQL para probarlo.

```bash
cd frontend
npm install
npm run dev      # abre en http://localhost:5173
```

Usuario de prueba: `artista@mgw.com` / `mgw12345`. Más detalles (otros usuarios, comandos y
estructura) en [`frontend/README.md`](frontend/README.md).

## Backend (Etapa 1)

### Requisitos

- JDK 21 o superior.
- MySQL en `localhost:3306` con usuario `root` y password `admin`.
- Postman.

### Cómo levantarlo

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

### Probar con Postman

1. Importar `docs/api/mgw-prod.postman_collection.json` y
   `docs/api/mgw-prod.postman_environment.json`.
2. Elegir el environment **mgw-prod local**.
3. Mandar los requests en orden (o "Run collection"). Cada request guarda en el environment
   los datos que necesita el siguiente (token, id del beat, id del desafío).

El usuario administrador no se puede registrar por la API: viene cargado en `schema.sql`
(email `admin@mgw.com`, contraseña `admin1234`).

### Módulos

| Paquete | Integrante | Qué hace |
|---|---|---|
| `users` | Santiago | Registro, login con token, roles (ARTIST / DISCOGRAFICA / ADMIN), perfil de artista |
| `catalog` | Santiago y Mateo | Beats (CRUD) y comentarios sobre beats |
| `collab` | Dani | Toplines sobre beats, comentarios y colaboraciones |
| `billing` | Dani | Plan free / premium con pago simulado |
| `challenges` | Paolo | Desafíos, entregas, votos, resultados y ranking |

Cada paquete tiene sus capas `controller`, `model`, `repository` y `service`. Las tablas se
crean a mano con `docs/db/schema.sql` (`ddl-auto=none`).
