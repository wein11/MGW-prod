// Test de punta a punta de MGW Prod contra el build de producción.
// Cómo correrlo: ver docs/qa/2026-11-05-qa-frontend.md.
import { chromium } from 'playwright-core'
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'

const BASE = process.env.BASE_URL || 'http://localhost:4173'
const axeSrc = readFileSync(new URL('./node_modules/axe-core/axe.min.js', import.meta.url), 'utf8')
const SHOTS = new URL('./shots/', import.meta.url).pathname
mkdirSync(SHOTS, { recursive: true })

const browser = await chromium.launch({ channel: 'chrome', headless: true })
const results = []
const only = process.argv[2]

function assert(cond, msg) { if (!cond) throw new Error(msg) }
function eq(a, b, msg) { if (a !== b) throw new Error(`${msg}: esperaba ${JSON.stringify(b)}, obtuve ${JSON.stringify(a)}`) }

async function t(area, name, fn, { width = 1280, height = 900 } = {}) {
  if (only && !area.includes(only)) return
  const ctx = await browser.newContext({ viewport: { width, height } })
  const page = await ctx.newPage()
  page.setDefaultTimeout(5000)
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push('pageerror: ' + e.message))
  page.on('dialog', d => d.dismiss())
  try {
    await fn(page, ctx)
    assert(errors.length === 0, 'errores de consola: ' + errors.join(' | '))
    results.push({ area, name, ok: true })
  } catch (e) {
    results.push({ area, name, ok: false, err: e.message.split('\n')[0] })
    await page.screenshot({ path: `${SHOTS}fail-${results.length}.png` }).catch(() => {})
  }
  await ctx.close()
}

const NOVA = 'artista@mgw.com', KIRA = 'kira@mgw.com', LABEL = 'discografica@mgw.com', PASS = 'mgw12345'

async function login(page, email, password = PASS) {
  await page.goto(BASE + '/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.waitForURL(BASE + '/')
}
// Navega sin recargar: los datos simulados viven en memoria.
async function nav(page, label) {
  const link = page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: label, exact: true })
  await link.click()
  await page.locator('nav[aria-label=Principal] a[aria-current=page]', { hasText: label }).waitFor()
  await page.waitForTimeout(80)
}
async function logout(page) { await page.getByRole('button', { name: 'Salir' }).click() }
async function loginSpa(page, email) {
  await page.getByRole('link', { name: 'Entrar' }).first().click()
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(PASS)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.waitForURL(BASE + '/')
}
async function openBeat(page, title) {
  await nav(page, 'Beats')
  await page.getByRole('link', { name: `Ver detalle de ${title}` }).click()
  await page.getByRole('heading', { level: 1, name: title }).waitFor()
}
const focusId = page => page.evaluate(() => document.activeElement?.id || document.activeElement?.tagName)
const msgs = page => page.locator('.field-message').allTextContents()
const body = page => page.locator('body').textContent()
const noHScroll = page => page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)

// Las rutas que recorren los controles generales.
const ROUTES = ['/', '/beats', '/beats/1', '/colaboraciones', '/desafios', '/ranking', '/premium', '/nosotros', '/contacto', '/privacidad', '/no-existe', '/login', '/registro']

/* ---------- GLOBAL ---------- */
for (const [w, h] of [[375, 812], [768, 1024], [1280, 900]]) {
  for (const who of ['visitante', 'artista']) {
    await t('Global', `Todas las rutas a ${w}px como ${who}: 1 h1, título, imágenes, sin scroll horizontal`, async page => {
      if (who === 'artista') await login(page, NOVA)
      for (const r of ROUTES) {
        if (who === 'artista' && (r === '/login' || r === '/registro')) continue
        await page.goto(BASE + r); await page.waitForLoadState('networkidle')
        eq(await page.locator('h1').count(), 1, `${r} cantidad de h1`)
        assert((await page.title()).includes('MGW Prod'), `${r} título: ${await page.title()}`)
        const broken = await page.evaluate(() => [...document.images].filter(i => !i.complete || i.naturalWidth === 0).map(i => i.src))
        eq(broken.length, 0, `${r} imágenes rotas ${broken}`)
        const over = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
        assert(over <= 0, `${r} desborda ${over}px`)
        const noAlt = await page.evaluate(() => [...document.images].filter(i => !i.hasAttribute('alt')).length)
        eq(noAlt, 0, `${r} imágenes sin alt`)
        const dup = await page.evaluate(() => { const ids = [...document.querySelectorAll('[id]')].map(e => e.id); return ids.filter((x, i) => ids.indexOf(x) !== i) })
        eq(dup.length, 0, `${r} ids duplicados ${dup}`)
      }
    }, { width: w, height: h })
  }
}

await t('Global', 'Recursos solo locales (sin Google Fonts ni CDN)', async page => {
  const ext = []
  page.on('request', q => { if (!q.url().startsWith(BASE) && !q.url().startsWith('data:')) ext.push(q.url()) })
  for (const r of ROUTES) { await page.goto(BASE + r); await page.waitForLoadState('networkidle') }
  eq(ext.length, 0, 'pedidos externos ' + ext.join(','))
})

await t('Global', 'Ningún título tiene texto chico arriba', async page => {
  await login(page, NOVA)
  const found = []
  for (const r of ROUTES.filter(x => x !== '/login' && x !== '/registro')) {
    await page.goto(BASE + r); await page.waitForLoadState('networkidle')
    const bad = await page.evaluate(() => [...document.querySelectorAll('h1,h2,h3')].filter(h => {
      const p = h.previousElementSibling
      return p && p.textContent.trim() && p.textContent.trim().length < 40 && parseFloat(getComputedStyle(p).fontSize) < parseFloat(getComputedStyle(h).fontSize) * 0.7
    }).map(h => h.previousElementSibling.textContent.trim() + ' > ' + h.textContent.trim()))
    if (bad.length) found.push(r + ': ' + bad.join('; '))
  }
  eq(found.length, 0, found.join(' || '))
})

await t('Global', 'Encabezados sin saltos de nivel en todas las rutas', async page => {
  await login(page, NOVA)
  const found = []
  for (const r of ROUTES.filter(x => x !== '/login' && x !== '/registro')) {
    await page.goto(BASE + r); await page.waitForLoadState('networkidle')
    const levels = await page.evaluate(() => [...document.querySelectorAll('h1,h2,h3,h4,h5,h6')].map(h => +h.tagName[1]))
    levels.forEach((l, i) => { if (i && l > levels[i - 1] + 1) found.push(`${r}: h${levels[i - 1]}→h${l}`) })
  }
  eq(found.length, 0, found.join(', '))
})

await t('Global', 'Skip link: primer Tab y lleva al contenido', async page => {
  await page.goto(BASE + '/beats')
  await page.keyboard.press('Tab')
  eq(await page.evaluate(() => document.activeElement.textContent), 'Saltar al contenido', 'primer foco')
  await page.keyboard.press('Enter')
  eq(await focusId(page), 'main', 'foco tras Enter')
})

