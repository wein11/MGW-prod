// Datos simulados de toda la app. En esta entrega no hay backend: la rúbrica
// permite datos locales. Están armados con los mismos ejemplos que el
// wireframe para que cada pantalla se vea igual que en el diseño.
// Las fechas van en formato ISO (AAAA-MM-DD); se muestran con formatDate().

import coverNoche from '../assets/covers/cover-1.svg'
import coverCumbia from '../assets/covers/cover-2.svg'
import coverRio from '../assets/covers/cover-3.svg'
import coverMadrugada from '../assets/covers/cover-4.svg'

export const GENRES = ['Trap', 'Cumbia', 'Boom bap', 'Lo-fi', 'Reggaetón', 'R&B']

// Usuarios de demo para el login simulado. Las contraseñas son de prueba y
// solo existen en este archivo: la sesión nunca guarda email ni contraseña.
export const users = [
  { name: 'DJ Nova', email: 'artista@mgw.com', password: 'mgw12345', role: 'ARTIST', city: 'CABA' },
  { name: 'Kira Beats', email: 'kira@mgw.com', password: 'mgw12345', role: 'ARTIST', city: 'Rosario' },
  { name: 'Sello Sur', email: 'discografica@mgw.com', password: 'mgw12345', role: 'LABEL', city: 'CABA' },
]

export const beats = [
  {
    id: 1, title: 'Noche en Palermo', genre: 'Trap', bpm: 140, key: 'Dm',
    audioUrl: 'https://soundcloud.com/djnova/noche-en-palermo', cover: coverNoche,
    ownerName: 'DJ Nova', publishedAt: '2026-10-12',
    description: 'Trap oscuro con 808 largos y una melodía de piano en menor. Pensado para una voz melódica.',
  },
  {
    id: 2, title: 'Cumbia Lunar', genre: 'Cumbia', bpm: 98, key: 'Am',
    audioUrl: 'https://soundcloud.com/kirabeats/cumbia-lunar', cover: coverCumbia,
    ownerName: 'Kira Beats', publishedAt: '2026-10-10',
    description: 'Cumbia con sintes espaciales y güira bien al frente.',
  },
  {
    id: 3, title: 'Río de Plata', genre: 'Boom bap', bpm: 90, key: 'Gm',
    audioUrl: 'https://soundcloud.com/tomiloop/rio-de-plata', cover: coverRio,
    ownerName: 'Tomi Loop', publishedAt: '2026-10-08',
    description: 'Boom bap con sample de bandoneón y batería con swing.',
  },
  {
    id: 4, title: 'Madrugada Fría', genre: 'Trap', bpm: 150, key: 'Cm',
    audioUrl: 'https://soundcloud.com/kirabeats/madrugada-fria', cover: coverMadrugada,
    ownerName: 'Kira Beats', publishedAt: '2026-10-05',
    description: 'Trap rápido, hi-hats en tresillo y pads fríos.',
  },
  {
    id: 5, title: 'Tarde de Domingo', genre: 'Lo-fi', bpm: 82, key: 'Am',
    audioUrl: 'https://soundcloud.com/djnova/tarde-de-domingo', cover: coverRio,
    ownerName: 'DJ Nova', publishedAt: '2026-10-03',
    description: 'Lo-fi tranquilo con guitarra criolla y vinilo de fondo.',
  },
  {
    id: 6, title: 'Baile en el Barrio', genre: 'Cumbia', bpm: 102, key: 'Em',
    audioUrl: 'https://soundcloud.com/tomiloop/baile-en-el-barrio', cover: coverCumbia,
    ownerName: 'Tomi Loop', publishedAt: '2026-09-29',
    description: 'Cumbia villera clásica con teclado y bajo saltado.',
  },
  {
    id: 7, title: 'Perreo Austral', genre: 'Reggaetón', bpm: 94, key: 'F#m',
    audioUrl: 'https://soundcloud.com/lunasur/perreo-austral', cover: coverNoche,
    ownerName: 'Luna Sur', publishedAt: '2026-09-25',
    description: 'Dembow con percusión patagónica y voces cortadas.',
  },
  {
    id: 8, title: 'Neón Suave', genre: 'R&B', bpm: 72, key: 'Ebm',
    audioUrl: 'https://soundcloud.com/betosampler/neon-suave', cover: coverMadrugada,
    ownerName: 'Beto Sampler', publishedAt: '2026-09-20',
    description: 'R&B lento con Rhodes y bajo sintetizado.',
  },
]

export const comments = [
  { id: 1, beatId: 1, authorName: 'Kira Beats', text: 'El bajo está enorme, me encantó la intro.', createdAt: '2026-10-14' },
  { id: 2, beatId: 1, authorName: 'Tomi Loop', text: 'Le sumaría un poco más de aire al hi-hat.', createdAt: '2026-10-15' },
  { id: 3, beatId: 1, authorName: 'Luna Sur', text: 'Ideal para una topline melódica.', createdAt: '2026-10-16' },
  { id: 4, beatId: 2, authorName: 'DJ Nova', text: 'Los sintes le quedan perfectos.', createdAt: '2026-10-11' },
  { id: 5, beatId: 4, authorName: 'Tomi Loop', text: 'Ese tresillo de hi-hat es una locura.', createdAt: '2026-10-06' },
]

