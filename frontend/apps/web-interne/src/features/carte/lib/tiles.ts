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

/*
 * Charte bleu + blanc : bleu = affecte (en activite), gris = disponible, gris clair = reforme,
 * rouge = en maintenance (seule couleur d'alerte).
 */

/** Classes de remplissage SVG par etat (jetons semantiques, generes par Tailwind). */
export const ETAT_FILL: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'fill-muted-foreground',
  AFFECTE: 'fill-primary',
  EN_MAINTENANCE: 'fill-danger',
  REFORME: 'fill-border-strong',
}

/** Classes de fond par etat (pastilles HTML des marqueurs et de la legende). */
export const ETAT_BG: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'bg-muted-foreground',
  AFFECTE: 'bg-primary',
  EN_MAINTENANCE: 'bg-danger',
  REFORME: 'bg-border-strong',
}

/** Texte contraste sur `ETAT_BG`. */
export const ETAT_TEXT: Readonly<Record<EtatMateriel, string>> = {
  DISPONIBLE: 'text-surface',
  AFFECTE: 'text-primary-foreground',
  EN_MAINTENANCE: 'text-danger-foreground',
  REFORME: 'text-foreground',
}
