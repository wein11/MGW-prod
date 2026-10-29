# MGW Prod: frontend (React + Vite)

Frontend con datos simulados (no se conecta al backend en esta entrega).

## Cómo correrlo

```bash
cd frontend
npm install
npm run dev      # abre en http://localhost:5173
```

## Comandos

- `npm test`: tests de las validaciones (Vitest)
- `npm run lint`: revisa el código (oxlint, con reglas de accesibilidad)
- `npm run build`: genera la versión final en `dist/`

## Usuarios de prueba (login simulado)

| Nombre | Email | Contraseña | Rol |
|---|---|---|---|
| DJ Nova | artista@mgw.com | mgw12345 | ARTIST |
| Kira Beats | kira@mgw.com | mgw12345 | ARTIST |
| Sello Sur | discografica@mgw.com | mgw12345 | LABEL |

## Dónde está cada cosa

- `src/pages/`: una página por pantalla (cada uno trabaja en las suyas)
- `src/components/layout/`: header, footer, PageHeader, onda decorativa
- `src/components/ui/`: Button, FormField, Alert, StatusBadge, ConfirmDelete, EmptyState, PrivacyConsent
- `src/context/`: sesión simulada (`useAuth`) y datos compartidos (`useData`)
- `src/data/`: datos simulados y `fakeRequest`
- `src/utils/`: validaciones, formatos y ayudas de formularios

El plan completo está en `docs/superpowers/plans/2026-10-29-frontend-react.md`.
