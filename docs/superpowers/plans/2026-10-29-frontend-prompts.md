# Prompts para Claude Code: frontend React de MGW Prod

Orden: A sube la base el viernes 30/10. **Recién entonces** B, C y D pegan su prompt.

---

## Prompt común (va al principio de los 4)

```
Estoy trabajando en el TPO de Aplicaciones Interactivas (UADE), repo github.com/wein11/MGW-prod.
Hacé `git fetch` y `git checkout front/react`, y después `git pull`.

Antes de tocar nada, leé completos:
1. docs/superpowers/plans/2026-10-29-frontend-react.md (el plan del equipo). Mirá sobre todo
   "Restricciones globales", "Foco de revisión" y MI tarea.
2. docs/wireframes/MGW-Prod-Wireframes.pdf: las pantallas que tengo que respetar.

Reglas que no se negocian:
- NINGÚN título lleva texto chico arriba (eyebrow/kicker tipo "CATALOGO", "PASO 1 DE 3").
  El wireframe los tiene: se ignoran. Solo el título grande y la bajada abajo.
- Datos simulados (src/data/mockData.js); no se conecta al backend.
- HTML semántico (nada de div en lugar de h/p/ul/table), labels en todos los inputs,
  foco visible, navegable con teclado y sin scroll horizontal a 375 px.
- Usá los componentes que ya existen (PageHeader, FormField, Alert, Button, StatusBadge,
  ConfirmDelete, EmptyState, useAuth, fakeRequest, utils/validation.js). NO modifiques
  components/, context/, data/ ni utils/: si te falta algo, frená y decímelo, así lo pido al grupo.
- Imágenes solo locales en src/assets; nada de Google Fonts ni CDNs.
- Comentarios del código en español, explicando qué hace y por qué. Commits en español,
  SIN "Co-Authored-By" y sin mencionar a Claude.
- Trabajá en tu propia rama (te la indico abajo) y al final abrí un PR contra front/react.
  No mergees vos.

Explicame cada decisión mientras avanzás: tengo que poder defender el código delante del profe.
Antes de decir que terminaste, corré la verificación de mi tarea, `npm run lint` y `npm run build`,
y mostrame el resultado.
```

---

## A (Santiago): base + Landing + Nosotros + Privacidad + 404 + QA

```
[pegar el prompt común]

Soy la persona A. Crear la rama front/react desde main (si no existe) y traer docs/wireframes/
desde la rama entrega/etapa2. Hacer la Tarea 1 (base) primero y pushearla en cuanto pase
su verificación, porque los otros 3 arrancan desde ahí. mockData.js tiene que incluir TODOS
los datos que muestran las 14 pantallas del wireframe (beats, comentarios, toplines,
desafíos, entregas, ranking top 10, planes, usuarios de demo), para que nadie más lo toque.
Después hacer la Tarea 2 en la rama front/a-landing. La Tarea 6 (QA) va cuando estén los 3 PR.
```

## B (Mateo): Beats + Detalle de beat

```
[pegar el prompt común]

Soy la persona B. Mi parte es la Tarea 3: pantallas "2. Listado de beats" y
"3. Detalle de beat (dueño)" del wireframe. Rama: front/b-beats.
Puntos clave: formulario "Publicar beat" solo para ARTIST, con el error exacto
"El BPM tiene que estar entre 40 y 220. Revisalo y volvé a intentar.";
filtros por género, BPM y productor con "Limpiar" y estado vacío; en el detalle:
comentarios, "Gestionar mi beat" solo para el dueño (editar + borrar en dos pasos)
y toplines (aceptar/rechazar el dueño, subir topline un ARTIST).
Probalo como visitante, como DJ Nova (artista@mgw.com / mgw12345) y como otro artista.
```

## C (Paolo): Colaboraciones + Desafíos + Ranking

```
[pegar el prompt común]

Soy la persona C. Mi parte es la Tarea 4: pantallas "3b. Colaboraciones",
"5. Desafíos" y "6. Ranking" del wireframe. Rama: front/c-desafios.
Puntos clave: en Colaboraciones, filtro por estado, aceptar/rechazar/quitar/borrar
(borrar en dos pasos), editar el link del topline inline, y los estados "sin sesión" y
"cuenta discográfica". En Desafíos, el voto del 1 al 10 es un fieldset con radios
(no divs) con espera y el mensaje "Tu voto se registró."; después de votar la entrega
pasa a "Ya votaste"; el podio es un <ol> (el orden visual 2-1-3 va por CSS).
Ranking es una <table> con caption y th scope, que no desborde a 375 px.
Probalo como visitante, como ARTIST (artista@mgw.com / mgw12345) y como LABEL
(discografica@mgw.com / mgw12345).
```

## D (Danilo): Premium + Login + Registro + Contacto

```
[pegar el prompt común]

Soy la persona D. Mi parte es la Tarea 5: pantallas "4a/4b/4c Premium",
"9a. Login", "9b. Registro" y "8. Contacto" del wireframe. Rama: front/d-formularios.
Acá están los DATOS SENSIBLES, así que es crítico:
- Checkbox obligatorio "Leí y acepto la Política de privacidad" (con link a /privacidad)
  en Registro, Contacto y Pago; sin tildarlo no se envía.
- La tarjeta NUNCA se guarda: ni localStorage, ni sessionStorage, ni console.log.
  En el paso 3 solo se muestra "•••• 1111" y el formulario se limpia.
- autocomplete correcto (cc-name, cc-number, cc-exp, cc-csc, email,
  new-password, current-password); CVV y contraseña con type="password".
Todos los formularios: error por campo (aria-invalid + aria-describedby), foco en el
primer error, estado de espera ("Enviando…") y mensaje de éxito.
Verificá con la tarjeta 4111 1111 1111 1112 (error), con 4111 1111 1111 1111 (OK) y con el
vencimiento 09/26 (vencida), y revisá en DevTools → Application que no quede nada sensible.
```