await t('Global', 'Menú hamburguesa con teclado a 375 px', async page => {
  await page.goto(BASE + '/')
  const btn = page.getByRole('button', { name: 'Menú' })
  eq(await btn.getAttribute('aria-expanded'), 'false', 'aria-expanded inicial')
  assert(!(await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Beats' }).isVisible()), 'links visibles con menú cerrado')
  await btn.focus(); await page.keyboard.press('Enter')
  const close = page.getByRole('button', { name: 'Cerrar' })
  eq(await close.getAttribute('aria-expanded'), 'true', 'aria-expanded abierto')
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Beats' }).focus()
  await page.keyboard.press('Enter')
  await page.waitForURL(BASE + '/beats')
  eq(await page.getByRole('button', { name: 'Menú' }).getAttribute('aria-expanded'), 'false', 'se cierra al navegar')
}, { width: 375, height: 812 })

await t('Global', 'Con el menú cerrado los links no reciben foco a 375 px', async page => {
  await page.goto(BASE + '/')
  const seen = []
  for (let i = 0; i < 6; i++) { await page.keyboard.press('Tab'); seen.push(await page.evaluate(() => document.activeElement.textContent.trim())) }
  assert(!seen.includes('Beats') || seen.indexOf('Beats') > 3, 'foco en link oculto: ' + seen.join(','))
  assert(!seen.includes('Colaboraciones'), 'foco en link oculto: ' + seen.join(','))
}, { width: 375, height: 812 })

await t('Global', 'Link activo con aria-current y navegación del menú', async page => {
  await page.goto(BASE + '/')
  for (const [label, path] of [['Beats', '/beats'], ['Colaboraciones', '/colaboraciones'], ['Desafíos', '/desafios'], ['Ranking', '/ranking'], ['Nosotros', '/nosotros'], ['Contacto', '/contacto'], ['Inicio', '/']]) {
    await nav(page, label)
    await page.waitForURL(BASE + path)
    eq(await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: label, exact: true }).getAttribute('aria-current'), 'page', label)
  }
})

await t('Global', 'Footer: Quiénes somos, Contacto y Privacidad', async page => {
  await page.goto(BASE + '/')
  const f = page.getByRole('navigation', { name: 'Pie de página' })
  for (const [label, path] of [['Quiénes somos', '/nosotros'], ['Contacto', '/contacto'], ['Política de privacidad', '/privacidad']]) {
    await f.getByRole('link', { name: label }).click(); await page.waitForURL(BASE + path)
  }
})

await t('Global', 'Login y Registro tienen link a la Política de privacidad (plan: "todas las páginas")', async page => {
  await page.goto(BASE + '/login')
  assert(await page.getByRole('link', { name: /privacidad/i }).count() > 0, 'Login no tiene link a Privacidad')
})

await t('Global', 'F5 conserva la sesión; ruta rara da 404 con link al inicio', async page => {
  await login(page, NOVA)
  await page.goto(BASE + '/beats/1'); await page.reload()
  assert(await page.getByRole('button', { name: 'Salir' }).isVisible(), 'perdió la sesión')
  await page.goto(BASE + '/cualquier/cosa')
  await page.getByRole('heading', { level: 1, name: 'Página no encontrada' }).waitFor()
  await page.locator('main').getByRole('link', { name: 'Ir al inicio' }).click(); await page.waitForURL(BASE + '/')
})

await t('Global', 'Vuelve arriba al cambiar de página', async page => {
  await page.goto(BASE + '/beats')
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await page.getByRole('link', { name: 'Ver detalle de Neón Suave' }).click()
  await page.getByRole('heading', { level: 1, name: 'Neón Suave' }).waitFor()
  eq(await page.evaluate(() => window.scrollY), 0, 'scrollY')
})

await t('Global', 'Salir cierra la sesión y borra sessionStorage', async page => {
  await login(page, NOVA)
  await logout(page)
  await page.getByRole('link', { name: 'Entrar' }).first().waitFor()
  eq(await page.evaluate(() => sessionStorage.length), 0, 'sessionStorage')
})

for (const w of [320, 375, 430]) {
  await t('Global', `A ${w}px ningún h1 corta una palabra al medio`, async page => {
    await login(page, NOVA)
    const cut = []
    for (const r of ROUTES.filter(x => x !== '/login' && x !== '/registro')) {
      await page.goto(BASE + r); await page.waitForLoadState('networkidle')
      const bad = await page.locator('h1').evaluate(h => {
        const node = h.firstChild, out = []
        for (const m of node.textContent.matchAll(/\S+/g)) {
          const range = document.createRange()
          range.setStart(node, m.index); range.setEnd(node, m.index + m[0].length)
          if (new Set([...range.getClientRects()].map(q => Math.round(q.top))).size > 1) out.push(m[0])
        }
        return out
      })
      if (bad.length) cut.push(`${r}: ${bad.join(',')}`)
    }
    eq(cut.length, 0, cut.join(' | '))
  }, { width: w, height: 800 })
}
await t('Global', 'A 375px las filas de beats no tienen huecos', async page => {
  for (const r of ['/', '/beats']) {
    await page.goto(BASE + r); await page.waitForLoadState('networkidle')
    const h = await page.locator('.beat-row').first().evaluate(e => e.getBoundingClientRect().height)
    assert(h < 170, `${r}: la fila mide ${Math.round(h)}px`)
  }
}, { width: 375, height: 812 })

/* ---------- INICIO / NOSOTROS / PRIVACIDAD ---------- */
await t('Inicio', 'Botones, 3 tarjetas con link, jurado y últimos 3 beats', async page => {
  await page.goto(BASE + '/')
  await page.getByRole('heading', { level: 1, name: /Tu música/ }).waitFor()
  assert(/30/.test(await body(page)) && /40/.test(await body(page)), 'banda de jurado')
  eq(await page.locator('.feature-list > li').count(), 3, 'tarjetas')
  eq(await page.locator('.beat-list > li').count(), 3, 'últimos beats')
  await page.getByRole('link', { name: 'Crear cuenta' }).click(); await page.waitForURL(BASE + '/registro')
  await page.goto(BASE + '/')
  for (const [name, path] of [['Ver beats', '/beats'], ['Ver desafíos', '/desafios'], ['Ver ranking', '/ranking']]) {
    await page.goto(BASE + '/')
    await page.locator('.feature-list').getByRole('link', { name }).click(); await page.waitForURL(BASE + path)
  }
})
await t('Inicio', 'Con sesión reemplaza Crear cuenta y los últimos beats incluyen uno nuevo', async page => {
  await login(page, NOVA)
  eq(await page.getByRole('link', { name: 'Crear cuenta' }).count(), 0, 'Crear cuenta visible')
  await nav(page, 'Beats')
  await page.locator('#title').fill('Beat QA'); await page.locator('#audioUrl').fill('https://soundcloud.com/qa/x')
  await page.locator('#bpm').fill('120'); await page.locator('#key').fill('Am')
  await page.getByRole('button', { name: 'Publicar beat' }).click()
  await page.getByText('ya está publicado').waitFor()
  assert(/9 beats publicados/i.test(await body(page)), 'franja En vivo no actualizó')
  await nav(page, 'Inicio')
  assert(await page.locator('.beat-list').getByRole('heading', { name: 'Beat QA' }).isVisible(), 'no está en últimos beats')
})
await t('Nosotros', 'Integrantes con los nombres que indicaste', async page => {
  await page.goto(BASE + '/nosotros')
  const names = await page.locator('.team-list h3').allTextContents()
  eq(names.length, 4, 'integrantes')
  for (const n of ['Santiago Weinbinder', 'Danilo Gariboldi', 'Mateo Galluzzo', 'Paolo Maffei']) assert(names.includes(n), `falta "${n}"; hay: ${names.join(', ')}`)
})
await t('Nosotros', 'Botón Escribinos', async page => {
  await page.goto(BASE + '/nosotros')
  await page.getByRole('link', { name: 'Escribinos' }).click(); await page.waitForURL(BASE + '/contacto')
})
await t('Privacidad', 'Secciones obligatorias: Ley 25.326, AAIP, tarjeta, fecha', async page => {
  await page.goto(BASE + '/privacidad')
  const txt = await body(page)
  for (const k of ['25.326', 'AAIP', 'tarjeta', 'supresión', 'UADE', 'actualización']) assert(txt.includes(k) || txt.toLowerCase().includes(k.toLowerCase()), 'falta ' + k)
  assert(await page.locator('main h2').count() >= 10, 'pocas secciones')
})

