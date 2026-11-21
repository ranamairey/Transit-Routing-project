import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Admin console: a separate app (and container) from the rider app. /api proxied to the backend in dev.
// VITE_BASE serves the console under a sub-path (for example /transit-admin/); it defaults to the site root.
export default defineConfig({
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
