import type { EtatMateriel } from '@/shared/domain/labels'

/** Fond de carte (tuiles raster compatibles OpenStreetMap). */
export interface TileConfig {
  readonly url: string
  readonly attribution: string
  readonly subdomains: string
}

const ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'

/**
 * Fonds CARTO "Positron" (clair) et "Dark Matter" (sombre), derives d'OpenStreetMap :
 * lisibles sous des marqueurs colores et coherents avec le theme de l'application.
 */
export const TILES: Readonly<Record<'light' | 'dark', TileConfig>> = {
  light: {
    url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
    attribution: ATTRIBUTION,
    subdomains: 'abcd',
  },
  dark: {
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    attribution: ATTRIBUTION,
    subdomains: 'abcd',
  },
}

/** Classes de remplissage SVG par etat (jetons semantiques, generes par Tailwind). */
export const ETAT_FILL: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'fill-success',
  AFFECTE: 'fill-primary',
  EN_MAINTENANCE: 'fill-warning',
  REFORME: 'fill-muted-foreground',
}

/** Classes de fond par etat (pastilles HTML des marqueurs et de la legende). */
export const ETAT_BG: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'bg-success',
  AFFECTE: 'bg-primary',
  EN_MAINTENANCE: 'bg-warning',
  REFORME: 'bg-muted-foreground',
}

/** Texte contraste sur `ETAT_BG`. */
export const ETAT_TEXT: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'text-success-foreground',
  AFFECTE: 'text-primary-foreground',
  EN_MAINTENANCE: 'text-warning-foreground',
  REFORME: 'text-surface',
}
