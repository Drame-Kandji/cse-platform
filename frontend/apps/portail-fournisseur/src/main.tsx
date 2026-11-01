import '@fontsource-variable/inter'
import '../../web-interne/src/index.css'
import './i18n'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'

const container = document.getElementById('root')
if (!container) throw new Error('Element #root introuvable dans index.html')

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