/* ---------- BEATS ---------- */
await t('Beats', 'Visitante y discográfica no ven el formulario de publicar', async page => {
  await page.goto(BASE + '/beats')
  eq(await page.locator('#title').count(), 0, 'visitante ve form')
  assert((await body(page)).includes('tenés que entrar'), 'sin aviso')
  await login(page, LABEL); await nav(page, 'Beats')
  eq(await page.locator('#title').count(), 0, 'label ve form')
  assert((await body(page)).includes('Solo las cuentas de artista'), 'sin aviso label')
})
await t('Beats', 'Publicar vacío: errores por campo, aria y foco', async page => {
  await login(page, NOVA); await nav(page, 'Beats')
  await page.getByRole('button', { name: 'Publicar beat' }).click()
  eq((await msgs(page)).length, 4, 'errores')
  eq(await focusId(page), 'title', 'foco')
  eq(await page.locator('#title').getAttribute('aria-invalid'), 'true', 'aria-invalid')
  eq(await page.locator('#title').getAttribute('aria-describedby'), 'title-error', 'describedby')
})
await t('Beats', 'BPM 300/39/221/12.5 dan el error exacto; 40 y 220 pasan; http rechazado', async page => {
  await login(page, NOVA); await nav(page, 'Beats')
  const exact = 'El BPM tiene que estar entre 40 y 220. Revisalo y volvé a intentar.'
  await page.locator('#title').fill('T'); await page.locator('#key').fill('Dm')
  await page.locator('#audioUrl').fill('http://x.com/a')
  for (const v of ['300', '39', '221', '12.5']) {
    await page.locator('#bpm').fill(v)
    await page.getByRole('button', { name: 'Publicar beat' }).click()
    eq(await page.locator('#bpm-error').textContent(), exact, 'BPM ' + v)
  }
  assert((await page.locator('#audioUrl-error').textContent()).includes('https://'), 'url')
  await page.locator('#audioUrl').fill('https://x.com/a')
  for (const v of ['40', '220']) {
    await page.locator('#title').fill('T' + v); await page.locator('#audioUrl').fill('https://x.com/a'); await page.locator('#key').fill('Dm')
    await page.locator('#bpm').fill(v)
    await page.getByRole('button', { name: 'Publicar beat' }).click()
    await page.getByText(`"T${v}" ya está publicado`).waitFor()
  }
})
await t('Beats', 'Publicar: espera, éxito, primero en lista, contador y form limpio', async page => {
  await login(page, NOVA); await nav(page, 'Beats')
  await page.locator('#title').fill('Nuevo QA'); await page.locator('#genre').selectOption('Lo-fi')
  await page.locator('#audioUrl').fill('https://soundcloud.com/q/a'); await page.locator('#bpm').fill('100'); await page.locator('#key').fill('Am')
  await page.getByRole('button', { name: 'Publicar beat' }).click()
  await page.getByRole('button', { name: 'Enviando…' }).waitFor()
  await page.getByText('ya está publicado').waitFor()
  eq(await page.locator('.beat-list > li h3').first().textContent(), 'Nuevo QA', 'primero')
  assert((await page.locator('.section-head p[aria-live]').textContent()).includes('9 beats'), 'contador')
  eq(await page.locator('#title').inputValue(), '', 'form limpio')
  await page.getByRole('link', { name: 'Ver detalle de Nuevo QA' }).click()
  await page.getByRole('heading', { level: 2, name: 'Gestionar mi beat' }).waitFor()
})
await t('Beats', 'Filtros por género, BPM, productor, combinados, vacío y Limpiar', async page => {
  await page.goto(BASE + '/beats')
  const rows = () => page.locator('.beat-list > li').count()
  eq(await rows(), 8, 'inicial')
  await page.locator('#filter-genre').selectOption('Trap'); eq(await rows(), 2, 'Trap')
  await page.locator('#filter-bpm').fill('140'); eq(await rows(), 1, 'Trap+140')
  await page.locator('#filter-producer').selectOption('Kira Beats')
  await page.getByRole('heading', { name: 'No hay beats con esos filtros' }).waitFor()
  assert((await page.locator('.section-head p[aria-live]').textContent()).includes('0 beats'), 'contador 0')
  await page.getByRole('button', { name: 'Limpiar' }).click(); eq(await rows(), 8, 'limpiar')
  eq(await page.locator('#filter-bpm').inputValue(), '', 'limpiar bpm')
  await page.locator('#filter-producer').selectOption('Kira Beats'); eq(await rows(), 2, 'Kira')
})
await t('Beats', 'Enter en el filtro no recarga la página', async page => {
  await page.goto(BASE + '/beats')
  await page.locator('#filter-bpm').fill('140'); await page.locator('#filter-bpm').press('Enter')
  eq(await page.locator('.beat-list > li').count(), 1, 'filas'); eq(page.url(), BASE + '/beats', 'url')
})

