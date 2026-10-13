import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The React app talks to the Spring Boot backend through /api (proxied in dev).
// In production set VITE_API_BASE to the backend origin or serve both behind one host.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
