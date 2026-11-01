import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import auth from '@/locales/fr/auth.json'
import common from '@/locales/fr/common.json'
import portail from './fr/portail.json'

/**
 * Ressources du portail : `common` et `auth` de web-interne (utilises par le design system partage)
 * + le namespace `portail` propre a cette application. Cles typees (voir i18next.d.ts).
 */
export const resources = { fr: { common, auth, portail } } as const

export const defaultNS = 'common'

void i18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  resources,
  defaultNS,
  ns: Object.keys(resources.fr),
  interpolation: { escapeValue: false },
  returnNull: false,
  initImmediate: false,
})

export { i18n }
