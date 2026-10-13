import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource-variable/cairo'
import 'leaflet/dist/leaflet.css'
import './index.css'
import './pwa.js'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { I18nProvider } from './i18n/I18nContext.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <I18nProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </I18nProvider>
    </BrowserRouter>
  </StrictMode>,
)
