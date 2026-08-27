# mgw-prod

Music Discovery & Challenge Platform — TPO de Aplicaciones Interactivas (UADE).
Grupo: Santiago Weinbinder, Mateo Galluzo, Paolo Maffei.

## Requisitos

- JDK 21 o superior (si tenés varias versiones instaladas, verificá que `JAVA_HOME` apunte a una ≥21 — un JDK más viejo falla con "release 21 not supported").
- MySQL corriendo en `localhost:3306`, con un usuario `root` y password `admin` (misma convención que `GestorDeInventario` de Clase 2).
- No hace falta tener Maven instalado — el proyecto trae el wrapper (`./mvnw`).

## Setup inicial

1. Crear la base de datos local (una sola vez, o cada vez que las entidades cambien de forma — ver nota abajo):

   ```bash
   mysql -u root -padmin -e "CREATE DATABASE IF NOT EXISTS mgw_prod;"
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

## Convención: recrear la base al cambiar entidades

`spring.jpa.hibernate.ddl-auto=update` (en `application.properties`) solo agrega columnas — nunca las borra ni renombra. Si cambiás la forma de una entidad (`@Entity`), dropeá y recreá la base local en vez de dejar que Hibernate acumule columnas viejas:

```bash
mysql -u root -padmin -e "DROP DATABASE IF EXISTS mgw_prod; CREATE DATABASE mgw_prod;"
```

## Troubleshooting

Si en Windows o con una versión distinta de MySQL aparece un error de zona horaria o de "public key retrieval", agregá parámetros a la URL en `application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/mgw_prod?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
```

## Stack

Ver `docs/superpowers/specs/2026-08-25-mgw-prod-tpo-design.md` para el diseño completo y `CLAUDE.md` para el contexto del proyecto.