/* ---------- DETALLE ---------- */
await t('Detalle', 'Visitante: datos, portada, escuchar, volver; sin gestionar ni formularios', async page => {
  await page.goto(BASE + '/beats/1')
  await page.getByRole('heading', { level: 1, name: 'Noche en Palermo' }).waitFor()
  const a = page.getByRole('link', { name: /Escuchar beat/ })
  assert((await a.getAttribute('href')).startsWith('https://'), 'href'); eq(await a.getAttribute('target'), '_blank', 'target')
  assert((await page.getByRole('img', { name: /Portada/ }).count()) === 1, 'portada')
  eq(await page.getByRole('heading', { name: 'Gestionar mi beat' }).count(), 0, 'gestionar')
  eq(await page.locator('#comment').count(), 0, 'form comentario'); eq(await page.locator('#topline-url').count(), 0, 'form topline')
  await page.getByRole('link', { name: 'Volver a beats' }).click(); await page.waitForURL(BASE + '/beats')
})
for (const r of ['/beats/999', '/beats/abc']) await t('Detalle', `${r} muestra "no existe" con link`, async page => {
  await page.goto(BASE + r)
  await page.getByRole('heading', { name: 'Beat no encontrado' }).waitFor()
  await page.getByRole('link', { name: 'Ir a Beats' }).click(); await page.waitForURL(BASE + '/beats')
})
await t('Detalle', 'Comentar: vacío, >500, éxito y contador en detalle y listado', async page => {
  await login(page, KIRA); await openBeat(page, 'Río de Plata')
  await page.getByRole('button', { name: 'Comentar' }).click()
  eq(await focusId(page), 'comment', 'foco')
  await page.locator('#comment').fill('x'.repeat(501)); await page.getByRole('button', { name: 'Comentar' }).click()
  assert((await page.locator('#comment-error').textContent()).includes('500'), '>500')
  await page.locator('#comment').fill('x'.repeat(500)); await page.getByRole('button', { name: 'Comentar' }).click()
  await page.getByText('Tu comentario se publicó.').waitFor()
  await page.locator('.comment').getByText('Kira Beats').waitFor()
  await nav(page, 'Beats')
  const row = page.locator('.beat-row', { hasText: 'Río de Plata' })
  assert((await row.textContent()).includes('1 comentario'), 'contador listado: ' + await row.textContent())
})
await t('Detalle', 'Discográfica puede comentar', async page => {
  await login(page, LABEL); await openBeat(page, 'Cumbia Lunar')
  await page.locator('#comment').fill('Buen tema'); await page.getByRole('button', { name: 'Comentar' }).click()
  await page.getByText('Tu comentario se publicó.').waitFor()
  assert((await body(page)).includes('Solo las cuentas de artista pueden subir toplines'), 'aviso topline')
})
await t('Detalle', 'Dueño edita: errores, guarda y se refleja en título y listado', async page => {
  await login(page, NOVA); await openBeat(page, 'Noche en Palermo')
  await page.locator('#edit-bpm').fill('300'); await page.locator('#edit-title').fill('')
  await page.getByRole('button', { name: 'Guardar cambios' }).click()
  eq(await focusId(page), 'edit-title', 'foco'); assert((await page.locator('#edit-bpm-error').textContent()).includes('entre 40 y 220'), 'bpm')
  await page.locator('#edit-title').fill('Noche Editada'); await page.locator('#edit-bpm').fill('141')
  await page.getByRole('button', { name: 'Guardar cambios' }).click()
  await page.getByText('Los cambios del beat se guardaron.').waitFor()
  await page.getByRole('heading', { level: 1, name: 'Noche Editada' }).waitFor()
  assert((await body(page)).includes('141 BPM'), 'bpm en header')
  await nav(page, 'Beats'); await page.getByRole('heading', { name: 'Noche Editada' }).waitFor()
})
await t('Detalle', 'Borrar en dos pasos: foco en Cancelar, cancela, confirma y limpia datos', async page => {
  await login(page, NOVA); await openBeat(page, 'Noche en Palermo')
  await page.getByRole('button', { name: 'Borrar beat' }).click()
  eq(await page.evaluate(() => document.activeElement.textContent), 'Cancelar', 'foco')
  await page.getByRole('button', { name: 'Cancelar' }).click()
  await page.getByRole('button', { name: 'Borrar beat' }).click()
  await page.getByRole('button', { name: 'Sí, borrar' }).click()
  await page.waitForURL(BASE + '/beats')
  await page.getByText('El beat "Noche en Palermo" se borró.').waitFor()
  assert((await page.locator('.section-head p[aria-live]').textContent()).includes('7 beats'), 'contador')
  await nav(page, 'Colaboraciones')
  eq(await page.getByRole('link', { name: 'Noche en Palermo' }).count(), 0, 'sigue en Colaboraciones')
})
await t('Detalle', 'Aceptar/Rechazar del dueño se refleja en Colaboraciones', async page => {
  await login(page, NOVA); await openBeat(page, 'Noche en Palermo')
  await page.getByRole('button', { name: /Aceptar topline de Kira Beats/ }).click()
  const row = page.locator('#topline-1')
  assert((await row.textContent()).includes('Aceptada'), 'badge'); eq(await focusId(page), 'topline-1', 'foco en fila')
  eq(await row.getByRole('button', { name: /Rechazar/ }).count(), 0, 'botones siguen')
  eq(await page.locator('#topline-url').count(), 0, 'dueño puede subir a su beat')
  await nav(page, 'Colaboraciones')
  assert((await page.locator('#proposal-1').textContent()).includes('Aceptada'), 'en colaboraciones')
})
await t('Detalle', 'Otro artista: sin gestionar, sube topline (validación), lo borra, y al dueño le llega', async page => {
  await login(page, KIRA); await openBeat(page, 'Tarde de Domingo')
  eq(await page.getByRole('heading', { name: 'Gestionar mi beat' }).count(), 0, 'gestionar')
  await page.locator('#topline-url').fill('http://x.com'); await page.getByRole('button', { name: 'Subir topline' }).click()
  eq(await focusId(page), 'topline-url', 'foco')
  await page.locator('#topline-url').fill('https://soundcloud.com/k/qa'); await page.getByRole('button', { name: 'Subir topline' }).click()
  await page.getByText('Listo: topline subido. Se creó una propuesta de colaboración pendiente.').waitFor()
  await page.locator('.topline-row', { hasText: 'Kira Beats' }).getByRole('button', { name: 'Borrar topline' }).waitFor()
  await logout(page); await loginSpa(page, NOVA); await nav(page, 'Colaboraciones')
  await page.locator('.collab-group', { hasText: 'Tarde de Domingo' }).locator('.topline-row', { hasText: 'Kira Beats' }).filter({ hasText: 'Pendiente' }).waitFor()
})