// Un topline es la voz de un artista sobre un beat ajeno. Al subirlo se crea
// una propuesta de colaboración que el dueño del beat acepta o rechaza.
export const TOPLINE_STATUS = { PENDING: 'PENDING', ACCEPTED: 'ACCEPTED', REJECTED: 'REJECTED' }

export const toplines = [
  { id: 1, beatId: 1, artistName: 'Kira Beats', audioUrl: 'https://soundcloud.com/kirabeats/topline-noche', status: 'PENDING', createdAt: '2026-10-18' },
  { id: 2, beatId: 1, artistName: 'Tomi Loop', audioUrl: 'https://soundcloud.com/tomiloop/topline-noche', status: 'ACCEPTED', createdAt: '2026-10-15' },
  { id: 3, beatId: 1, artistName: 'Luna Sur', audioUrl: 'https://soundcloud.com/lunasur/topline-noche', status: 'REJECTED', createdAt: '2026-10-14' },
  { id: 4, beatId: 5, artistName: 'Luna Sur', audioUrl: 'https://soundcloud.com/lunasur/topline-domingo', status: 'REJECTED', createdAt: '2026-10-14' },
  { id: 5, beatId: 2, artistName: 'DJ Nova', audioUrl: 'https://soundcloud.com/kira/cumbia-lunar-v2', status: 'PENDING', createdAt: '2026-10-20' },
  { id: 6, beatId: 4, artistName: 'DJ Nova', audioUrl: 'https://soundcloud.com/djnova/topline-madrugada', status: 'ACCEPTED', createdAt: '2026-10-12' },
]

// Jurado mixto de cada desafío y puntos que se reparten al cerrar.
export const JURY_WEIGHTS = { community: 30, verified: 30, guest: 40 }
export const PRIZE_POINTS = [500, 300, 150]

export const challenges = [
  {
    id: 1, title: 'Mejor beat de trap', genre: 'Trap', bpmRange: '130 a 160 BPM', rules: 'libre',
    status: 'VOTING', closesAt: '2026-10-30', prize: 'Sesión de estudio',
  },
  {
    id: 2, title: 'Cumbia futurista', genre: 'Cumbia', bpmRange: '90 a 110 BPM', rules: 'sin samples',
    status: 'OPEN', closesAt: '2026-11-15', prize: 'Mezcla y master',
  },
  {
    id: 3, title: 'Boom bap con samples', genre: 'Boom bap', bpmRange: '80 a 95 BPM', rules: 'sample obligatorio',
    status: 'CLOSED', closesAt: '2026-10-05', prize: 'Pack de plugins',
    podium: [
      { place: 1, name: 'DJ Nova', points: 47 },
      { place: 2, name: 'Kira Beats', points: 42 },
      { place: 3, name: 'Tomi Loop', points: 38 },
    ],
  },
]

// Entregas de los desafíos. votedByMe simula que el usuario actual ya votó.
export const submissions = [
  { id: 1, challengeId: 1, beatTitle: 'Madrugada Fría', producerName: 'Kira Beats', bpm: 150, votedByMe: false },
  { id: 2, challengeId: 1, beatTitle: 'Calle Oscura', producerName: 'Tomi Loop', bpm: 142, votedByMe: true },
  { id: 3, challengeId: 1, beatTitle: 'Noche en Palermo', producerName: 'DJ Nova', bpm: 140, votedByMe: false },
]

export const ranking = [
  { position: 1, name: 'DJ Nova', points: 312 },
  { position: 2, name: 'Kira Beats', points: 288 },
  { position: 3, name: 'Tomi Loop', points: 254 },
  { position: 4, name: 'Luna Sur', points: 231 },
  { position: 5, name: 'Beto Sampler', points: 207 },
  { position: 6, name: 'Mari Trap', points: 188 },
  { position: 7, name: 'Facu Drums', points: 160 },
  { position: 8, name: 'Nico 808', points: 142 },
  { position: 9, name: 'Ceci Loops', points: 121 },
  { position: 10, name: 'Pablo Keys', points: 97 },
]

export const RANKING_TOTAL_PRODUCERS = 48

export const plans = [
  {
    id: 'FREE', name: 'Free', price: 0, productionLimit: 50,
    features: ['Hasta 50 producciones', 'Participar en desafíos', 'Aparecer en el ranking'],
    missing: ['Sin beats destacados'],
  },
  {
    id: 'PREMIUM', name: 'Premium', price: 15000, productionLimit: null,
    features: ['Producciones sin límite', 'Participar en desafíos', 'Aparecer en el ranking', 'Beats destacados en el listado'],
    missing: [],
  },
]

// Producciones usadas por el usuario de demo (beats + toplines, histórico).
export const DEMO_PRODUCTIONS_USED = 12
