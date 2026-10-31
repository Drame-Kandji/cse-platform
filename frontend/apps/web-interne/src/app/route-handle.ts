import type navigationFr from '@/locales/fr/navigation.json'
import type { NavItemKey } from './navigation'

/** Cle i18n (namespace navigation) affichee dans le fil d'Ariane. */
export type CrumbKey = `items.${NavItemKey}` | `crumbs.${keyof (typeof navigationFr)['crumbs']}`

/** Metadonnees attachees a une route (`handle`). */
export interface RouteHandle {
  readonly crumb?: CrumbKey
}

export function isRouteHandle(value: unknown): value is RouteHandle {
  return typeof value === 'object' && value !== null && 'crumb' in value
}