/* ---------- COLABORACIONES ---------- */
await t('Colaboraciones', 'Visitante y discográfica ven su aviso', async page => {
  await page.goto(BASE + '/colaboraciones')
  await page.getByText('Tenés que iniciar sesión para ver tus colaboraciones.').waitFor()
  await page.locator('main').getByRole('link', { name: 'Entrar' }).click(); await page.waitForURL(BASE + '/login')
  await login(page, LABEL); await nav(page, 'Colaboraciones')
  await page.getByText('Las colaboraciones son solo para artistas.').waitFor()
})
await t('Colaboraciones', 'Stats, agrupado por beat y filtro de estado', async page => {
  await login(page, NOVA); await nav(page, 'Colaboraciones')
  const txt = await page.locator('.page-header').textContent()
  assert(/Propuestas recibidas\s*4/i.test(txt) && /Pendientes\s*1/i.test(txt) && /Toplines subidos\s*2/i.test(txt), 'stats: ' + txt)
  eq(await page.locator('.collab-group').count(), 2, 'grupos')
  const n = () => page.locator('[id^=proposal-]').count()
  eq(await n(), 4, 'todas')
  await page.locator('#status-filter').selectOption('PENDING'); eq(await n(), 1, 'pend')
  await page.locator('#status-filter').selectOption('ACCEPTED'); eq(await n(), 1, 'acep')
  await page.locator('#status-filter').selectOption('REJECTED'); eq(await n(), 2, 'rech')
})
await t('Colaboraciones', 'Aceptar, rechazar, quitar y borrar en dos pasos actualizan stats y foco', async page => {
  await login(page, NOVA); await nav(page, 'Colaboraciones')
  await page.locator('#proposal-1').getByRole('button', { name: /Aceptar/ }).click()
  assert((await page.locator('#proposal-1').textContent()).includes('Aceptada'), 'aceptar')
  eq(await focusId(page), 'proposal-1', 'foco fila')
  assert(/Pendientes\s*0/i.test(await page.locator('.page-header').textContent()), 'stat pendientes')
  await page.locator('#proposal-3').getByRole('button', { name: /Quitar propuesta/ }).click()
  eq(await page.locator('#proposal-3').count(), 0, 'quitar'); eq(await focusId(page), 'proposal-3' === 0 ? '' : 'proposals-title', 'foco título')
  await page.locator('#proposal-2').getByRole('button', { name: 'Borrar topline' }).click()
  eq(await page.evaluate(() => document.activeElement.textContent), 'Cancelar', 'foco cancelar')
  await page.locator('#proposal-2').getByRole('button', { name: 'Sí, borrar' }).click()
  eq(await page.locator('#proposal-2').count(), 0, 'borrar')
  assert(/Propuestas recibidas\s*2/i.test(await page.locator('.page-header').textContent()), 'stat recibidas')
})
await t('Colaboraciones', 'Filtro en Pendiente: al aceptar la última aparece estado vacío y foco en título', async page => {
  await login(page, NOVA); await nav(page, 'Colaboraciones')
  await page.locator('#status-filter').selectOption('PENDING')
  await page.locator('#proposal-1').getByRole('button', { name: /Aceptar/ }).click()
  await page.getByText('No hay propuestas con ese estado').waitFor()
  eq(await focusId(page), 'proposals-title', 'foco')
})
await t('Colaboraciones', 'Mis toplines: editar con validación, cancelar, guardar y borrar', async page => {
  await login(page, NOVA); await nav(page, 'Colaboraciones')
  await page.locator('#edit-5').click()
  eq(await page.locator('#topline-url').inputValue(), 'https://soundcloud.com/kira/cumbia-lunar-v2', 'valor')
  await page.locator('#topline-url').fill('http://a.b'); await page.getByRole('button', { name: 'Guardar' }).click()
  eq(await focusId(page), 'topline-url', 'foco error')
  await page.locator('.inline-edit').getByRole('button', { name: 'Cancelar' }).click()
  eq(await focusId(page), 'edit-5', 'foco tras cancelar')
  await page.locator('#edit-5').click()
  await page.locator('#topline-url').fill('https://soundcloud.com/nuevo/link'); await page.getByRole('button', { name: 'Guardar' }).click()
  await page.getByText('Listo: el link se actualizó.').waitFor()
  await page.locator('.my-topline').first().getByRole('button', { name: 'Borrar topline' }).click()
  await page.locator('.my-topline').first().getByRole('button', { name: 'Sí, borrar' }).click()
  eq(await page.locator('.my-topline').count(), 1, 'borrado')
  eq(await focusId(page), 'my-toplines-title', 'foco título')
})
await t('Colaboraciones', 'El link editado se ve en el detalle del beat', async page => {
  await login(page, NOVA); await nav(page, 'Colaboraciones')
  await page.locator('#edit-5').click()
  await page.locator('#topline-url').fill('https://soundcloud.com/nuevo/link'); await page.getByRole('button', { name: 'Guardar' }).click()
  await page.getByText('Listo: el link se actualizó.').waitFor()
  await openBeat(page, 'Cumbia Lunar')
  eq(await page.locator('#topline-5 a').first().getAttribute('href'), 'https://soundcloud.com/nuevo/link', 'href')
})

/* ---------- DESAFIOS ---------- */
await t('Desafíos', 'Destacado, lista de 3 con estado, entregas y podio', async page => {
  await page.goto(BASE + '/desafios')
  await page.getByRole('heading', { level: 1, name: 'Mejor beat de trap' }).waitFor()
  const txt = await body(page)
  for (const k of ['En votación', 'Abierto', 'Cerrado']) assert(new RegExp(k, 'i').test(txt), 'falta ' + k)
  eq(await page.locator('#entregas .topline-row').count(), 3, 'entregas')
})
await t('Desafíos', 'Podio: <ol> en orden 1-2-3 y visual 2-1-3', async page => {
  await page.goto(BASE + '/desafios')
  const places = await page.locator('.podium > li .podium-name').allTextContents()
  eq(places.join(), 'DJ Nova,Kira Beats,Tomi Loop', 'orden DOM')
  const xs = await page.locator('.podium > li').evaluateAll(l => l.map(e => e.getBoundingClientRect().x))
  assert(xs[1] < xs[0] && xs[0] < xs[2], 'orden visual ' + xs)
})
await t('Desafíos', 'Podio en 375 px sigue siendo legible (no desborda)', async page => {
  await page.goto(BASE + '/desafios')
  assert(await noHScroll(page), 'desborda')
}, { width: 375, height: 812 })
await t('Desafíos', 'Botón "Ver entregas" lleva a la sección', async page => {
  await page.goto(BASE + '/desafios')
  await page.getByRole('link', { name: 'Ver entregas' }).click()
  await page.waitForTimeout(400)
  const top = await page.locator('#entregas').evaluate(e => e.getBoundingClientRect().top)
  assert(Math.abs(top) < 150, `no scrolleó: la sección quedó a ${Math.round(top)}px del borde superior`)
})
await t('Desafíos', 'Link "Votar" lleva al formulario', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  await page.locator('#entregas').getByRole('link', { name: 'Votar' }).first().click()
  await page.waitForTimeout(400)
  const top = await page.locator('#votar').evaluate(e => e.getBoundingClientRect().top)
  assert(Math.abs(top) < 150, `no scrolleó, quedó a ${Math.round(top)}px`)
})
await t('Desafíos', 'Link "Votar" de una entrega preselecciona ESA entrega', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  await page.locator('#entregas .topline-row', { hasText: 'Noche en Palermo' }).getByRole('link', { name: 'Votar' }).click()
  await page.waitForTimeout(200)
  const sel = await page.locator('#vote-submission option:checked').textContent()
  assert(sel.includes('Noche en Palermo'), `tocaste Votar en "Noche en Palermo" pero el formulario quedó en "${sel}"`)
})
await t('Desafíos', 'Visitante: pide entrar y no ve el formulario', async page => {
  await page.goto(BASE + '/desafios')
  eq(await page.locator('.vote-form').count(), 0, 'form')
  await page.getByRole('heading', { name: 'Entrá para votar' }).waitFor()
})
await t('Desafíos', 'Votar: sin puntaje da error, con 8 espera y confirma; pasa a Ya votaste', async page => {
  await login(page, NOVA); await nav(page, 'Desafíos')
  assert((await page.locator('#entregas .topline-row', { hasText: 'Noche en Palermo' }).textContent()).includes('Es tu entrega'), 'propia')
  assert((await page.locator('#entregas .topline-row', { hasText: 'Calle Oscura' }).textContent()).includes('Ya votaste'), 'ya votada')
  await page.getByRole('button', { name: 'Enviar voto' }).click()
  assert((await msgs(page)).join().includes('Elegí un puntaje'), 'error')
  eq(await focusId(page), 'vote-score', 'foco')
  await page.locator('.score-option', { hasText: /^8$/ }).click()
  await page.locator('#vote-comment').fill('Muy bueno')
  await page.getByRole('button', { name: 'Enviar voto' }).click()
  await page.getByRole('button', { name: 'Enviando…' }).waitFor()
  await page.getByText('Tu voto se registró.').waitFor()
  assert((await page.locator('#entregas .topline-row', { hasText: 'Madrugada Fría' }).textContent()).includes('Ya votaste'), 'ya votaste')
  await page.getByRole('heading', { name: 'No te queda ninguna entrega por votar' }).waitFor()
})
await t('Desafíos', 'Puntaje con teclado: Tab entra al grupo y flechas eligen', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  await page.locator('#vote-submission').focus()
  await page.keyboard.press('Tab')
  await page.keyboard.press('ArrowRight'); await page.keyboard.press('ArrowRight')
  const v = await page.locator('input[name=vote-score]:checked').inputValue()
  assert(['2', '3'].includes(v), 'valor ' + v)
  const ring = await page.evaluate(() => { const l = document.activeElement.closest('label'); const s = l.querySelector('span'); const a = getComputedStyle(s).outlineStyle, b = getComputedStyle(l).outlineStyle; return a !== 'none' || b !== 'none' })
  assert(ring, 'sin foco visible en el puntaje')
})
await t('Desafíos', 'Dos votos seguidos (discográfica) reconfirman', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  for (let i = 0; i < 2; i++) {
    await page.locator('.score-option', { hasText: /^5$/ }).click()
    await page.getByRole('button', { name: 'Enviar voto' }).click()
    await page.getByText('Tu voto se registró.').waitFor()
    await page.waitForTimeout(700)
  }
  await page.getByRole('heading', { name: 'No te queda ninguna entrega por votar' }).waitFor()
})

