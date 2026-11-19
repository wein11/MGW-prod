import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// PARA ESTUDIAR: vite.config.js
// Vite es la herramienta que levanta el servidor de desarrollo (npm run dev)
// y arma la versión final optimizada (npm run build → carpeta dist/).
// El plugin de React le enseña a Vite a entender JSX.
// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
})
