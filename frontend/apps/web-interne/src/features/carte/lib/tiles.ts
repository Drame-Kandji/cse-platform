import type { EtatMateriel } from '@/shared/domain/labels'

/** Fond de carte (tuiles raster OpenStreetMap). */
export interface TileConfig {
  readonly url: string
  readonly attribution: string
}

/**
 * Tuiles standard OpenStreetMap (sans cle d'API). Le theme sombre ne change pas de source : un filtre
 * CSS est applique au seul calque des tuiles (`.cse-carte--dark`, voir carte.css), les marqueurs
 * restent intacts.
 */
export const OSM_TILES: TileConfig = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">contributeurs OpenStreetMap</a>',
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