await t('Desafíos', 'El voto queda en la entrega elegida con el link Votar', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  await page.locator('#entregas .topline-row', { hasText: 'Noche en Palermo' }).getByRole('link', { name: /Votar/ }).click()
  await page.locator('.score-option', { hasText: /^7$/ }).click()
  await page.getByRole('button', { name: 'Enviar voto' }).click()
  await page.getByText('Tu voto se registró.').waitFor()
  assert((await page.locator('#entregas .topline-row', { hasText: 'Noche en Palermo' }).textContent()).includes('Ya votaste'), 'no quedó en Noche en Palermo')
  assert(!(await page.locator('#entregas .topline-row', { hasText: 'Madrugada Fría' }).textContent()).includes('Ya votaste'), 'votó otra entrega')
})
await t('Desafíos', 'El puntaje elegido no se pierde al cambiar de entrega', async page => {
  await login(page, LABEL); await nav(page, 'Desafíos')
  await page.locator('.score-option', { hasText: /^9$/ }).click()
  await page.locator('#vote-submission').selectOption({ label: 'Noche en Palermo, DJ Nova' })
  eq(await page.locator('input[name=vote-score]:checked').inputValue(), '9', 'puntaje')
})
await t('Desafíos', 'Podio y ranking usan los puntos de premio; las fechas abiertas no están vencidas', async page => {
  await page.goto(BASE + '/desafios')
  eq((await page.locator('.podium-points').allTextContents()).join(), '500 puntos,300 puntos,150 puntos', 'podio')
  const open = await page.locator('.topline-row time').allTextContents()
  const parse = x => { const [d, m, y] = x.split('/').map(Number); return new Date(y, m - 1, d, 23, 59) }
  for (const f of open.slice(0, 2)) assert(parse(f) >= new Date(), 'fecha vencida ' + f)
  await page.goto(BASE + '/ranking')
  const pts = (await page.locator('.ranking-points').allTextContents()).slice(1).map(Number)
  for (const x of pts) assert(x % 50 === 0, 'puntos que no salen de premios ' + x)
  eq(JSON.stringify(pts), JSON.stringify([...pts].sort((a, b) => b - a)), 'orden')
})

/* ---------- RANKING ---------- */
await t('Ranking', 'Tabla semántica, 10 filas y (vos)', async page => {
  await page.goto(BASE + '/ranking')
  assert(await page.locator('caption').count() === 1, 'caption')
  eq(await page.locator('thead th[scope=col]').count(), 3, 'th'); eq(await page.locator('tbody tr').count(), 10, 'filas')
  assert((await body(page)).includes('Entrá con tu cuenta'), 'hint')
  await login(page, NOVA); await nav(page, 'Ranking')
  assert((await page.locator('tr.ranking-me').textContent()).includes('(vos)'), 'vos')
})
await t('Ranking', 'A 375 px la tabla entra sin scroll interno', async page => {
  await page.goto(BASE + '/ranking')
  const over = await page.locator('.table-scroll').evaluate(e => e.scrollWidth - e.clientWidth)
  assert(over <= 0, `la tabla desborda ${over}px`)
}, { width: 375, height: 812 })

