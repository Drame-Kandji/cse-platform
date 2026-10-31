import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import auth from '@/locales/fr/auth.json'
import common from '@/locales/fr/common.json'
import navigation from '@/locales/fr/navigation.json'
import appelsOffres from '@/locales/fr/appelsOffres.json'
import contrats from '@/locales/fr/contrats.json'
import finance from '@/locales/fr/finance.json'
import fournisseurs from '@/locales/fr/fournisseurs.json'
import besoins from '@/locales/fr/besoins.json'
import chantiers from '@/locales/fr/chantiers.json'
import maintenances from '@/locales/fr/maintenances.json'

/**
 * Ressources de traduction. Pour ajouter un namespace de module :
 * 1. creer `src/locales/fr/<module>.json` ; 2. l'importer et l'ajouter ici.
 * Les cles sont alors typees automatiquement (voir i18next.d.ts).
 */
export const resources = {
  fr: { common, auth, navigation, appelsOffres, contrats, fournisseurs, finance, besoins, chantiers, maintenances },
} as const

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
