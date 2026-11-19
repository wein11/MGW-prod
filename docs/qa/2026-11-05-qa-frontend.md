# QA del frontend React: 5 de noviembre de 2026

Prueba completa de `front/react` antes de la entrega, y correcciones hechas a partir de ella.
Se probó el commit `94875db` y después la rama `front/qa-correcciones`, siempre con el build de producción (`vite build` + `vite preview`).

## Resultado

| Control | Antes | Después |
|---|---|---|
| Pruebas de punta a punta (Chrome real) | 97 de 105 | **116 de 116** |
| Lint (`npm run lint`) | sin errores | sin errores ni advertencias |
| Tests de validación (`npm test`) | 9 de 9 | 9 de 9 |
| Build (`npm run build`) | OK | OK |
| Lighthouse accesibilidad, 10 rutas, celular y escritorio | 100 | 100 |
| Lighthouse buenas prácticas y rendimiento | 100 | 100 |
| Lighthouse SEO | 91 a 92 | 100 |
| axe (WCAG 2.1 A/AA): 33 escenarios | sin violaciones | sin violaciones |

Las pruebas nuevas cubren cada corrección, para que el problema no vuelva sin que se note.

## Problemas encontrados y cómo se corrigieron

| # | Prioridad | Problema | Causa | Corrección | Archivos |
|---|---|---|---|---|---|
| 1 | Alta | En celular, las filas de beats medían 328 px, con un hueco grande después del título | `flex: 1 1 14rem` pasa a ser altura cuando la fila se apila en columna | Debajo de 640 px el bloque usa `flex: 0 0 auto` | `styles/global.css` |
| 2 | Alta | "Votar" en una entrega dejaba elegida otra: se podía votar la equivocada | El select tenía su propio estado y arrancaba siempre en la primera opción | La página guarda la entrega elegida; el link y el select la comparten. El puntaje no se pierde al cambiarla | `pages/ChallengesPage.jsx`, `pages/challenges/VoteForm.jsx` |
| 3 | Alta | "Ver entregas" no bajaba a la sección | El `Link` del router cambia la URL pero no hace scroll al ancla | Ancla común (`<a href="#entregas">`) con el estilo del botón | `pages/ChallengesPage.jsx` |
| 4 | Alta | Una discográfica podía comprar Premium (el backend lo rechaza con 403) | La pantalla solo controlaba que hubiera sesión | Mensaje "Premium es solo para artistas." con link a Desafíos | `pages/PremiumPage.jsx` |
| 5 | Media | Nombres incorrectos en Nosotros | Texto desactualizado | "Danilo Gariboldi" y "Mateo Galluzzo" | `pages/AboutPage.jsx` |
| 6 | Media | Título cortado a 375 px: "COLABORACION / ES" | La palabra no entraba con 36 px | Hasta 480 px el `h1` usa `clamp(1.5rem, 7.8vw, 2.25rem)`. Probado a 320, 375 y 430 px | `styles/global.css` |
| 7 | Media | Una cuenta recién creada no podía volver a entrar | Registro no guardaba la cuenta en ningún lado | Las cuentas se guardan en memoria. Además, un email repetido ahora da error | `context/AuthContext.jsx`, `pages/RegisterPage.jsx` |
| 8 | Media | Premium se perdía al salir y volver a entrar | Login siempre iniciaba con plan Free | El plan de cada usuario se recuerda en memoria | `context/AuthContext.jsx` |
| 9 | Baja | "Salir" desde Premium llevaba a Login | Al cerrar la sesión primero, Premium redirigía antes de llegar al inicio | Primero se navega al inicio y después se cierra la sesión | `components/layout/Header.jsx` |
| 10 | Baja | Login y Registro sin link propio a Privacidad | `AuthLayout` no tenía pie | Pie con el link, en otra pestaña para no perder lo escrito | `components/layout/AuthLayout.jsx`, `AuthLayout.css` |
| 11 | Datos | El texto dice 500, 300 y 150 puntos, pero el podio mostraba 47, 42 y 38 y el primero del ranking 312 | Datos de ejemplo incoherentes | Podio 500, 300 y 150; los totales del ranking son sumas posibles de esos premios | `data/mockData.js`, `pages/RankingPage.jsx`, `pages/HomePage.jsx` |
| 12 | Datos | Un desafío en votación cerraba el 30/10/2026, fecha ya vencida | Fecha fija en los datos | Cierres: 30/11/2026 y 15/12/2026 | `data/mockData.js` |
| 13 | Menor | Lighthouse marcaba el nombre accesible del logo y la falta de `robots.txt` | "MGW" y "PROD" se leían pegados; el archivo no existía | Espacio entre palabras y `public/robots.txt` | `components/layout/Logo.jsx`, `public/robots.txt` |

### Decisiones que conviene conocer

- **Las contraseñas de las cuentas nuevas quedan en memoria** mientras la pestaña está abierta. Es el mismo tratamiento que los usuarios de demo y es necesario para el login simulado. Nunca van a `sessionStorage`, `localStorage` ni a la consola; una prueba lo verifica.
- **Se modificaron `components/`, `context/` y `data/`**, que el plan pide no tocar sin avisar. Los cambios se hicieron a pedido, después de la prueba, y quedan a revisión en el PR.
- **Los puntajes de ejemplo** ya no coinciden con los anteriores del mock, porque tienen que ser coherentes con los premios. Si Santiago prefiere mantener los del wireframe, habría que cambiar también el texto de los 500, 300 y 150 puntos.
- **El comentario del voto no se guarda.** Se valida y se limpia, igual que antes; la pantalla no muestra votos anteriores.

## Comportamiento simulado (no son errores)

- F5 borra lo creado durante la visita: beats, comentarios, toplines, votos, cuentas nuevas y el plan de usuarios sin sesión abierta. La sesión abierta sí se mantiene. **No recargar en medio de la demo.**
- El front no se conecta al backend, como permite la rúbrica. Por eso el precio simulado de $15.000 / mes no coincide con los 15 USD de pago único de `SubscriptionService`.
- El link "Escuchar" lleva a URLs de ejemplo de SoundCloud, que pueden no existir.

## Pendiente: no se resuelve desde el código

1. **Deploy de Vercel con error.** Falla en los commits `08b3e6e` y `94875db` de `front/react`. El build local funciona; los logs solo los puede ver quien administra el proyecto de Vercel:

   ```bash
   npx vercel inspect dpl_5Rje6ijZnwaPebDJpqdH4A3WGtVe --logs
   ```

2. **Rama de entrega.** `frontend/` no está en `main` ni en `entrega/etapa2`. Hay que definir a dónde se lleva `front/react` antes de entregar.

## Qué no cubre esta prueba

- Safari, Firefox y dispositivos físicos.
- Lectores de pantalla reales: se revisaron con axe y Lighthouse los atributos, no la experiencia de uso.
- La versión publicada en Vercel.
- Backend.

## Cómo repetir la prueba

Desde la raíz del repo, en una terminal:

```bash
cd frontend && npm ci && npm run build && npx vite preview --port 4173 --strictPort
```

En otra terminal (hace falta Google Chrome instalado). Las dependencias se instalan en una carpeta temporal para no sumarlas al proyecto:

```bash
QA=$(mktemp -d) && cp docs/qa/e2e-frontend.mjs "$QA/run.mjs" && cd "$QA" && npm init -y >/dev/null && npm i playwright-core@1.48.2 axe-core@4.10.2 && node run.mjs
```

Para probar otra dirección, por ejemplo la de Vercel, definí `BASE_URL`. El script muestra el total por área y detalla cada falla, con una captura en `shots/`.