/* ---------- PREMIUM ---------- */
async function toPayment(page) {
  await nav(page, 'Premium')
  await page.getByRole('button', { name: 'Continuar con Premium' }).click()
}
async function fillCard(page, n = '4111 1111 1111 1111', exp = '1028', cvv = '123') {
  await page.locator('#cardName').fill('Dj Nova'); await page.locator('#cardNumber').fill(n)
  await page.locator('#cardExpiry').fill(exp); await page.locator('#cardCvv').fill(cvv)
}
await t('Premium', 'Sin sesión manda a Login', async page => {
  await page.goto(BASE + '/premium'); await page.waitForURL(BASE + '/login')
})
await t('Premium', 'Paso 1: precios, stepper con aria-current', async page => {
  await login(page, NOVA); await nav(page, 'Premium')
  const txt = await body(page)
  assert(txt.includes('$15.000') && txt.includes('$0'), 'precios')
  assert((await page.locator('[aria-current=step]').textContent()).includes('Plan'), 'step')
})
await t('Premium', 'Discográfica NO puede comprar Premium (el backend responde 403 a no artistas)', async page => {
  await login(page, LABEL); await nav(page, 'Premium')
  eq(await page.getByRole('button', { name: 'Continuar con Premium' }).count(), 0, 'botón Continuar con Premium para discográfica')
  await page.getByText('Premium es solo para artistas.').waitFor()
  await page.locator('main').getByRole('link', { name: 'Ver desafíos' }).click(); await page.waitForURL(BASE + '/desafios')
})
await t('Premium', 'Paso 2: autocomplete, types, vacío y validaciones de tarjeta', async page => {
  await login(page, NOVA); await toPayment(page)
  for (const [id, ac] of [['cardName', 'cc-name'], ['cardNumber', 'cc-number'], ['cardExpiry', 'cc-exp'], ['cardCvv', 'cc-csc']]) eq(await page.locator('#' + id).getAttribute('autocomplete'), ac, id)
  eq(await page.locator('#cardCvv').getAttribute('type'), 'password', 'cvv type')
  const pay = page.getByRole('button', { name: /^Pagar/ })
  await pay.click(); eq((await msgs(page)).length, 5, 'errores vacío'); eq(await focusId(page), 'cardName', 'foco')
  await fillCard(page, '4111 1111 1111 1112', '0926', '12'); await pay.click()
  const m = (await msgs(page)).join()
  assert(m.includes('no es válido') && m.includes('vencida') && m.includes('3 o 4'), m)
  await fillCard(page); await pay.click()
  assert((await msgs(page)).join().includes('Política de privacidad'), 'privacidad'); eq(await focusId(page), 'privacy', 'foco privacidad')
})
await t('Premium', 'Volver descarta los datos de tarjeta', async page => {
  await login(page, NOVA); await toPayment(page); await fillCard(page)
  await page.getByRole('button', { name: 'Volver' }).click()
  await page.getByRole('button', { name: 'Continuar con Premium' }).click()
  eq(await page.locator('#cardNumber').inputValue(), '', 'número sigue')
})
await t('Premium', 'Pago OK: espera, •••• 1111, sin datos en storage/DOM/consola, plan queda tras F5', async page => {
  const logs = []
  page.on('console', m => logs.push(m.text()))
  await login(page, NOVA); await toPayment(page); await fillCard(page)
  await page.locator('#privacy').check()
  await page.getByRole('button', { name: /^Pagar/ }).click()
  await page.getByRole('button', { name: 'Enviando…' }).waitFor()
  await page.getByRole('heading', { level: 1, name: 'Bienvenido a Premium' }).waitFor()
  assert((await body(page)).includes('•••• 1111'), 'máscara')
  const html = await page.content()
  assert(!html.includes('4111') && !html.includes('Dj Nova'), 'tarjeta en DOM')
  const st = await page.evaluate(() => JSON.stringify({ ...sessionStorage }) + JSON.stringify({ ...localStorage }) + document.cookie)
  assert(!/4111|123"|@/.test(st), 'storage: ' + st)
  assert(!logs.join().includes('4111'), 'consola')
  await page.reload()
  await page.getByText('Tu plan actual').waitFor()
  eq(await page.getByRole('button', { name: 'Continuar con Premium' }).count(), 0, 'sigue ofreciendo')
  await page.getByRole('link', { name: 'Publicar un beat' }).count()
})
await t('Premium', 'Botones de confirmación navegan', async page => {
  await login(page, NOVA); await toPayment(page); await fillCard(page); await page.locator('#privacy').check()
  await page.getByRole('button', { name: /^Pagar/ }).click()
  await page.getByRole('link', { name: 'Ver desafíos' }).click(); await page.waitForURL(BASE + '/desafios')
})
await t('Premium', 'El plan Premium sobrevive a salir y volver a entrar', async page => {
  await login(page, NOVA); await toPayment(page); await fillCard(page); await page.locator('#privacy').check()
  await page.getByRole('button', { name: /^Pagar/ }).click()
  await page.getByRole('heading', { level: 1, name: 'Bienvenido a Premium' }).waitFor()
  await nav(page, 'Inicio'); await logout(page); await page.waitForTimeout(100); await loginSpa(page, NOVA); await nav(page, 'Premium')
  eq(await page.getByRole('button', { name: 'Continuar con Premium' }).count(), 0, 'volvió a Free: le ofrece comprar de nuevo')
})
await t('Premium', 'Salir estando en /premium lleva al inicio (como en el resto de las páginas)', async page => {
  await login(page, NOVA); await nav(page, 'Premium'); await logout(page)
  await page.waitForTimeout(300)
  eq(new URL(page.url()).pathname, '/', 'ruta tras Salir')
})

await t('Premium', 'Premium sobrevive a F5 + salir + entrar; otro usuario sigue en Free', async page => {
  await login(page, NOVA); await toPayment(page); await fillCard(page); await page.locator('#privacy').check()
  await page.getByRole('button', { name: /^Pagar/ }).click()
  await page.getByRole('heading', { level: 1, name: 'Bienvenido a Premium' }).waitFor()
  await page.reload(); await page.getByText('Tu plan actual').waitFor()
  await nav(page, 'Inicio'); await logout(page); await page.waitForTimeout(100); await loginSpa(page, NOVA); await nav(page, 'Premium')
  eq(await page.getByRole('button', { name: 'Continuar con Premium' }).count(), 0, 'perdió Premium tras F5')
  await nav(page, 'Inicio'); await logout(page); await page.waitForTimeout(100); await loginSpa(page, KIRA); await nav(page, 'Premium')
  eq(await page.getByRole('button', { name: 'Continuar con Premium' }).count(), 1, 'Kira no debería ser Premium')
})
await t('Global', 'Salir desde Inicio, Beats y Colaboraciones lleva al inicio sin sesión', async page => {
  await login(page, NOVA); await logout(page)
  await page.getByRole('link', { name: 'Entrar' }).first().waitFor()
  for (const label of ['Beats', 'Colaboraciones']) {
    await loginSpa(page, NOVA); await nav(page, label); await logout(page)
    await page.getByRole('link', { name: 'Entrar' }).first().waitFor()
    eq(new URL(page.url()).pathname, '/', label)
  }
  eq(await page.evaluate(() => sessionStorage.length), 0, 'sessionStorage')
})

/* ---------- LOGIN / REGISTRO / CONTACTO ---------- */
await t('Login', 'Vacío, email inválido, credenciales mal y bien; con sesión redirige', async page => {
  await page.goto(BASE + '/login')
  const go = page.getByRole('button', { name: 'Entrar' })
  await go.click(); eq((await msgs(page)).length, 2, 'errores'); eq(await focusId(page), 'email', 'foco')
  await page.locator('#email').fill('sinarroba'); await page.locator('#password').fill('x'); await go.click()
  assert((await page.locator('#email-error').textContent()).includes('no es válido'), 'formato')
  await page.locator('#email').fill(NOVA); await page.locator('#password').fill('mala'); await go.click()
  await page.getByText('El email o la contraseña no son correctos.').waitFor()
  eq(await page.locator('#password').inputValue(), '', 'contraseña borrada'); eq(await focusId(page), 'password', 'foco')
  await page.locator('#email').fill(' ARTISTA@mgw.com '); await page.locator('#password').fill(PASS); await go.click()
  await page.waitForURL(BASE + '/')
  assert((await body(page)).includes('DJ Nova'), 'nombre')
  const st = await page.evaluate(() => sessionStorage.getItem('mgw-session'))
  assert(!/@|password|mgw12345/.test(st), 'sensible en sesión: ' + st)
  await page.goto(BASE + '/login'); await page.waitForURL(BASE + '/')
})
await t('Login', 'Links: Registrate, logo al inicio', async page => {
  await page.goto(BASE + '/login')
  await page.getByRole('link', { name: 'Registrate' }).click(); await page.waitForURL(BASE + '/registro')
  await page.getByRole('link', { name: /MGW/ }).first().click(); await page.waitForURL(BASE + '/')
})
await t('Registro', 'Vacío, contraseña corta, privacidad; autocomplete; Artista entra y puede publicar', async page => {
  await page.goto(BASE + '/registro')
  const go = page.getByRole('button', { name: 'Crear cuenta' })
  await go.click(); eq((await msgs(page)).length, 4, 'errores'); eq(await focusId(page), 'name', 'foco')
  eq(await page.locator('#password').getAttribute('autocomplete'), 'new-password', 'ac')
  await page.locator('#name').fill('Luna'); await page.locator('#email').fill('l@t.co'); await page.locator('#password').fill('1234567'); await page.locator('#privacy').check(); await go.click()
  assert((await page.locator('#password-error').textContent()).includes('8'), 'corta')
  await page.locator('#password').fill('12345678'); await go.click()
  await page.getByRole('button', { name: 'Enviando…' }).waitFor()
  await page.waitForURL(BASE + '/')
  await nav(page, 'Beats'); await page.locator('#title').waitFor()
})
await t('Registro', 'Discográfica: se elige con teclado y queda como LABEL', async page => {
  await page.goto(BASE + '/registro')
  await page.locator('input[name=role]').first().focus(); await page.keyboard.press('ArrowRight')
  await page.locator('#name').fill('Sello QA'); await page.locator('#email').fill('s@t.co'); await page.locator('#password').fill('12345678'); await page.locator('#privacy').check()
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.waitForURL(BASE + '/')
  assert((await page.evaluate(() => sessionStorage.getItem('mgw-session'))).includes('LABEL'), 'rol')
  await nav(page, 'Colaboraciones'); await page.getByText('Las colaboraciones son solo para artistas.').waitFor()
})
await t('Registro', 'Después de registrarme, salir y volver a entrar con esa cuenta funciona', async page => {
  await page.goto(BASE + '/registro')
  await page.locator('#name').fill('Luna'); await page.locator('#email').fill('luna@t.co'); await page.locator('#password').fill('12345678'); await page.locator('#privacy').check()
  await page.getByRole('button', { name: 'Crear cuenta' }).click(); await page.waitForURL(BASE + '/')
  await logout(page)
  await page.getByRole('link', { name: 'Entrar' }).first().click()
  await page.locator('#email').fill('luna@t.co'); await page.locator('#password').fill('12345678')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.waitForURL(BASE + '/', { timeout: 2000 }).catch(() => { throw new Error('la cuenta recién creada da "email o contraseña no son correctos"') })
})
await t('Registro', 'Email repetido da error (de demo y de cuenta nueva) y permite corregir', async page => {
  await page.goto(BASE + '/registro')
  await page.locator('#name').fill('Otra'); await page.locator('#email').fill(' ARTISTA@mgw.com'); await page.locator('#password').fill('12345678'); await page.locator('#privacy').check()
  await page.getByRole('button', { name: 'Crear cuenta' }).click()
  await page.locator('#email-error').getByText('Ya hay una cuenta').waitFor()
  eq(await focusId(page), 'email', 'foco')
  await page.locator('#email').fill('otra@t.co'); await page.getByRole('button', { name: 'Crear cuenta' }).click(); await page.waitForURL(BASE + '/')
  const st = await page.evaluate(() => JSON.stringify({ ...sessionStorage }) + JSON.stringify({ ...localStorage }))
  assert(!/otra@|12345678/.test(st), 'dato sensible en storage: ' + st)
})
await t('Registro', 'La cuenta nueva conserva su rol y plan al volver a entrar', async page => {
  await page.goto(BASE + '/registro')
  await page.locator('input[name=role]').first().focus(); await page.keyboard.press('ArrowRight')
  await page.locator('#name').fill('Sello'); await page.locator('#email').fill('sello@t.co'); await page.locator('#password').fill('12345678'); await page.locator('#privacy').check()
  await page.getByRole('button', { name: 'Crear cuenta' }).click(); await page.waitForURL(BASE + '/')
  await logout(page); await page.getByRole('link', { name: 'Entrar' }).first().click()
  await page.locator('#email').fill('sello@t.co'); await page.locator('#password').fill('12345678'); await page.getByRole('button', { name: 'Entrar' }).click(); await page.waitForURL(BASE + '/')
  assert((await page.evaluate(() => sessionStorage.getItem('mgw-session'))).includes('LABEL'), 'rol')
})
await t('Registro', 'El link de Política de privacidad abre sin perder lo escrito', async (page, ctx) => {
  await page.goto(BASE + '/registro')
  await page.locator('#name').fill('Luna')
  const [tab] = await Promise.all([ctx.waitForEvent('page'), page.locator('form').getByRole('link', { name: 'Política de privacidad' }).click()])
  await tab.waitForLoadState(); assert(tab.url().endsWith('/privacidad'), 'url')
  eq(await page.locator('#name').inputValue(), 'Luna', 'perdió')
})
await t('Contacto', 'Vacío, mensaje corto, privacidad, espera, éxito, Enviar otro', async page => {
  await page.goto(BASE + '/contacto')
  const go = page.getByRole('button', { name: 'Enviar mensaje' })
  await go.click(); eq((await msgs(page)).length, 4, 'errores'); eq(await focusId(page), 'name', 'foco')
  eq((await page.locator('#reason option').allTextContents()).join(), 'Consulta,Sugerencia,Reclamo', 'motivos')
  await page.locator('#name').fill('A'); await page.locator('#email').fill('a@b.co'); await page.locator('#message').fill('corto'); await page.locator('#privacy').check(); await go.click()
  eq(await focusId(page), 'message', 'foco mensaje')
  await page.locator('#message').fill('Un mensaje suficientemente largo'); await go.click()
  await page.getByRole('button', { name: 'Enviando…' }).waitFor()
  await page.getByText('Recibimos tu mensaje. Te respondemos por email.').waitFor()
  eq(await page.locator('form').count(), 0, 'form sigue')
  await page.getByRole('button', { name: 'Enviar otro' }).click()
  eq(await page.locator('#name').inputValue(), '', 'limpio'); assert(!(await page.locator('#privacy').isChecked()), 'privacidad limpia')
})

/* ---------- ACCESIBILIDAD (axe) ---------- */
async function axe(page) {
  await page.addScriptTag({ content: axeSrc })
  return page.evaluate(async () => {
    const r = await window.axe.run(document, { runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] })
    return r.violations.map(v => `${v.id}[${v.impact}] x${v.nodes.length}: ${v.nodes[0].target} — ${v.nodes[0].failureSummary?.split('\n')[1]?.trim().slice(0, 110)}`)
  })
}
for (const [w, h] of [[375, 812], [1280, 900]]) {
  for (const r of ROUTES) {
    await t('Axe', `${r} a ${w}px sin violaciones WCAG A/AA (visitante)`, async page => {
      await page.goto(BASE + r); await page.waitForLoadState('networkidle')
      const v = await axe(page); eq(v.length, 0, v.join(' || '))
    }, { width: w, height: h })
  }
}
for (const r of ['/beats', '/beats/1', '/colaboraciones', '/desafios', '/premium', '/ranking']) {
  await t('Axe', `${r} con sesión de artista`, async page => {
    await login(page, NOVA); await page.goto(BASE + r); await page.waitForLoadState('networkidle')
    const v = await axe(page); eq(v.length, 0, v.join(' || '))
  })
}
await t('Axe', 'Formularios con errores visibles (registro, pago)', async page => {
  await page.goto(BASE + '/registro'); await page.getByRole('button', { name: 'Crear cuenta' }).click()
  const a = await axe(page)
  await login(page, NOVA); await toPayment(page); await page.getByRole('button', { name: /^Pagar/ }).click()
  const b = await axe(page)
  eq(a.length + b.length, 0, [...a, ...b].join(' || '))
})

await browser.close()
const fail = results.filter(r => !r.ok)
const by = {}
for (const r of results) { by[r.area] ??= [0, 0]; by[r.area][r.ok ? 0 : 1]++ }
console.log(Object.entries(by).map(([a, [o, f]]) => `${a}: ${o} ok, ${f} fallan`).join('\n'))
console.log(`\nTOTAL ${results.length} | OK ${results.length - fail.length} | FALLAN ${fail.length}\n`)
for (const f of fail) console.log(`✗ [${f.area}] ${f.name}\n   ${f.err}\n`)
writeFileSync(new URL('./results.json', import.meta.url), JSON.stringify(results, null, 1))
